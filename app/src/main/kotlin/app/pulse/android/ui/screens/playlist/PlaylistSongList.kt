package app.pulse.android.ui.screens.playlist

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.pulse.android.Database
import app.pulse.android.LocalPlayerAwareWindowInsets
import app.pulse.android.LocalPlayerServiceBinder
import app.pulse.android.R
import app.pulse.android.models.Playlist
import app.pulse.android.models.SongPlaylistMap
import app.pulse.android.query
import app.pulse.android.transaction
import app.pulse.android.ui.components.LocalMenuState
import app.pulse.android.ui.components.ShimmerHost
import app.pulse.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.pulse.android.ui.components.themed.Header
import app.pulse.android.ui.components.themed.HeaderIconButton
import app.pulse.android.ui.components.themed.HeaderPlaceholder
import app.pulse.android.ui.components.themed.LayoutWithAdaptiveThumbnail
import app.pulse.android.ui.components.themed.NonQueuedMediaItemMenu
import app.pulse.android.ui.components.themed.PlaylistInfo
import app.pulse.android.ui.components.themed.SecondaryTextButton
import app.pulse.android.ui.components.themed.TextFieldDialog
import app.pulse.android.ui.components.themed.adaptiveThumbnailContent
import app.pulse.android.ui.items.SongItem
import app.pulse.android.ui.items.SongItemPlaceholder
import app.pulse.android.utils.PlaylistDownloadIcon
import app.pulse.android.utils.asMediaItem
import app.pulse.android.utils.completed
import app.pulse.android.utils.enqueue
import app.pulse.android.utils.forcePlayAtIndex
import app.pulse.android.utils.forcePlayFromBeginning
import app.pulse.android.utils.playingSong
import app.pulse.compose.persist.persist
import app.pulse.core.ui.Dimensions
import app.pulse.core.ui.LocalAppearance
import app.pulse.core.ui.utils.isLandscape
import app.pulse.providers.innertube.Innertube
import app.pulse.providers.innertube.models.bodies.BrowseBody
import app.pulse.providers.innertube.requests.playlistPage
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistSongList(
    browseId: String,
    params: String?,
    maxDepth: Int?,
    shouldDedup: Boolean,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    var playlistPage by persist<Innertube.PlaylistOrAlbumPage?>("playlist/$browseId/playlistPage")

    LaunchedEffect(Unit) {
        if (playlistPage != null && playlistPage?.songsPage?.continuation == null) return@LaunchedEffect

        playlistPage = withContext(Dispatchers.IO) {
            Innertube
                .playlistPage(BrowseBody(browseId = browseId, params = params))
                ?.completed(
                    maxDepth = maxDepth ?: Int.MAX_VALUE,
                    shouldDedup = shouldDedup
                )
                ?.getOrNull()
        }
    }

    var isImportingPlaylist by rememberSaveable { mutableStateOf(false) }

    if (isImportingPlaylist) TextFieldDialog(
        hintText = stringResource(R.string.enter_playlist_name_prompt),
        initialTextInput = playlistPage?.title.orEmpty(),
        onDismiss = { isImportingPlaylist = false },
        onAccept = { text ->
            query {
                transaction {
                    val playlistId = Database.insert(
                        Playlist(
                            name = text,
                            browseId = browseId,
                            thumbnail = playlistPage?.thumbnail?.url
                        )
                    )

                    playlistPage?.songsPage?.items
                        ?.map(Innertube.SongItem::asMediaItem)
                        ?.onEach(Database::insert)
                        ?.mapIndexed { index, mediaItem ->
                            SongPlaylistMap(
                                songId = mediaItem.mediaId,
                                playlistId = playlistId,
                                position = index
                            )
                        }?.let(Database::insertSongPlaylistMaps)
                }
            }
        }
    )

    val headerContent: @Composable () -> Unit = {
        if (playlistPage == null) HeaderPlaceholder(modifier = Modifier.shimmer())
        else Header(title = playlistPage?.title ?: stringResource(R.string.unknown)) {
            SecondaryTextButton(
                text = stringResource(R.string.enqueue),
                enabled = playlistPage?.songsPage?.items?.isNotEmpty() == true,
                onClick = {
                    playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)
                        ?.let { mediaItems ->
                            binder?.player?.enqueue(mediaItems)
                        }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)
                ?.let { PlaylistDownloadIcon(songs = it.toImmutableList()) }

            HeaderIconButton(
                icon = R.drawable.add,
                color = colorPalette.text,
                onClick = { isImportingPlaylist = true }
            )

            HeaderIconButton(
                icon = R.drawable.share_social,
                color = colorPalette.text,
                onClick = {
                    val url = playlistPage?.url
                        ?: "https://music.youtube.com/playlist?list=${browseId.removePrefix("VL")}"

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }

                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
            )
        }
    }

    val thumbnailContent = adaptiveThumbnailContent(
        isLoading = playlistPage == null,
        url = playlistPage?.thumbnail?.url
    )

    val lazyListState = rememberLazyListState()

    val (currentMediaId, playing) = playingSong(binder)

    LayoutWithAdaptiveThumbnail(
        thumbnailContent = thumbnailContent,
        modifier = modifier
    ) {
        Box {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        headerContent()
                        if (!isLandscape) thumbnailContent()
                        PlaylistInfo(playlist = playlistPage)
                    }
                }

                itemsIndexed(items = playlistPage?.songsPage?.items ?: emptyList()) { index, song ->
                    SongItem(
                        song = song,
                        thumbnailSize = Dimensions.thumbnails.song,
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        NonQueuedMediaItemMenu(
                                            onDismiss = menuState::hide,
                                            mediaItem = song.asMediaItem
                                        )
                                    }
                                },
                                onClick = {
                                    playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)
                                        ?.let { mediaItems ->
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(mediaItems, index)
                                        }
                                }
                            ),
                        isPlaying = playing && currentMediaId == song.key
                    )
                }

                if (playlistPage == null) item(key = "loading") {
                    ShimmerHost(modifier = Modifier.fillParentMaxSize()) {
                        repeat(4) {
                            SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                        }
                    }
                }
            }

        }
    }
}

