package app.pulse.android.utils

import app.pulse.android.Database
import app.pulse.android.models.Playlist
import app.pulse.android.models.SongPlaylistMap
import app.pulse.android.transaction
import app.pulse.core.data.models.SongEntity
import app.pulse.android.Dependencies
import app.pulse.providers.innertube.Innertube
import app.pulse.providers.innertube.models.bodies.BrowseBody
import app.pulse.providers.innertube.models.bodies.SearchBody
import app.pulse.providers.innertube.requests.playlistPage
import app.pulse.providers.innertube.requests.searchPage
import app.pulse.providers.innertube.utils.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class PlaylistSource { YOUTUBE, SPOTIFY, UNKNOWN }

data class ParsedUrl(val source: PlaylistSource, val playlistId: String)

data class RawTrack(
    val title: String,
    val artists: List<String>,
    val durationMs: Long,
    val videoId: String? = null,
    val thumbnailUrl: String? = null
)

data class RawPlaylist(
    val name: String,
    val thumbnailUrl: String?,
    val tracks: List<RawTrack>
)

data class ResolvedTrack(
    val rawTrack: RawTrack,
    val youtubeVideoId: String
)

data class ImportResult(
    val playlistName: String,
    val thumbnailUrl: String?,
    val resolvedTracks: List<ResolvedTrack>,
    val unresolvedTracks: List<RawTrack>
)

object PlaylistImporter {
    private val YT_LIST_REGEX = Regex("""[?&]list=([a-zA-Z0-9_-]+)""")
    private val SPOTIFY_ID_REGEX = Regex("""/playlist/([a-zA-Z0-9]+)""")
    private val WORD_REGEX = Regex("\\W+")

    fun parseUrl(url: String): ParsedUrl? {
        val lower = url.lowercase()
        val isYt = lower.contains("youtube.com") || lower.contains("music.youtube.com")
        if (isYt) {
            val match = YT_LIST_REGEX.find(url)
            if (match != null) return ParsedUrl(PlaylistSource.YOUTUBE, match.groupValues[1])
        }
        if (lower.contains("spotify.com/playlist")) {
            val match = SPOTIFY_ID_REGEX.find(url)
            if (match != null) return ParsedUrl(PlaylistSource.SPOTIFY, match.groupValues[1])
        }
        return null
    }

