package app.pulse.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import app.pulse.android.Database
import app.pulse.android.LocalPlayerServiceBinder
import app.pulse.android.R
import app.pulse.android.models.ui.toUiMedia
import app.pulse.android.ui.components.themed.CircularProgressIndicator
import app.pulse.core.ui.utils.px
import app.pulse.android.utils.DisposableListener
import app.pulse.android.utils.asMediaItem
import app.pulse.android.utils.positionAndDurationState
import app.pulse.android.utils.rememberIsBuffering
import app.pulse.android.utils.seamlessPlay
import app.pulse.android.utils.secondary
import app.pulse.android.utils.semiBold
import app.pulse.android.utils.shouldBePlaying
import app.pulse.android.utils.thumbnail
import app.pulse.core.ui.Dimensions
import app.pulse.core.ui.LocalAppearance
import app.pulse.android.models.ui.UiMedia
import coil3.compose.AsyncImage

@Composable
fun rememberMiniPlayerState(): MiniPlayerState {
    val binder = LocalPlayerServiceBinder.current

    var mediaItem by remember(binder) {
        mutableStateOf(
            value = binder?.player?.currentMediaItem,
            policy = neverEqualPolicy()
        )
    }
    var shouldBePlaying by remember(binder) { mutableStateOf(binder?.player?.shouldBePlaying == true) }
    val isBuffering = binder?.player?.rememberIsBuffering() ?: false

    var historyMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    LaunchedEffect(binder, mediaItem) {
        if (mediaItem == null) {
            Database.history(1).collect { songs ->
                historyMediaItem = songs.firstOrNull()?.asMediaItem
            }
        } else {
            historyMediaItem = null
        }
    }

    binder?.player?.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(newMediaItem: MediaItem?, reason: Int) {
                mediaItem = newMediaItem
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = player.shouldBePlaying
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = player.shouldBePlaying
            }
        }
    }

    val positionState = binder?.player?.positionAndDurationState()
    val duration = positionState?.component2() ?: 0L

    val activeMediaItem = mediaItem ?: historyMediaItem
    val metadata = activeMediaItem?.toUiMedia(duration)

    return remember(activeMediaItem, metadata, shouldBePlaying, isBuffering, binder, mediaItem, historyMediaItem) {
        MiniPlayerState(activeMediaItem, metadata, shouldBePlaying, isBuffering, binder, mediaItem, historyMediaItem)
    }
}

data class MiniPlayerState(
    val activeMediaItem: MediaItem?,
    val metadata: UiMedia?,
    val shouldBePlaying: Boolean,
    val isBuffering: Boolean,
    val binder: app.pulse.android.service.PlayerService.Binder?,
    val mediaItem: MediaItem?,
    val historyMediaItem: MediaItem?
)

@Composable
fun FloatingMiniPlayer(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val state = rememberMiniPlayerState()
    val (activeMediaItem, metadata, shouldBePlaying, isBuffering, binder) = state

    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(colorPalette.background1)
            .height(Dimensions.items.collapsedPlayerHeight)
            .clickable(
                enabled = activeMediaItem != null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = activeMediaItem?.mediaMetadata?.artworkUri?.thumbnail(Dimensions.thumbnails.song.px),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(Dimensions.items.collapsedPlayerHeight)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(colorPalette.background0)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    BasicText(
                        text = metadata?.title ?: stringResource(R.string.no_music_played),
                        style = typography.xs.semiBold.copy(color = colorPalette.text),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        text = metadata?.artist ?: "-",
                        style = typography.xs.secondary.copy(color = colorPalette.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (activeMediaItem != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(
                        targetState = shouldBePlaying to isBuffering,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = ""
                    ) { (isPlaying, buffering) ->
                        Box(
                            modifier = Modifier
                                .padding(all = 8.dp)
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (shouldBePlaying) binder?.player?.pause()
                                        else if (state.mediaItem != null) binder?.player?.play()
                                        else state.historyMediaItem?.let { binder?.player?.seamlessPlay(it) }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (buffering && isPlaying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(colorPalette.text),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MorphingMiniPlayer(
    progress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val state = rememberMiniPlayerState()
    val (activeMediaItem, metadata, shouldBePlaying, isBuffering, binder) = state

    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(colorPalette.background1)
            .height(Dimensions.items.collapsedPlayerHeight)
            .clickable(
                enabled = activeMediaItem != null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (8.dp * (1f - progress)).coerceAtLeast(4.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val thumbSize = (Dimensions.items.collapsedPlayerHeight * (1f - 0.4f * progress))

            AsyncImage(
                model = activeMediaItem?.mediaMetadata?.artworkUri?.thumbnail(Dimensions.thumbnails.song.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(thumbSize)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(colorPalette.background0)
            )

            Spacer(modifier = Modifier.width((12.dp * (1f - progress)).coerceAtLeast(6.dp)))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Dynamically scale text size based on progress
                val titleFontSize = typography.xs.fontSize * (1f - 0.1f * progress)
                
                BasicText(
                    text = metadata?.title ?: stringResource(R.string.no_music_played),
                    style = typography.xs.semiBold.copy(
                        color = colorPalette.text,
                        fontSize = titleFontSize
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val artistFontSize = typography.xs.fontSize * (1f - 0.15f * progress)
                BasicText(
                    text = metadata?.artist ?: "-",
                    style = typography.xs.secondary.copy(
                        color = colorPalette.textSecondary,
                        fontSize = artistFontSize
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        alpha = (1f - progress).coerceIn(0f, 1f)
                    }
                )
            }

            if (progress < 0.8f) {
                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = (1f - (progress / 0.8f)).coerceIn(0f, 1f)
                        }
                        .then(if (progress > 0.5f) Modifier.pointerInput(Unit) {} else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeMediaItem != null) {
                        AnimatedContent(
                            targetState = shouldBePlaying to isBuffering,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = ""
                        ) { (isPlaying, buffering) ->
                            Box(
                                modifier = Modifier
                                    .padding(all = 8.dp)
                                    .size(24.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (shouldBePlaying) binder?.player?.pause()
                                            else if (state.mediaItem != null) binder?.player?.play()
                                            else state.historyMediaItem?.let { binder?.player?.seamlessPlay(it) }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (buffering && isPlaying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.text),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun CompactMiniPlayer(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val state = rememberMiniPlayerState()
    val (activeMediaItem, metadata) = state
    val (colorPalette, typography) = LocalAppearance.current

    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(colorPalette.background1)
            .height(Dimensions.items.collapsedPlayerHeight)
            .clickable(
                enabled = activeMediaItem != null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxSize(), // Fill height to enable vertical centering
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = activeMediaItem?.mediaMetadata?.artworkUri?.thumbnail(Dimensions.thumbnails.song.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(Dimensions.items.collapsedPlayerHeight * 0.55f) // Smaller, more balanced size
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(colorPalette.background0)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicText(
                text = metadata?.title ?: stringResource(R.string.no_music_played),
                style = typography.xs.semiBold.copy(color = colorPalette.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
