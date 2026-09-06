package app.pulse.android.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pulse.android.Database
import app.pulse.android.LocalPlayerAwareWindowInsets
import app.pulse.android.R
import app.pulse.android.models.PipedSession
import app.pulse.android.models.Playlist
import app.pulse.android.models.PlaylistPreview
import app.pulse.android.preferences.DataPreferences
import app.pulse.android.preferences.OrderPreferences
import app.pulse.android.preferences.UIStatePreferences
import app.pulse.android.query
import app.pulse.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.pulse.android.ui.components.themed.CollapsingHeader
import app.pulse.android.ui.components.themed.HeaderIconButton
import app.pulse.android.ui.components.themed.HeaderPillIconButton
import app.pulse.android.ui.components.themed.SecondaryTextButton
import app.pulse.android.ui.components.NewMenu
import app.pulse.android.ui.components.NewMenuDivider
import app.pulse.android.ui.components.NewMenuEntry
import app.pulse.android.ui.components.themed.HeaderIconButton
import app.pulse.android.ui.components.themed.ImportPlaylistDialog
import app.pulse.android.ui.components.themed.TextFieldDialog
import app.pulse.android.ui.components.themed.VerticalDivider
import app.pulse.android.ui.items.PlaylistItem
import app.pulse.android.ui.screens.Route
import app.pulse.android.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import app.pulse.android.ui.screens.settings.SettingsEntryGroupText
import app.pulse.android.ui.screens.settings.SettingsGroupSpacer
import app.pulse.compose.persist.persist
import app.pulse.compose.persist.persistList
import app.pulse.core.data.enums.PlaylistSortBy
import app.pulse.core.data.enums.SortOrder
import app.pulse.core.ui.Dimensions
import app.pulse.core.ui.LocalAppearance
import app.pulse.providers.piped.Piped
import app.pulse.providers.piped.models.Session
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import app.pulse.providers.piped.models.PlaylistPreview as PipedPlaylistPreview