    suspend fun importFromUrl(
        url: String,
        onProgress: (suspend (current: Int, total: Int, currentTrack: String) -> Unit)? = null
    ): Result<ImportResult> {
        return try {
            val parsed = parseUrl(url) ?: return Result.failure(IllegalArgumentException("Invalid playlist URL"))

            val rawPlaylist = when (parsed.source) {
                PlaylistSource.YOUTUBE -> fetchYouTubePlaylist(parsed.playlistId)
                PlaylistSource.SPOTIFY -> fetchSpotifyPlaylist(parsed.playlistId, url).getOrThrow()
                PlaylistSource.UNKNOWN -> return Result.failure(IllegalArgumentException("Unsupported playlist URL"))
            }

            if (rawPlaylist.tracks.isEmpty()) {
                return Result.failure(IllegalStateException("Playlist is empty"))
            }

            val existing = Database.playlistByName(rawPlaylist.name)
            if (existing != null) {
                return Result.failure(IllegalStateException("Playlist already imported"))
            }

            val resolvedTracks = resolveTracks(rawPlaylist.tracks, onProgress)

            Result.success(
                ImportResult(
                    playlistName = rawPlaylist.name,
                    thumbnailUrl = rawPlaylist.thumbnailUrl,
                    resolvedTracks = resolvedTracks,
                    unresolvedTracks = rawPlaylist.tracks.filter { track ->
                        resolvedTracks.none { it.rawTrack == track }
                    }
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchSpotifyPlaylist(playlistId: String, url: String): Result<RawPlaylist> {
        return try {
            val py = Dependencies.py
            val module = py.getModule("spotify_import")
            val jsonStr = module.callAttr("get_playlist", url).toString()
            val json = Json.parseToJsonElement(jsonStr).jsonObject
            val name = json["name"]?.jsonPrimitive?.content ?: "Spotify Playlist"
            val thumbnail = json["thumbnail"]?.jsonPrimitive?.content
            val tracks = json["tracks"]?.jsonArray?.map { element ->
                val obj = element.jsonObject
                RawTrack(
                    title = obj["title"]?.jsonPrimitive?.content ?: "",
                    artists = obj["artists"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                    durationMs = obj["duration_ms"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    thumbnailUrl = obj["thumbnail"]?.jsonPrimitive?.content
               )
            } ?: emptyList()
            Result.success(RawPlaylist(name = name, thumbnailUrl = thumbnail, tracks = tracks))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchYouTubePlaylist(playlistId: String): RawPlaylist {
        val browseId = "VL$playlistId"
        val page = Innertube.playlistPage(body = BrowseBody(browseId = browseId))?.getOrNull()
        // Kotlin 2.5 can't resolve PlaylistOrAlbumPage.title — use asMediaItem workaround
        val songs = page?.songsPage?.items?.map { it.asMediaItem } ?: emptyList()
        return RawPlaylist(
            name = with(page) { this?.let { runCatching { it.title }.getOrNull() } ?: "YouTube Playlist" },
            thumbnailUrl = with(page) { this?.let { runCatching { it.thumbnail?.url }.getOrNull() } },
            tracks = songs.map { mediaItem ->
                RawTrack(
                    title = mediaItem.mediaMetadata.title?.toString() ?: "",
                    artists = listOf(mediaItem.mediaMetadata.artist?.toString().orEmpty()),
                    durationMs = parseDurationText(mediaItem.mediaMetadata.extras?.getString("durationText").orEmpty()),
                    videoId = mediaItem.mediaId,
                    thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString()
                )
            }
        )
    }

    private suspend fun resolveTracks(
        tracks: List<RawTrack>,
        onProgress: (suspend (current: Int, total: Int, currentTrack: String) -> Unit)? = null
    ): List<ResolvedTrack> = coroutineScope {
        val semaphore = Semaphore(5)

        tracks.mapIndexed { index, track ->
            async {
                semaphore.withPermit {
                    onProgress?.invoke(index + 1, tracks.size, "${track.title} - ${track.artists.joinToString()}")

                    if (track.videoId != null) {
                        return@withPermit ResolvedTrack(rawTrack = track, youtubeVideoId = track.videoId)
                    }

                    val searchQuery = buildSearchQuery(track)
                    val result = searchYouTube(searchQuery, track.title, track.artists, track.durationMs)
                    result?.let { ResolvedTrack(rawTrack = track, youtubeVideoId = it) }
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun buildSearchQuery(track: RawTrack): String {
        val artists = track.artists.joinToString(" ").trim()
        return if (artists.isEmpty()) track.title.trim() else "${track.title.trim()} - $artists"
    }

    private suspend fun searchYouTube(
        query: String,
        expectedTitle: String,
        expectedArtists: List<String>,
        expectedDurationMs: Long
    ): String? {
        return try {
            val result = Innertube.searchPage(
                body = SearchBody(query = query, params = Innertube.SearchFilter.Song.value),
                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
            )?.getOrNull()

            val candidates = result?.items?.filterIsInstance<Innertube.SongItem>() ?: return null
            if (candidates.isEmpty()) return null

            candidates.firstNotNullOfOrNull { song ->
                val videoId = song.info?.endpoint?.videoId ?: return@firstNotNullOfOrNull null
                val score = matchScore(
                    expectedTitle = expectedTitle,
                    candidateTitle = song.info?.name ?: "",
                    expectedArtists = expectedArtists,
                    candidateAuthors = song.authors?.mapNotNull { it.name } ?: emptyList(),
                    expectedDurationMs = expectedDurationMs,
                    candidateDurationText = song.durationText,
                )
                if (score >= 0.5f) videoId else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun matchScore(
        expectedTitle: String, candidateTitle: String,
        expectedArtists: List<String>, candidateAuthors: List<String>,
        expectedDurationMs: Long, candidateDurationText: String?
    ): Float {
        val titleScore = fuzzyMatch(expectedTitle, candidateTitle)
        val artistScore = if (expectedArtists.isEmpty() || candidateAuthors.isEmpty()) 0.5f
        else expectedArtists.maxOfOrNull { e -> candidateAuthors.maxOf { c -> fuzzyMatch(e, c) } } ?: 0f
        val durationScore = if (expectedDurationMs <= 0 || candidateDurationText == null) 0.5f
        else {
            val candidateMs = parseDurationText(candidateDurationText)
            if (candidateMs <= 0) 0.5f
            else when (kotlin.math.abs(expectedDurationMs - candidateMs)) {
                in 0..3000L -> 1.0f
                in 3001..10000L -> 0.7f
                in 10001..30000L -> 0.3f
                else -> 0f
            }
        }
        return titleScore * 0.5f + artistScore * 0.3f + durationScore * 0.2f
    }

    private fun fuzzyMatch(a: String, b: String): Float {
        val normA = a.lowercase().trim()
        val normB = b.lowercase().trim()
        if (normA == normB) return 1.0f
        if (normA.contains(normB) || normB.contains(normA)) return 0.85f
        val wordsA = WORD_REGEX.split(normA).filter { it.length > 1 }.toSet()
        val wordsB = WORD_REGEX.split(normB).filter { it.length > 1 }.toSet()
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0f
        return wordsA.intersect(wordsB).size.toFloat() / maxOf(wordsA.size, wordsB.size)
    }

    private fun parseDurationText(text: String): Long {
        val parts = text.split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60L + parts[1]) * 1000L
            3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000L
            else -> 0L
        }
    }

    fun persistImport(result: ImportResult) {
        transaction {
            result.resolvedTracks.forEach { resolved ->
                Database.insert(
                    SongEntity(
                        id = resolved.youtubeVideoId,
                        title = resolved.rawTrack.title,
                        artistsText = resolved.rawTrack.artists.joinToString(),
                        durationText = null,
                        thumbnailUrl = resolved.rawTrack.thumbnailUrl ?: ""
                    )
                )
            }
            val existing = Database.playlistByName(result.playlistName)
            val playlistId = existing?.id ?: Database.insert(Playlist(name = result.playlistName))
            if (playlistId > 0) {
                Database.clearPlaylist(playlistId)
                Database.insertSongPlaylistMaps(
                    result.resolvedTracks.mapIndexed { index, resolved ->
                        SongPlaylistMap(songId = resolved.youtubeVideoId, playlistId = playlistId, position = index)
                    }
                )
            }
        }
    }
}
