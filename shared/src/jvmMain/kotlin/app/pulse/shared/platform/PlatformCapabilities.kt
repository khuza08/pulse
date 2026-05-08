package app.pulse.shared.platform

actual fun defaultPlatformCapabilities(): PlatformCapabilities = object : PlatformCapabilities {
    override val supportsDynamicColor: Boolean = false
    override val supportsBackgroundService: Boolean = false
    override val supportsEqualizer: Boolean = false
    override val supportsSystemMediaControls: Boolean = true
}
