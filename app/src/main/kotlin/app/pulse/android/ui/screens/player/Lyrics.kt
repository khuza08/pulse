package app.pulse.android.ui.screens.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import app.pulse.android.Database
import app.pulse.android.LocalPlayerServiceBinder
import app.pulse.android.R
import app.pulse.android.models.Lyrics
import app.pulse.android.preferences.PlayerPreferences
import app.pulse.android.query
import app.pulse.android.service.LOCAL_KEY_PREFIX
import app.pulse.android.transaction
import app.pulse.android.ui.components.LocalMenuState
import app.pulse.android.ui.components.themed.CircularProgressIndicator
import app.pulse.android.ui.components.themed.DefaultDialog
import app.pulse.android.ui.components.themed.Menu
import app.pulse.android.ui.components.themed.MenuEntry
import app.pulse.android.ui.components.themed.TextField
import app.pulse.android.ui.components.themed.TextFieldDialog
import app.pulse.android.ui.components.themed.TextPlaceholder
import app.pulse.android.ui.components.themed.ValueSelectorDialogBody
import app.pulse.android.ui.modifiers.verticalFadingEdge
import app.pulse.android.utils.SynchronizedLyrics
import app.pulse.android.utils.SynchronizedLyricsState
import app.pulse.android.utils.bold
import app.pulse.android.utils.center
import app.pulse.android.utils.color
import app.pulse.android.utils.isInPip
import app.pulse.android.utils.medium
import app.pulse.android.utils.semiBold
import app.pulse.android.utils.toast
import app.pulse.core.ui.LocalAppearance
import app.pulse.core.ui.onOverlay
import app.pulse.core.ui.onOverlayShimmer
import app.pulse.core.ui.utils.dp
import app.pulse.providers.innertube.Innertube
import app.pulse.providers.innertube.models.bodies.NextBody
import app.pulse.providers.innertube.requests.lyrics
import app.pulse.providers.kugou.KuGou
import app.pulse.providers.lrclib.LrcLib
import app.pulse.providers.lrclib.LrcParser
import app.pulse.providers.lrclib.models.Track
import app.pulse.providers.lrclib.toLrcFile
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val UPDATE_DELAY = 50.milliseconds

private const val ACTIVE_LINE_SCALE = 1.05f

// Center a lyric line using measured layout metrics (ported from ArchiveTune):
// read the item's real offset and size from layoutInfo and scroll BY the delta
// to the viewport center, so centering is exact at every font size. When the
// item is not composed yet (seek/initial), jump to its vicinity first; the next
// tick's measured branch then centers it precisely.
private const val LYRIC_FOCUS_TOP_GUARD = 0.30f
private const val LYRIC_FOCUS_BOTTOM_GUARD = 0.70f
private const val LYRIC_FOCUS_MIN_SCROLL_PX = 8

