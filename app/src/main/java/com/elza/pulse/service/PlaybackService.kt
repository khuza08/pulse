package com.elza.pulse.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.elza.pulse.Dependencies
import com.elza.pulse.utils.YouTubeDLResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        
        val dataSourceFactory = createYouTubeDataSourceResolverFactory(this)
        
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        fun createYouTubeDataSourceResolverFactory(context: Context): DataSource.Factory {
            val defaultFactory = DefaultDataSource.Factory(context)
            
            return ResolvingDataSource.Factory(defaultFactory) { dataSpec ->
                val mediaId = dataSpec.key ?: dataSpec.uri.toString()
                
                // If it's already a direct URL or local, return as is
                if (dataSpec.uri.scheme == "file" || dataSpec.uri.host?.contains("googlevideo.com") == true) {
                    return@Factory dataSpec
                }

                // Resolve using yt-dlp via Chaquopy
                try {
                    val videoId = mediaId.substringAfter("v=", mediaId).substringBefore("&")
                    val jsonResponse = Dependencies.runDownload(videoId)
                    val response = YouTubeDLResponse.fromString(jsonResponse)
                    
                    val uri = android.net.Uri.parse(response.url ?: error("No URL in yt-dlp response"))
                    
                    dataSpec.withUri(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    dataSpec
                }
            }
        }
    }
}
