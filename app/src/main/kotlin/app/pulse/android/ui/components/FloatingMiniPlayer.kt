package app.pulse.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import app.pulse.core.ui.collapsedPlayerProgressBar
import app.pulse.core.ui.utils.px
import app.pulse.android.ui.components.themed.IconButton
import app.pulse.android.utils.DisposableListener
import app.pulse.android.utils.asMediaItem
import app.pulse.android.utils.forcePlay
import app.pulse.android.utils.forceSeekToNext
import app.pulse.android.utils.positionAndDurationState
import app.pulse.android.utils.seamlessPlay
import app.pulse.android.utils.secondary
import app.pulse.android.utils.semiBold
import app.pulse.android.utils.shouldBePlaying
import app.pulse.android.utils.thumbnail
import app.pulse.core.ui.Dimensions
import app.pulse.core.ui.LocalAppearance
import coil3.compose.AsyncImage
import kotlin.math.absoluteValue

@Composable
fun FloatingMiniPlayer(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val (colorPalette, typography) = LocalAppearance.current

    var mediaItem by remember(binder) {
        mutableStateOf(
            value = binder?.player?.currentMediaItem,
            policy = neverEqualPolicy()
        )
    }
    var shouldBePlaying by remember(binder) { mutableStateOf(binder?.player?.shouldBePlaying == true) }

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
    val position = positionState?.component1() ?: 0L
    val duration = positionState?.component2() ?: 0L

    val activeMediaItem = mediaItem ?: historyMediaItem
    val metadata = activeMediaItem?.toUiMedia(duration)

    val progress = runCatching {
        if (duration.absoluteValue > 0) position.toFloat() / duration.absoluteValue else 0f
    }.getOrElse { 0f }

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(colorPalette.background1)
            .clickable(
                enabled = activeMediaItem != null,
                onClick = onClick
            )
            .drawBehind {
                if (activeMediaItem != null) {
                    drawRect(
                        color = colorPalette.collapsedPlayerProgressBar,
                        topLeft = Offset.Zero,
                        size = Size(
                            width = size.width * progress,
                            height = size.height
                        )
                    )
                }
            }
            .fillMaxWidth()
            .height(Dimensions.items.collapsedPlayerHeight)
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
                        targetState = shouldBePlaying,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = ""
                    ) { isPlaying ->
                        IconButton(
                            onClick = {
                                if (isPlaying) binder?.player?.pause()
                                else if (mediaItem != null) binder?.player?.forcePlay(mediaItem!!)
                                else historyMediaItem?.let { binder?.player?.seamlessPlay(it) }
                            },
                            icon = if (isPlaying) R.drawable.pause else R.drawable.play,
                            color = colorPalette.text,
                            modifier = Modifier.padding(all = 8.dp).size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { binder?.player?.forceSeekToNext() },
                        icon = R.drawable.play_skip_forward,
                        color = colorPalette.text,
                        modifier = Modifier.padding(all = 8.dp).size(24.dp)
                    )
                }
            }
        }
    }
}
