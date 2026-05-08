package app.pulse.shared.platform

import androidx.compose.runtime.staticCompositionLocalOf

interface PlatformCapabilities {
    val supportsDynamicColor: Boolean
    val supportsBackgroundService: Boolean
    val supportsEqualizer: Boolean
    val supportsSystemMediaControls: Boolean
}

val LocalPlatformCapabilities = staticCompositionLocalOf<PlatformCapabilities> {
    error("No PlatformCapabilities provided")
}

expect fun defaultPlatformCapabilities(): PlatformCapabilities
