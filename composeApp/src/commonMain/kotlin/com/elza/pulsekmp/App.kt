package com.elza.pulsekmp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pulse.shared.ui.theme.PulseTheme
import app.pulse.shared.ui.theme.LocalAppearance
import app.pulse.shared.ui.components.themed.CollapsingHeader
import app.pulse.shared.ui.components.themed.SegmentedControl

@Composable
fun App() {
    PulseTheme {
        val appearance = LocalAppearance.current
        val colorPalette = appearance.colorPalette
        var selectedTab by remember { mutableStateOf(0) }
        
        CollapsingHeader(
            title = "Pulse KMP",
            scrollState = rememberScrollState(),
            content = {
                Box(modifier = Modifier.fillMaxSize().padding(top = 120.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SegmentedControl(
                            segments = listOf("Songs", "Albums", "Artists"),
                            selectedSegment = selectedTab,
                            onSegmentSelected = { selectedTab = it }
                        )
                        
                        Text(
                            "Selected: ${when(selectedTab) {
                                0 -> "Songs"
                                1 -> "Albums"
                                else -> "Artists"
                            }}",
                            color = colorPalette.text,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        )
    }
}