@Route
@Composable
fun HomePlaylists(
    onPlaylistClick: (Playlist) -> Unit,
    onPipedPlaylistClick: (Session, PipedPlaylistPreview) -> Unit,
    onSearchClick: () -> Unit
) = with(OrderPreferences) {
    val (colorPalette) = LocalAppearance.current

    var isCreatingANewPlaylist by rememberSaveable { mutableStateOf(false) }
    var isImportingPlaylist by rememberSaveable { mutableStateOf(false) }
    var isMenuVisible by rememberSaveable { mutableStateOf(false) }

    if (isImportingPlaylist) ImportPlaylistDialog(
        onDismiss = { isImportingPlaylist = false }
    )

    if (isCreatingANewPlaylist) TextFieldDialog(
        hintText = stringResource(R.string.enter_playlist_name_prompt),
        onDismiss = { isCreatingANewPlaylist = false },
        onAccept = { text ->
            query {
                Database.insert(Playlist(name = text))
            }
        }
    )
    var items by persistList<PlaylistPreview>("home/playlists")
    var pipedSessions by persist<Map<PipedSession, List<PipedPlaylistPreview>?>>("home/piped")

    LaunchedEffect(playlistSortBy, playlistSortOrder) {
        Database
            .playlistPreviews(playlistSortBy, playlistSortOrder)
            .collect { items = it.toImmutableList() }
    }

    LaunchedEffect(Unit) {
        Database.pipedSessions().collect { sessions ->
            pipedSessions = sessions.associateWith { session ->
                async {
                    Piped.playlist.list(session = session.toApiSession())?.getOrNull()
                }
            }.mapValues { (_, value) -> value.await() }
        }
    }

    val lazyGridState = rememberLazyGridState()

    Box {
    CollapsingHeader(
        title = stringResource(R.string.playlists),
        lazyGridState = lazyGridState,
        headerActions = {
            val (pillPalette, _) = LocalAppearance.current
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(pillPalette.background2)
                    .border(0.5.dp, pillPalette.textSecondary.copy(alpha = 0.35f), RoundedCornerShape(percent = 50))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            HeaderIconButton(
                icon = R.drawable.add,
                onClick = { isCreatingANewPlaylist = true }
            )
            HeaderIconButton(
                icon = R.drawable.import_playlist, // this is the playlist page
                onClick = { isImportingPlaylist = true }
            )
            Box {
                HeaderIconButton(
                    icon = R.drawable.hamburger,
                    onClick = { isMenuVisible = !isMenuVisible }
                )


                Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                    NewMenu(
                    visible = isMenuVisible,
                    onDismiss = { isMenuVisible = false }
                ) {
                NewMenuEntry(
                    icon = R.drawable.medical,
                    text = "Sort by song count",
                    checked = playlistSortBy == PlaylistSortBy.SongCount,
                    onClick = {
                        playlistSortBy = PlaylistSortBy.SongCount
                        isMenuVisible = false
                    }
                )
                NewMenuEntry(
                    icon = R.drawable.text,
                    text = "Sort by name",
                    checked = playlistSortBy == PlaylistSortBy.Name,
                    onClick = {
                        playlistSortBy = PlaylistSortBy.Name
                        isMenuVisible = false
                    }
                )
                NewMenuEntry(
                    icon = R.drawable.time,
                    text = "Sort by date added",
                    checked = playlistSortBy == PlaylistSortBy.DateAdded,
                    onClick = {
                        playlistSortBy = PlaylistSortBy.DateAdded
                        isMenuVisible = false
                    }
                )

                NewMenuDivider()

                NewMenuEntry(
                    icon = R.drawable.arrow_up,
                    text = "Sort order",
                    secondaryText = if (playlistSortOrder == SortOrder.Ascending) "Ascending" else "Descending",
                    onClick = {
                        playlistSortOrder = !playlistSortOrder
                        isMenuVisible = false
                    }
                )
                NewMenuEntry(
                    icon = if (UIStatePreferences.playlistsAsGrid) R.drawable.grid else R.drawable.list,
                    text = "Layout",
                    secondaryText = if (UIStatePreferences.playlistsAsGrid) "Grid" else "List",
                    onClick = {
                        UIStatePreferences.playlistsAsGrid = !UIStatePreferences.playlistsAsGrid
                        isMenuVisible = false
                    }
                )
                }
                }                }
                }
            }
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = if (UIStatePreferences.playlistsAsGrid)
                GridCells.Adaptive(Dimensions.thumbnails.playlist + Dimensions.items.alternativePadding * 2)
            else GridCells.Fixed(1),
            contentPadding = PaddingValues(
                top = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
                    .calculateTopPadding() + 32.dp,
                bottom = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
                    .calculateBottomPadding()
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.items.alternativePadding),
            verticalArrangement = if (UIStatePreferences.playlistsAsGrid)
                Arrangement.spacedBy(Dimensions.items.alternativePadding)
            else Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .background(colorPalette.background0)
        ) {
            item(key = "spacer", span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(80.dp))
            }

            items(
                items = items,
                key = { it.playlist.id }
            ) { playlistPreview ->
                PlaylistItem(
                    playlist = playlistPreview,
                    thumbnailSize = if (UIStatePreferences.playlistsAsGrid) Dimensions.thumbnails.playlist else Dimensions.thumbnails.playlist - 24.dp,
                    alternative = UIStatePreferences.playlistsAsGrid,
                    showChevron = true,
                    modifier = Modifier
                        .clickable(onClick = { onPlaylistClick(playlistPreview.playlist) })
                        .animateItem(fadeInSpec = null, fadeOutSpec = null)
                )
            }

            pipedSessions
                ?.ifEmpty { null }
                ?.filter { it.value?.isNotEmpty() == true }
                ?.forEach { (session, playlists) ->
                    item(
                        key = "piped-header-${session.username}",
                        contentType = 0,
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        SettingsGroupSpacer()
                        SettingsEntryGroupText(title = session.username)
                    }

                    playlists?.let {
                        items(
                            items = playlists,
                            key = { "piped-${session.username}-${it.id}" }
                        ) { playlist ->
                            PlaylistItem(
                                name = playlist.name,
                                songCount = playlist.videoCount,
                                channelName = null,
                                thumbnailUrl = playlist.thumbnailUrl.toString(),
                                thumbnailSize = if (UIStatePreferences.playlistsAsGrid) Dimensions.thumbnails.playlist else Dimensions.thumbnails.playlist - 8.dp,
                                alternative = UIStatePreferences.playlistsAsGrid,
                                showChevron = true,
                                modifier = Modifier
                                    .clickable(onClick = {
                                        onPipedPlaylistClick(
                                            session.toApiSession(),
                                            playlist
                                        )
                                    })
                                    .animateItem(fadeInSpec = null, fadeOutSpec = null)
                            )
                        }
                    }
                }
        }
    }

    FloatingActionsContainerWithScrollToTop(
            lazyGridState = lazyGridState,
            icon = null
        )
    }
}
