package app.pulse.android.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import app.pulse.android.R
import app.pulse.android.preferences.UIStatePreferences
import app.pulse.core.ui.LocalAppearance
import app.pulse.core.ui.utils.isLandscape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class NavigationState(
    val tabs: ImmutableList<Tab>,
    val tabIndex: Int,
    val onTabChange: (Int) -> Unit,
    val hiddenTabs: ImmutableList<String>
)

val LocalNavigationState = staticCompositionLocalOf<MutableState<NavigationState?>> {
    mutableStateOf(null)
}
@Composable
fun Scaffold(
    key: String,
    topIconButtonId: Int,
    onTopIconButtonClick: () -> Unit,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    tabColumnContent: TabsBuilder.() -> Unit,
    modifier: Modifier = Modifier,
    tabsEditingTitle: String = stringResource(R.string.tabs),
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    var hiddenTabs by UIStatePreferences.mutableTabStateOf(key)
    val tabs = TabsBuilder.rememberTabs(tabColumnContent)
    val isLandscape = isLandscape
    val globalNavigationState = LocalNavigationState.current

    DisposableEffect(tabs, tabIndex, onTabChange, hiddenTabs) {
        globalNavigationState.value = NavigationState(
            tabs = tabs,
            tabIndex = tabIndex,
            onTabChange = onTabChange,
            hiddenTabs = hiddenTabs
        )
        onDispose {
            globalNavigationState.value = null
        }
    }

    Row(
        modifier = modifier
            .background(colorPalette.background0)
            .fillMaxSize()
    ) {
            if (isLandscape) {
                NavigationRail(
                    topIconButtonId = topIconButtonId,
                    onTopIconButtonClick = onTopIconButtonClick,
                    tabIndex = tabIndex,
                    onTabIndexChange = onTabChange,
                    hiddenTabs = hiddenTabs,
                    setHiddenTabs = { hiddenTabs = it.toImmutableList() },
                    tabsEditingTitle = tabsEditingTitle,
                    content = tabColumnContent
                )
            }

            AnimatedContent(
                targetState = tabIndex,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val slideDirection = if (targetState > initialState) Up else Down
                    val animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )

                    ContentTransform(
                        targetContentEnter = slideIntoContainer(slideDirection, animationSpec),
                        initialContentExit = slideOutOfContainer(slideDirection, animationSpec),
                        sizeTransform = null
                    )
                },
                content = content,
                label = ""
            )
        }
}
