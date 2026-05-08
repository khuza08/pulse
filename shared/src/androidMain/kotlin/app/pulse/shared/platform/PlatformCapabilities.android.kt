package app.pulse.shared.platform

import android.os.Build

class AndroidPlatformCapabilities : PlatformCapabilities {
    override val supportsDynamicColor: Boolean
        get() = Build.VERSION.SDK_INT >= 31
    override val supportsBackgroundService: Boolean
        get() = true
    override val supportsEqualizer: Boolean
        get() = true
    override val supportsSystemMediaControls: Boolean
        get() = true
}

actual fun defaultPlatformCapabilities(): PlatformCapabilities = AndroidPlatformCapabilities()
