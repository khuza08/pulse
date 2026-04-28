@file:Suppress("TooManyFunctions")

package app.pulse.android.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pulse.android.R
import app.pulse.android.ui.components.themed.Header
import app.pulse.android.ui.components.themed.HeaderIconButton
import app.pulse.android.ui.components.themed.Scaffold
import app.pulse.android.ui.screens.GlobalRoutes
import app.pulse.android.ui.screens.Route
import app.pulse.android.ui.screens.aboutSettingsRoute
import app.pulse.android.ui.screens.appearanceSettingsRoute
import app.pulse.android.ui.screens.cacheSettingsRoute
import app.pulse.android.ui.screens.databaseSettingsRoute
import app.pulse.android.ui.screens.logsRoute
import app.pulse.android.ui.screens.otherSettingsRoute
import app.pulse.android.ui.screens.playerSettingsRoute
import app.pulse.android.ui.screens.syncSettingsRoute
import app.pulse.android.utils.color
import app.pulse.android.utils.secondary
import app.pulse.android.utils.semiBold
import app.pulse.compose.routing.RouteHandler
import app.pulse.core.ui.LocalAppearance

@Route
@Composable
fun SettingsScreen() {
    RouteHandler {
        GlobalRoutes()

        Content {
            Scaffold(
                key = "settings",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = 0,
                onTabChange = {},
                tabColumnContent = {
                    tab(0, R.string.settings, R.drawable.settings, canHide = false)
                }
            ) {
                MasterSettingsCategoryScreen(title = stringResource(R.string.settings)) {
                    MasterSettingsGroup(title = stringResource(R.string.appearance)) {
                        SettingsMenuEntry(
                            title = stringResource(R.string.appearance),
                            description = stringResource(R.string.appearance_description),
                            icon = R.drawable.color_palette,
                            onClick = { appearanceSettingsRoute.global() }
                        )
                    }

                    MasterSettingsGroup(title = stringResource(R.string.player)) {
                        SettingsMenuEntry(
                            title = stringResource(R.string.player),
                            description = stringResource(R.string.player_description),
                            icon = R.drawable.play,
                            onClick = { playerSettingsRoute.global() }
                        )
                    }

                    MasterSettingsGroup(title = stringResource(R.string.cache_and_database)) {
                        SettingsMenuEntry(
                            title = stringResource(R.string.cache),
                            description = stringResource(R.string.cache_description),
                            icon = R.drawable.settings,
                            onClick = { cacheSettingsRoute.global() }
                        )
                        SettingsMenuEntry(
                            title = stringResource(R.string.database),
                            description = stringResource(R.string.database_description),
                            icon = R.drawable.settings,
                            onClick = { databaseSettingsRoute.global() }
                        )
                    }

                    MasterSettingsGroup(title = stringResource(R.string.sync)) {
                        SettingsMenuEntry(
                            title = stringResource(R.string.sync),
                            description = stringResource(R.string.sync_description_short),
                            icon = R.drawable.settings,
                            onClick = { syncSettingsRoute.global() }
                        )
                    }

                    MasterSettingsGroup(title = stringResource(R.string.other)) {
                        SettingsMenuEntry(
                            title = stringResource(R.string.other),
                            description = stringResource(R.string.other_description),
                            icon = R.drawable.settings,
                            onClick = { otherSettingsRoute.global() }
                        )
                        SettingsMenuEntry(
                            title = stringResource(R.string.logs),
                            description = stringResource(R.string.logs_description),
                            icon = R.drawable.settings,
                            onClick = { logsRoute.global() }
                        )
                    }

                    MasterSettingsGroup(title = stringResource(R.string.about)) {
                        SettingsMenuEntry(
                            title = stringResource(R.string.about),
                            description = stringResource(R.string.about_description),
                            icon = R.drawable.settings,
                            onClick = { aboutSettingsRoute.global() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MasterSettingsCategoryScreen(
    title: String,
    description: String? = null,
    onBackClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 80.dp)
    ) {
        Header(
            title = title,
            modifier = Modifier.padding(top = 16.dp),
            onBackClick = onBackClick
        )

        if (description != null) {
            BasicText(
                text = description,
                style = typography.s.secondary,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    .alpha(0.7f)
            )
        }

        content()
    }
}

@Composable
fun MasterSettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        BasicText(
            text = title.uppercase(),
            style = typography.xs.semiBold.secondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colorPalette.background1)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsMenuEntry(
    title: String,
    description: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.text),
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = typography.m.semiBold
            )
            BasicText(
                text = description,
                style = typography.s.secondary,
                modifier = Modifier.alpha(0.7f)
            )
        }

        Image(
            painter = painterResource(R.drawable.chevron_forward),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.text),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun Header(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val (colorPalette, typography) = LocalAppearance.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBackClick != null) {
            HeaderIconButton(
                icon = R.drawable.chevron_back,
                onClick = onBackClick
            )
        }

        BasicText(
            text = title,
            style = typography.xxl.semiBold
        )
    }
}