private suspend fun LazyListState.centerActiveItem(targetIndex: Int) {
    var itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
    if (itemInfo == null) {
        val distance = abs(targetIndex - firstVisibleItemIndex)
        if (distance > 15) scrollToItem(targetIndex)
        else animateScrollToItem(targetIndex, 0)
        // wait one frame for layout to update after scroll
        withFrameNanos { }
        itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
    }
    itemInfo ?: return

    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    if (viewportHeight <= 0) return

    val itemCenter = itemInfo.offset + itemInfo.size / 2
    val center = layoutInfo.viewportStartOffset + viewportHeight / 2
    val delta = itemCenter - center

    // Guard zone: skip if item is already in the sweet spot (30-70% of viewport)
    val topGuard = layoutInfo.viewportStartOffset + (viewportHeight * LYRIC_FOCUS_TOP_GUARD).toInt()
    val bottomGuard = layoutInfo.viewportEndOffset - (viewportHeight * LYRIC_FOCUS_BOTTOM_GUARD).toInt()
    if (itemCenter in topGuard..bottomGuard) return

    if (abs(delta) > LYRIC_FOCUS_MIN_SCROLL_PX) {
        animateScrollBy(
            value = delta.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
}

internal suspend fun fetchLyricsParallel(
    mediaId: String,
    artist: String,
    title: String,
    album: String?,
    duration: Long,
    currentFixed: String?,
    currentSynced: String?
): Pair<String?, String?> = coroutineScope {
    val fixedDef = async {
        currentFixed ?: run {
            listOf(
                async { Innertube.lyrics(NextBody(videoId = mediaId))?.getOrNull() },
                async {
                    LrcLib.bestLyrics(
                        artist = artist,
                        title = title,
                        duration = duration.milliseconds,
                        album = album,
                        synced = false
                    )?.map { it?.text }?.getOrNull()
                }
            ).firstNotNullOfOrNull { it.await() }
        }
    }
    val syncedDef = async {
        currentSynced ?: run {
            val altTitle = title.split("(")[0].trim()
            listOfNotNull(
                async {
                    LrcLib.bestLyrics(
                        artist = artist,
                        title = title,
                        duration = duration.milliseconds,
                        album = album
                    )?.map { it?.text }?.getOrNull()
                },
                if (altTitle != title) async {
                    LrcLib.bestLyrics(
                        artist = artist,
                        title = altTitle,
                        duration = duration.milliseconds,
                        album = album
                    )?.map { it?.text }?.getOrNull()
                } else null,
                async {
                    KuGou.lyrics(
                        artist = artist,
                        title = title,
                        duration = duration / 1000
                    )?.map { it?.value }?.getOrNull()
                }
            ).firstNotNullOfOrNull { it.await() }
        }
    }
    fixedDef.await() to syncedDef.await()
}

@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    mediaMetadataProvider: () -> MediaMetadata,
    durationProvider: () -> Long,
    ensureSongInserted: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuLaunch: () -> Unit = { },
    onOpenDialog: (() -> Unit)? = null,
    shouldShowSynchronizedLyrics: Boolean = PlayerPreferences.isShowingSynchronizedLyrics,
    setShouldShowSynchronizedLyrics: (Boolean) -> Unit = {
        PlayerPreferences.isShowingSynchronizedLyrics = it
    },
    shouldKeepScreenAwake: Boolean = PlayerPreferences.lyricsKeepScreenAwake,
    shouldUpdateLyrics: Boolean = true,
    showControls: Boolean = true,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val currentEnsureSongInserted by rememberUpdatedState(ensureSongInserted)
    val currentMediaMetadataProvider by rememberUpdatedState(mediaMetadataProvider)
    val currentDurationProvider by rememberUpdatedState(durationProvider)

    val (colorPalette, typography) = LocalAppearance.current
    val lyricsFontSize = PlayerPreferences.lyricsFontSize.sp
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val binder = LocalPlayerServiceBinder.current
    val view = LocalView.current

    val pip = isInPip()

    var lyrics by remember { mutableStateOf<Lyrics?>(LyricsCache[mediaId]) }

    val showSynchronizedLyrics = remember(shouldShowSynchronizedLyrics, lyrics) {
        shouldShowSynchronizedLyrics && lyrics?.synced?.isBlank() != true
    }

    var editing by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }
    var picking by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }
    var error by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }

    val text = remember(lyrics, showSynchronizedLyrics) {
        if (showSynchronizedLyrics) lyrics?.synced else lyrics?.fixed
    }
    var invalidLrc by remember(text) { mutableStateOf(false) }

    DisposableEffect(shouldKeepScreenAwake) {
        view.keepScreenOn = shouldKeepScreenAwake

        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(mediaId, shouldShowSynchronizedLyrics) {
        LyricsCache[mediaId]?.let { cached ->
            lyrics = cached
            error = (shouldShowSynchronizedLyrics && cached.synced.isNullOrBlank()) ||
                (!shouldShowSynchronizedLyrics && cached.fixed.isNullOrBlank())
        }

        runCatching {
            withContext(Dispatchers.IO) {
                Database
                    .lyrics(mediaId)
                    .distinctUntilChanged()
                    .cancellable()
                    .collect { currentLyrics ->
                        if (
                            !shouldUpdateLyrics ||
                            (currentLyrics?.fixed != null && currentLyrics.synced != null)
                        ) lyrics = currentLyrics
                        else {
                            val mediaMetadata = currentMediaMetadataProvider()
                            var duration =
                                withContext(Dispatchers.Main) { currentDurationProvider() }

                            while (duration == C.TIME_UNSET) {
                                delay(100.milliseconds)
                                duration =
                                    withContext(Dispatchers.Main) { currentDurationProvider() }
                            }

                            val album = mediaMetadata.albumTitle?.toString()
                            val artist = mediaMetadata.artist?.toString().orEmpty()
                            val title = mediaMetadata.title?.toString().orEmpty().let {
                                if (mediaId.startsWith(LOCAL_KEY_PREFIX)) it
                                    .substringBeforeLast('.')
                                    .trim()
                                else it
                            }

                            lyrics = null
                            error = false

                            val (fixed, synced) = fetchLyricsParallel(
                                mediaId = mediaId,
                                artist = artist,
                                title = title,
                                album = album,
                                duration = duration,
                                currentFixed = currentLyrics?.fixed,
                                currentSynced = currentLyrics?.synced
                            )

                            Lyrics(
                                songId = mediaId,
                                fixed = fixed.orEmpty(),
                                synced = synced.orEmpty()
                            ).also {
                                ensureActive()

                                LyricsCache[mediaId] = it

                                transaction {
                                    runCatching {
                                        currentEnsureSongInserted()
                                        Database.upsert(it)
                                    }
                                }
                            }
                        }

                        error =
                            (shouldShowSynchronizedLyrics && lyrics?.synced?.isBlank() == true) ||
                            (!shouldShowSynchronizedLyrics && lyrics?.fixed?.isBlank() == true)
                    }
            }
        }.exceptionOrNull()?.let {
            if (it is CancellationException) throw it
            else it.printStackTrace()
        }
    }

    // close when the song has no lyrics
    LaunchedEffect(error) {
        if (error) {
            delay(2.seconds)
            if (error) onDismiss()
        }
    }

    if (isDisplayed) {
        if (editing) TextFieldDialog(
        hintText = stringResource(R.string.enter_lyrics),
        initialTextInput = (if (shouldShowSynchronizedLyrics) lyrics?.synced else lyrics?.fixed)
            .orEmpty(),
        singleLine = false,
        maxLines = 10,
        isTextInputValid = { true },
        onDismiss = { editing = false },
        onAccept = {
            transaction {
                runCatching {
                    currentEnsureSongInserted()

                    Database.upsert(
                        if (shouldShowSynchronizedLyrics) Lyrics(
                            songId = mediaId,
                            fixed = lyrics?.fixed,
                            synced = it
                        ) else Lyrics(
                            songId = mediaId,
                            fixed = it,
                            synced = lyrics?.synced
                        )
                    )
                }
            }
        }
    )

    if (picking && shouldShowSynchronizedLyrics) {
        var query by rememberSaveable {
            mutableStateOf(
                currentMediaMetadataProvider().title?.toString().orEmpty().let {
                    if (mediaId.startsWith(LOCAL_KEY_PREFIX)) it
                        .substringBeforeLast('.')
                        .trim()
                    else it
                }
            )
        }

        LrcLibSearchDialog(
            query = query,
            setQuery = { query = it },
            onDismiss = { picking = false },
            onPick = {
                runCatching {
                    transaction {
                        Database.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = lyrics?.fixed,
                                synced = it.syncedLyrics
                            )
                        )
                    }
                }
            }
        )
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopStart,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = error,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            BasicText(
                text = stringResource(
                    if (shouldShowSynchronizedLyrics) R.string.synchronized_lyrics_not_available
                    else R.string.lyrics_not_available
                ),
                style = typography.xs.center.medium.color(colorPalette.onOverlay),
                modifier = Modifier
                    .background(Color.Black.copy(0.4f))
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                maxLines = if (pip) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
            )
        }

        AnimatedVisibility(
            visible = !text.isNullOrBlank() && !error && invalidLrc && shouldShowSynchronizedLyrics,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            BasicText(
                text = stringResource(R.string.invalid_synchronized_lyrics),
                style = typography.xs.center.medium.color(colorPalette.onOverlay),
                modifier = Modifier
                    .background(Color.Black.copy(0.4f))
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                maxLines = if (pip) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
            )
        }

        val lyricsState = rememberSaveable(text) {
            val file = lyrics?.synced?.takeIf { it.isNotBlank() }?.let {
                LrcParser.parse(it)?.toLrcFile()
            }

            SynchronizedLyricsState(
                sentences = file?.lines,
                offset = file?.offset?.inWholeMilliseconds ?: 0L
            )
        }

        val synchronizedLyrics = remember(lyricsState, mediaId) {
            invalidLrc = lyricsState.sentences == null
            lyricsState.sentences?.let {
                SynchronizedLyrics(it.toImmutableMap()) {
                    binder?.player?.let { player ->
                        // During crossfade, player.currentMediaItem still points
                        // to old song — return 0 so lyrics stay at top.
                        if (player.currentMediaItem?.mediaId != mediaId) 0L
                        else player.currentPosition + lyricsState.offset -
                            (lyrics?.startTime ?: 0L)
                    } ?: 0L
                }
            }
        }

        AnimatedContent(
            targetState = showSynchronizedLyrics,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentAlignment = Alignment.TopStart,
            label = ""
        ) { synchronized ->
            if (synchronized) {

                var autoFollowPausedUntil by remember { mutableStateOf(0L) }

                val manualScrollConnection = remember {
                    var lastScrollTime = 0L
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (source == NestedScrollSource.UserInput) {
                                val now = System.currentTimeMillis()
                                if (now - lastScrollTime > 50) {
                                    autoFollowPausedUntil =
                                        now + 2.seconds.inWholeMilliseconds
                                    lastScrollTime = now
                                }
                            }
                            return super.onPostScroll(consumed, available, source)
                        }

                        override suspend fun onPostFling(
                            consumed: Velocity,
                            available: Velocity
                        ): Velocity {
                            autoFollowPausedUntil =
                                System.currentTimeMillis() + 2.seconds.inWholeMilliseconds
                            return super.onPostFling(consumed, available)
                        }
                    }
                }

                LaunchedEffect(synchronizedLyrics, lyricsFontSize) {
                    val currentSynchronizedLyrics = synchronizedLyrics ?: return@LaunchedEffect

                    // Wait for the first layout: before items exist the helper's
                    // fallback animates a jump, which the 50ms tick then snaps to
                    // center - the visible open/close bounce. Centering only after
                    // layout lands dead-center without any animation.
                    while (lazyListState.layoutInfo.visibleItemsInfo.isEmpty()) {
                        delay(UPDATE_DELAY)
                    }

                    // Update index from current position before reading it.
                    // Without this, index is stale (-1 from init) and lyrics
                    // center at the top of the list instead of the active line.
                    currentSynchronizedLyrics.update()
                    val initIdx = currentSynchronizedLyrics.index + 1

                    // centerActiveItem handles both cases:
                    // - Item visible: centers via spring animation
                    // - Item not visible: scrollToItem to vicinity, then centers
                    // No separate scrollToItem needed — avoids race when
                    // active line changes simultaneously with panel open.
                    lazyListState.centerActiveItem(initIdx)

                    while (true) {
                        delay(UPDATE_DELAY)
                        currentSynchronizedLyrics.update()
                        if (autoFollowPausedUntil > System.currentTimeMillis()) continue
                        if (lazyListState.isScrollInProgress) continue

                        val targetIndex = currentSynchronizedLyrics.index + 1
                        lazyListState.centerActiveItem(targetIndex)
                    }
                }

                // re-center active line when app returns from background.
                // The tick loop may have stale layoutInfo after the view was
                // detached, causing centerActiveItem to scroll to the wrong
                // position, forcing a fresh center on resume fixes this.
                val lifecycleOwner = LocalLifecycleOwner.current
                val resumeScope = rememberCoroutineScope()
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val current = synchronizedLyrics ?: return@LifecycleEventObserver
                            current.update()
                            resumeScope.launch {
                                lazyListState.centerActiveItem(current.index + 1)
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                if (synchronizedLyrics != null) LazyColumn(
                    state = lazyListState,
                    userScrollEnabled = true,
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .verticalFadingEdge(topSize = 10, bottomSize = 2)
                        .nestedScroll(manualScrollConnection)
                        .fillMaxWidth()
                ) {
                    item(key = "header", contentType = 0) {
                        Spacer(modifier = Modifier.height(maxHeight / 2))
                    }
                    itemsIndexed(
                        items = synchronizedLyrics.sentences.values.toImmutableList()
                    ) { index, sentence ->
                        val active = index == synchronizedLyrics.index
                        val color by animateColorAsState(
                            if (active) Color.White
                            else colorPalette.text.copy(alpha = 0.5f)
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (active) ACTIVE_LINE_SCALE else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "lyricLineScale"
                        )
                        if (sentence.isBlank()) Image(
                            painter = painterResource(R.drawable.musical_notes),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(color),
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .padding(vertical = 16.dp, horizontal = 48.dp)
                                .size(lyricsFontSize.dp)
                        ) else BasicText(
                            text = sentence,
                            style = if (active) typography.m.bold.copy(fontSize = lyricsFontSize).color(color)
                            else typography.m.semiBold.copy(fontSize = lyricsFontSize).color(color),
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .padding(vertical = 12.dp, horizontal = 48.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        binder?.player?.seekTo(synchronizedLyrics.sentences.keys.elementAt(index))
                                    }
                                )
                        )
                    }
                    item(key = "footer", contentType = 0) {
                        Spacer(modifier = Modifier.height(maxHeight / 2))
                    }
                }
            } else BasicText(
                text = lyrics?.fixed.orEmpty(),
                style = typography.m.semiBold.copy(fontSize = lyricsFontSize).color(colorPalette.text.copy(alpha = 0.8f)),
                modifier = Modifier
                    .verticalFadingEdge(topSize = 4, bottomSize = 0)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 48.dp)
            )
        }

        if (text == null && !error) Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .shimmer()
        ) {
            repeat(4) {
                TextPlaceholder(
                    color = colorPalette.onOverlayShimmer,
                    modifier = Modifier.alpha(1f - it * 0.2f)
                )
            }
        }

        if (showControls) {
            if (onOpenDialog != null) Image(
                painter = painterResource(R.drawable.expand),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.onOverlay),
                modifier = Modifier
                    .padding(all = 4.dp)
                    .clickable(
                        indication = ripple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            onOpenDialog()
                        }
                    )
                    .padding(all = 8.dp)
                    .size(20.dp)
                    .align(Alignment.BottomStart)
            )

            Image(
                painter = painterResource(R.drawable.ellipsis_horizontal),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.onOverlay),
                modifier = Modifier
                    .padding(all = 4.dp)
                    .clickable(
                        indication = ripple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            onMenuLaunch()
                            menuState.display {
                                Menu {
                                    MenuEntry(
                                        icon = R.drawable.time,
                                        text = stringResource(
                                            if (shouldShowSynchronizedLyrics) R.string.show_unsynchronized_lyrics
                                            else R.string.show_synchronized_lyrics
                                        ),
                                        secondaryText = if (shouldShowSynchronizedLyrics) null
                                        else stringResource(R.string.provided_lyrics_by),
                                        onClick = {
                                            menuState.hide()
                                            setShouldShowSynchronizedLyrics(!shouldShowSynchronizedLyrics)
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.pencil,
                                        text = stringResource(R.string.edit_lyrics),
                                        onClick = {
                                            menuState.hide()
                                            editing = true
                                        }
                                    )

                                    val errorMsg = stringResource(R.string.no_browser_installed)
                                    MenuEntry(
                                        icon = R.drawable.search,
                                        text = stringResource(R.string.search_lyrics_online),
                                        onClick = {
                                            menuState.hide()
                                            val mediaMetadata = currentMediaMetadataProvider()

                                            try {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                        putExtra(
                                                            SearchManager.QUERY,
                                                            "${mediaMetadata.title} ${mediaMetadata.artist} lyrics"
                                                        )
                                                    }
                                                )
                                            } catch (_: ActivityNotFoundException) {
                                                context.toast(errorMsg)
                                            }
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.sync,
                                        text = stringResource(R.string.refetch_lyrics),
                                        enabled = lyrics != null,
                                        onClick = {
                                            menuState.hide()

                                            transaction {
                                                runCatching {
                                                    currentEnsureSongInserted()

                                                    Database.upsert(
                                                        if (shouldShowSynchronizedLyrics) Lyrics(
                                                            songId = mediaId,
                                                            fixed = lyrics?.fixed,
                                                            synced = null
                                                        ) else Lyrics(
                                                            songId = mediaId,
                                                            fixed = null,
                                                            synced = lyrics?.synced
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    if (shouldShowSynchronizedLyrics) {
                                        MenuEntry(
                                            icon = R.drawable.download,
                                            text = stringResource(R.string.pick_from_lrclib),
                                            onClick = {
                                                menuState.hide()
                                                picking = true
                                            }
                                        )
                                        MenuEntry(
                                            icon = R.drawable.play_skip_forward,
                                            text = stringResource(R.string.set_lyrics_start_offset),
                                            secondaryText = stringResource(
                                                R.string.set_lyrics_start_offset_description
                                            ),
                                            onClick = {
                                                menuState.hide()
                                                lyrics?.let {
                                                    val startTime = binder?.player?.currentPosition
                                                    query {
                                                        Database.upsert(it.copy(startTime = startTime))
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    .padding(all = 8.dp)
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
    }
}

@Composable
fun LrcLibSearchDialog(
    query: String,
    setQuery: (String) -> Unit,
    onDismiss: () -> Unit,
    onPick: (Track) -> Unit,
    modifier: Modifier = Modifier
) = DefaultDialog(
    onDismiss = onDismiss,
    horizontalPadding = 0.dp,
    modifier = modifier
) {
    val (_, typography) = LocalAppearance.current

    val tracks = remember { mutableStateListOf<Track>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        loading = true
        error = false

        delay(1.seconds)

        LrcLib.lyrics(
            query = query,
            synced = true
        )?.onSuccess { newTracks ->
            tracks.clear()
            tracks.addAll(newTracks.filter { !it.syncedLyrics.isNullOrBlank() })
            loading = false
            error = false
        }?.onFailure {
            loading = false
            error = true
            it.printStackTrace()
        } ?: run { loading = false }
    }

    TextField(
        value = query,
        onValueChange = setQuery,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        maxLines = 1,
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))

    when {
        loading -> CircularProgressIndicator(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        error || tracks.isEmpty() -> BasicText(
            text = stringResource(R.string.no_lyrics_found),
            style = typography.s.semiBold.center,
            modifier = Modifier
                .padding(all = 24.dp)
                .align(Alignment.CenterHorizontally)
        )

        else -> ValueSelectorDialogBody(
            onDismiss = onDismiss,
            title = stringResource(R.string.choose_lyric_track),
            selectedValue = null,
            values = tracks.toImmutableList(),
            onValueSelect = {
                transaction {
                    onPick(it)
                    onDismiss()
                }
            },
            valueText = {
                "${it.artistName} - ${it.trackName} (${
                    it.duration?.seconds?.toComponents { minutes, seconds, _ ->
                        "$minutes:${seconds.toString().padStart(2, '0')}"
                    } ?: "?:??"
                })"
            }
        )
    }
}
