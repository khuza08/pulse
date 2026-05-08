package app.pulse.shared.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import app.pulse.shared.ui.theme.LocalAppearance
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// Stub for Tab model until migrated
data class Tab(val id: String, val title: String)

data class NavigationState(
    val tabs: ImmutableList<Tab>,
    val tabIndex: Int,
    val onTabChange: (Int) -> Unit,
    val hiddenTabs: ImmutableList<String>
)

val LocalNavigationState = staticCompositionLocalOf<MutableState<NavigationState?>> {
    mutableStateOf(null)
}

val LocalDockHiddenCount = staticCompositionLocalOf<MutableState<Int>> {
    mutableStateOf(0)
}

val LocalDockScrolled = staticCompositionLocalOf<MutableState<Boolean>> {
    mutableStateOf(false)
}

@Composable
fun Scaffold(
    key: String,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isGlobalNav: Boolean = false,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    
    // TODO: Migrate UIStatePreferences to KMP DataStore
    var hiddenTabs by remember { mutableStateOf(persistentListOf<String>()) }
    
    // TODO: Implement TabsBuilder for KMP
    val tabs = remember { persistentListOf<Tab>() }
    
    val globalNavigationState = LocalNavigationState.current

    if (isGlobalNav) {
        DisposableEffect(tabs, tabIndex, onTabChange, hiddenTabs) {
            val previousState = globalNavigationState.value
            globalNavigationState.value = NavigationState(
                tabs = tabs,
                tabIndex = tabIndex,
                onTabChange = onTabChange,
                hiddenTabs = hiddenTabs
            )
            onDispose {
                globalNavigationState.value = previousState
            }
        }
    }

    val dockScrolled = LocalDockScrolled.current
    LaunchedEffect(tabIndex) {
        if (isGlobalNav) {
            dockScrolled.value = false
        }
    }

    Row(
        modifier = modifier
            .background(colorPalette.background0)
            .fillMaxSize()
    ) {
        // NavigationRail would go here if landscape
        
        Column(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = tabIndex,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val slideDirection = if (targetState > initialState) Left else Right
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
                label = "ScaffoldContent"
            )
        }
    }
}
