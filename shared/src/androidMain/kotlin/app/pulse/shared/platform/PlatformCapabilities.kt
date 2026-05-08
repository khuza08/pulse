package app.pulse.shared.platform

actual fun defaultPlatformCapabilities(): PlatformCapabilities = object : PlatformCapabilities {
    override val supportsDynamicColor: Boolean = true
    override val supportsBackgroundService: Boolean = true
    override val supportsEqualizer: Boolean = true
    override val supportsSystemMediaControls: Boolean = true
}
