# PulseKMP Migration Design Specification

**Status**: Draft / Under Review
**Date**: 2026-05-08
**Topic**: Migrating `pulse` (Android) to `PulseKMP` (Kotlin Multiplatform)

## 1. Overview
This specification outlines the transition of the `pulse` music player from a single-platform Android application to a multiplatform project (KMP) targeting **Android**, **iOS**, and **Desktop (Linux/Windows)**. The goal is to maximize code sharing by building a "Headless Music Engine" in the `shared` module while preserving high-quality platform-specific playback.

## 2. Architecture: "Atomic Shell"
The migration uses an "Atomic Shell" approach, where the `PulseKMP` project serves as the clean destination, and logic is migrated incrementally from the original `pulse` codebase.

### 2.1 Module Mapping
| Original Module (`pulse/`) | PulseKMP Target | Shared Level |
|----------------------------|-----------------|--------------|
| `app` (UI Logic)           | `shared/commonMain/ui` | Shared (Compose Multiplatform) |
| `app` (Service/Player)     | `composeApp/androidMain` | Platform (Android Service) |
| `core:data`                | `shared/commonMain/data` | Shared (Room KMP) |
| `providers/*`              | `shared/commonMain/providers` | Shared (Ktor Consolidation) |
| `compose:*`                | `shared/commonMain/ui` | Shared (Design System) |

### 2.2 Platform Capabilities
To prevent platform-specific logic from leaking into shared UI, we will use a `PlatformCapabilities` system:
```kotlin
interface PlatformCapabilities {
    val supportsDynamicColor: Boolean
    val supportsBackgroundService: Boolean
    val supportsEqualizer: Boolean
    // Add flags as needed to toggle UI features per platform
}

// Consumption Pattern:
val LocalPlatformCapabilities = staticCompositionLocalOf<PlatformCapabilities> {
    error("No PlatformCapabilities provided")
}
```

## 3. Technology Stack
*   **UI Framework**: Compose Multiplatform - Shared UI for all platforms.
*   **Database**: Room KMP (`2.7.0-alpha01+`) - Shared persistence with platform-specific drivers (e.g., `BundledSQLiteDriver` for iOS).
*   **Networking**: Ktor Client 3.x - Multi-source providers (InnerTube, Piped, etc.) consolidated into `commonMain`.
*   **Image Loading**: Coil 3 (KMP).
*   **Playback Engine**: Abstract & Implement:
    *   **Android**: Media3 / ExoPlayer (via `PlayerService`).
    *   **iOS**: `AVPlayer` wrapper.
    *   **Desktop**: `LibVLC` (vlcj) wrapper.

## 4. Key Implementation Details

### 4.1 Headless Music Engine
All provider logic (`com.pulse.providers.*`) will be moved to `shared/commonMain`.
*   **Consolidation**: Instead of multiple Gradle modules, providers will be organized by package.
*   **Stubbing**: Python/Chaquopy logic (if used for YouTube-DL signatures) will be abstracted via `expect/actual` or a `MusicExtractor` interface. iOS/Desktop will use native Ktor-based extraction where possible or a stub until a native/JS-based extractor is ported.

### 4.2 Reactive Playback Abstraction
A reactive `PulsePlayer` interface will be defined in `commonMain`:
```kotlin
interface PulsePlayer {
    val state: StateFlow<PlayerState>
    val currentTrack: StateFlow<Track?>
    val playbackPosition: StateFlow<PlaybackPosition>
    val isBuffering: StateFlow<Boolean>
    
    fun play(item: Track)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()
    fun setQueue(items: List<Track>)
    fun handleInterruption(interruption: InterruptionState)
}

data class PlaybackPosition(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val percent: Float = 0f
)

enum class InterruptionState {
    GAINED, LOST, LOST_TRANSIENT, LOST_LONG
}
```

### 4.3 PlayerController (The Orchestrator)
A shared `PlayerController` in `commonMain` will act as the single source of truth, holding the `PulsePlayer` instance and coordinating:
*   Queue management and shuffling logic.
*   Syncing current playback state to the UI.
*   Persistence of "Now Playing" state to the database.

## 5. Phases (Refined)
1.  **Phase 1: Foundation**: 
    *   Setup `shared` module with Ktor, Room, and core models.
    *   Initialize `PlayerController` and `PulsePlayer` interfaces.
2.  **Phase 2: Spike & Logic**: 
    *   Port `providers` and `data` repositories to `shared/commonMain`.
    *   **Spike**: Create proof-of-concept `AVPlayer` (iOS) and `LibVLC` (Desktop) wrappers.
    *   **Spike Exit Criteria**:
        *   Successful playback of a direct URL (MP3/AAC).
        *   Successful playback of an HLS/DASH stream (YouTube/Piped source).
        *   Verification of background audio on iOS (using `AVAudioSession`).
        *   Verification of hardware media key response (Desktop).
3.  **Phase 3: UI migration**: 
    *   Migrate Compose screens and glassmorphic components to `shared/commonMain`.
    *   Implement `PlatformCapabilities` to handle UI branching.
4.  **Phase 4: Full Platform Integration**: Complete the `PulsePlayer` implementations, handle system media controls (MPRIS/SMTC/NowPlaying), and polish notifications.

## 6. Engineering Standards

### 6.1 Audio Focus & Interruptions
Each platform implementation must handle interruptions:
*   **Android**: Use `AudioManager` in the `PlayerService`.
*   **iOS**: `AVAudioSession` observers.
*   **Desktop**: Optional, but hook into system mute/unmute events.

### 6.2 Testing Strategy
*   **Shared Logic**: High coverage of business logic; integration tests for providers with recorded JSON fixtures.
*   **UI**: Use `ComposeTestRule` for shared component testing.
*   **Playback**: Integration tests on real devices (manual verification during Phase 2 spike).

### 6.3 CI/CD
*   GitHub Actions with macOS runners for iOS build validation.
*   Matrix builds for Linux/Windows artifacts.

## 7. Risks & Mitigations
*   **Risk**: `PlayerService` is a God Object.
    *   **Mitigation**: Mandatory extraction of state into shared `PlayerController` in Phase 1.
*   **Risk**: Desktop Codec Issues.
    *   **Mitigation**: Use LibVLC to bundle codecs.
*   **Risk**: Room KMP Maturity.
    *   **Mitigation**: Use stable SQLite drivers and validate WAL-mode on iOS during Phase 1.
