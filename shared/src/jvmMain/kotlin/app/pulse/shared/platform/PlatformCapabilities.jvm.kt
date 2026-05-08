package app.pulse.shared.platform

class JVMPlatformCapabilities : PlatformCapabilities {
    override val supportsDynamicColor: Boolean = false
    override val supportsBackgroundService: Boolean = false
    override val supportsEqualizer: Boolean = false
    override val supportsSystemMediaControls: Boolean = true
}

actual fun defaultPlatformCapabilities(): PlatformCapabilities = JVMPlatformCapabilities()
