package app.pulse.android.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import app.pulse.android.models.Artist
import app.pulse.android.preferences.OrderPreferences
import app.pulse.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.pulse.android.ui.components.themed.CollapsingHeader
import app.pulse.android.ui.components.themed.CollapsingHeaderContentSpacer
import app.pulse.android.ui.components.themed.HeaderIconButton
import app.pulse.android.ui.components.themed.HeaderPillRow
import app.pulse.android.ui.components.NewMenu
import app.pulse.android.ui.components.NewMenuDivider
import app.pulse.android.ui.components.NewMenuEntry
import app.pulse.android.ui.items.ArtistItem
import app.pulse.android.ui.screens.Route
import app.pulse.compose.persist.persistList
import app.pulse.core.data.enums.ArtistSortBy
import app.pulse.core.data.enums.SortOrder
import app.pulse.core.ui.Dimensions
import app.pulse.core.ui.LocalAppearance
import kotlinx.collections.immutable.toImmutableList

@Route
@Composable
fun HomeArtistList(
    onArtistClick: (Artist) -> Unit,
    onSearchClick: () -> Unit
) = with(OrderPreferences) {
    val (colorPalette) = LocalAppearance.current

    var items by persistList<Artist>("home/artists")

    LaunchedEffect(artistSortBy, artistSortOrder) {
        Database
            .artists(artistSortBy, artistSortOrder)
            .collect { items = it.toImmutableList() }
    }

    var isMenuVisible by rememberSaveable { mutableStateOf(false) }

    val lazyGridState = rememberLazyGridState()

    Box {
    CollapsingHeader(
        title = stringResource(R.string.artists),
        lazyGridState = lazyGridState,
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
                                icon = R.drawable.text,
                                text = "Sort by name",
                                checked = artistSortBy == ArtistSortBy.Name,
                                onClick = {
                                    artistSortBy = ArtistSortBy.Name
                                    isMenuVisible = false
                                }
                            )
                            NewMenuEntry(
                                icon = R.drawable.time,
                                text = "Sort by date added",
                                checked = artistSortBy == ArtistSortBy.DateAdded,
                                onClick = {
                                    artistSortBy = ArtistSortBy.DateAdded
                                    isMenuVisible = false
                                }
                            )

                            NewMenuDivider()

                            NewMenuEntry(
                                icon = R.drawable.arrow_up,
                                text = "Sort order",
                                secondaryText = if (artistSortOrder == SortOrder.Ascending) "Ascending" else "Descending",
                                onClick = {
                                    artistSortOrder = !artistSortOrder
                                    isMenuVisible = false
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(Dimensions.thumbnails.song * 2 + Dimensions.items.verticalPadding * 2),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues(),
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            item(key = "spacer", span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(CollapsingHeaderContentSpacer))
            }

            items(items = items, key = Artist::id) { artist ->
                ArtistItem(
                    artist = artist,
                    thumbnailSize = Dimensions.thumbnails.song * 2,
                    alternative = true,
                    modifier = Modifier
                        .clickable(onClick = { onArtistClick(artist) })
                        .animateItem(fadeInSpec = null, fadeOutSpec = null)
                )
            }
        }
    }

    FloatingActionsContainerWithScrollToTop(
            lazyGridState = lazyGridState,
            icon = null
        )
    }
}
