package app.pulse.android.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pulse.android.Database
import app.pulse.android.LocalPlayerAwareWindowInsets
import app.pulse.android.R
import app.pulse.android.models.Album
import app.pulse.android.preferences.OrderPreferences
import app.pulse.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.pulse.android.ui.components.themed.CollapsingHeader
import app.pulse.android.ui.components.themed.CollapsingHeaderContentSpacer
import app.pulse.android.ui.components.themed.HeaderIconButton
import app.pulse.android.ui.components.themed.HeaderPillRow
import app.pulse.android.ui.components.NewMenu
import app.pulse.android.ui.components.NewMenuDivider
import app.pulse.android.ui.components.NewMenuEntry
import app.pulse.android.ui.items.AlbumItem
import app.pulse.android.ui.screens.Route
import app.pulse.compose.persist.persist
import app.pulse.core.data.enums.AlbumSortBy
import app.pulse.core.data.enums.SortOrder
import app.pulse.core.ui.Dimensions
import app.pulse.core.ui.LocalAppearance

@Route
@Composable
fun HomeAlbums(
    onAlbumClick: (Album) -> Unit,
    onSearchClick: () -> Unit
) = with(OrderPreferences) {
    val (colorPalette) = LocalAppearance.current

    var items by persist<List<Album>>(tag = "home/albums", emptyList())

    LaunchedEffect(albumSortBy, albumSortOrder) {
        Database.albums(albumSortBy, albumSortOrder).collect { items = it }
    }

    var isMenuVisible by rememberSaveable { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    Box {
    CollapsingHeader(
        title = stringResource(R.string.albums),
        lazyListState = lazyListState,
        headerActions = {
            HeaderPillRow {
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
                            icon = R.drawable.calendar,
                            text = "Sort by year",
                            checked = albumSortBy == AlbumSortBy.Year,
                            onClick = {
                                albumSortBy = AlbumSortBy.Year
                                isMenuVisible = false
                            }
                        )
                        NewMenuEntry(
                            icon = R.drawable.text,
                            text = "Sort by title",
                            checked = albumSortBy == AlbumSortBy.Title,
                            onClick = {
                                albumSortBy = AlbumSortBy.Title
                                isMenuVisible = false
                            }
                        )
                        NewMenuEntry(
                            icon = R.drawable.time,
                            text = "Sort by date added",
                            checked = albumSortBy == AlbumSortBy.DateAdded,
                            onClick = {
                                albumSortBy = AlbumSortBy.DateAdded
                                isMenuVisible = false
                            }
                        )

                        NewMenuDivider()

                        NewMenuEntry(
                            icon = R.drawable.arrow_up,
                            text = "Sort order",
                            secondaryText = if (albumSortOrder == SortOrder.Ascending) "Ascending" else "Descending",
                            onClick = {
                                albumSortOrder = !albumSortOrder
                                isMenuVisible = false
                            }
                        )
                    }
                }
            }
            }
        }
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(CollapsingHeaderContentSpacer))
            }

            items(
                items = items,
                key = Album::id
            ) { album ->
                AlbumItem(
                    album = album,
                    thumbnailSize = Dimensions.thumbnails.album,
                    modifier = Modifier
                        .clickable(onClick = { onAlbumClick(album) })
                        .animateItem()
                )
            }
        }
    }

    FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            icon = null
        )
    }
}
