---
goal: Migrate Pulse Android to PulseKMP Phase 1 (Foundation)
version: 2.0
date_created: 2026-05-08
status: 'Planned'
tags: [architecture, migration, foundation, kmp]
---

# Introduction

![Status: Planned](https://img.shields.io/badge/status-Planned-blue)

This plan covers Phase 1 of the migration from `pulse` (single-platform Android,
`~/StudioProjects/pulse/`) to `PulseKMP` (Kotlin Multiplatform,
`~/AndroidStudioProjects/PulseKMP/`). The goal is to establish the shared
infrastructure — dependency management, core models, persistence, and the
"Headless Music Engine" interfaces — without touching the existing `pulse` app,
which continues to run as the reference implementation.

## Observed Project State

### `pulse/` — Source of Truth
- **Package root**: `app.pulse.android`
- **Single Gradle module for app code**: `app/`
- **Separate Gradle modules**: `core:data`, `core:ui`, `core:material-compat`,
  `compose:persist`, `compose:preferences`, `compose:reordering`, `compose:routing`,
  `ktor-client-brotli`
- **Provider modules** (all separate Gradle modules):
  `providers:common`, `providers:innertube`, `providers:piped`, `providers:kugou`,
  `providers:lrclib`, `providers:sponsorblock`, `providers:translate`, `providers:github`
- **Models to migrate** (from `app/src/main/kotlin/app/pulse/android/models/`):
  `Song`, `Album`, `Artist`, `Playlist`, `PlaylistPreview`, `PlaylistWithSongs`,
  `SongAlbumMap`, `SongArtistMap`, `SongPlaylistMap`, `SortedSongPlaylistMap`,
  `QueuedMediaItem`, `Event`, `EventWithSong`, `Format`, `Info`, `Lyrics`, `Mood`,
  `PipedSession`, `SearchQuery`, `SongWithContentLength`
- **Existing DB**: `app/src/main/kotlin/app/pulse/android/Database.kt`
- **Player surface**: `app/src/main/kotlin/app/pulse/android/utils/PlayerState.kt`,
  `utils/Player.kt`, `utils/ExoPlayer.kt`
- **Service**: `service/PlayerService.kt` (the God Object)

### `PulseKMP/` — Clean Destination (Current State)
- **Package root**: `com.elza.pulsekmp` ← **needs alignment decision (see NOTE-001)**
- **Modules present**: `composeApp/`, `shared/`, `server/`
- **Targets already configured in `composeApp`**: `androidMain`, `iosMain`, `jvmMain`,
  `webMain` (web target should be **removed** — not in scope per design spec)
- **`shared/` targets**: `androidMain`, `iosMain`, `jsMain`, `jvmMain`, `wasmJsMain`
  — js/wasm/web targets need to be **pruned** to Android + iOS + JVM (Desktop) only
- **Current shared content**: Only scaffold (`Greeting.kt`, `Platform.kt`,
  `Constants.kt`) — effectively empty, ready for migration
- **`server/` module**: Ktor server scaffold — **not in scope**, leave untouched

> **NOTE-001 — Package naming decision required before TASK-001**:
> `pulse` uses `app.pulse.*`; `PulseKMP` currently uses `com.elza.pulsekmp`.
> The design spec (CON-001) says to preserve existing package naming where possible.
> Decision: migrate shared code under `app.pulse.*` packages inside `shared/`,
> keeping `com.elza.pulsekmp` only for the `composeApp` entry points. This minimises
> import churn when moving files from `pulse`.

---

## 1. Requirements & Constraints

- **REQ-001**: Kotlin 2.3.x, Compose Multiplatform 1.10.x
- **REQ-002**: Room KMP `2.7.0-alpha01+` with `BundledSQLiteDriver` for iOS/Desktop
- **REQ-003**: Ktor 3.4.2+ (already used by `pulse` providers via `ktor-client-brotli`)
- **REQ-004**: `shared` module targets: `androidTarget`, `iosArm64`, `iosSimulatorArm64`,
  `jvm` (Desktop). Remove `js`, `wasmJs`, `web` targets from both `shared` and
  `composeApp`.
- **CON-001**: Shared source packages use `app.pulse.*`; `composeApp` entry points
  keep `com.elza.pulsekmp`
- **CON-002**: `pulse/` is not modified during Phase 1 — it remains the live reference
- **PAT-001**: `expect/actual` for database builders and platform capabilities
- **PAT-002**: All shared interfaces defined in
  `shared/src/commonMain/kotlin/app/pulse/`

---

## 2. Implementation Steps

### Phase 1-A: Project Cleanup & Gradle Foundation

**GOAL-001**: Align `PulseKMP` build files with the 3-target (Android/iOS/Desktop)
scope and synchronise dependency versions from `pulse`.

| Task | Description | File(s) | Done |
|------|-------------|---------|------|
| TASK-001 | Remove `js`, `wasmJs`, and `web` source sets from `shared/build.gradle.kts`. Remove `webMain` from `composeApp/build.gradle.kts`. | `shared/build.gradle.kts`, `composeApp/build.gradle.kts` | |
| TASK-002 | Delete orphaned source directories: `shared/src/jsMain/`, `shared/src/wasmJsMain/`, `composeApp/src/webMain/` | filesystem | |
| TASK-003 | Sync `PulseKMP/gradle/libs.versions.toml` from `pulse/gradle/libs.versions.toml`: carry over Room, Ktor, Coil, Kotlinx-serialization, KSP versions. Upgrade Coil → v3, Room → `2.7.0-alpha01`. | `gradle/libs.versions.toml` | |
| TASK-004 | Add to `shared/build.gradle.kts` commonMain dependencies: `room-runtime`, `room-ktx`, `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `kotlinx-serialization-json`, `coil-compose`. Add platform-specific Ktor engines: OkHttp (Android), Darwin (iOS), CIO (JVM/Desktop). | `shared/build.gradle.kts` | |
| TASK-005 | Apply `ksp` and `kotlin-serialization` plugins to `shared/build.gradle.kts`. Configure KSP for Room targeting all 3 platforms. | `shared/build.gradle.kts` | |
| TASK-006 | Verify `./gradlew :shared:assemble` compiles clean for all 3 targets with no source yet beyond scaffold. | — | |

---

### Phase 1-B: Core Models Migration

**GOAL-002**: Move data classes from `pulse/app/src/main/kotlin/app/pulse/android/models/`
into `shared/src/commonMain/kotlin/app/pulse/data/models/`, stripping Android
imports and annotating for Room KMP and Kotlinx-Serialization.

| Task | Description | Source → Destination | Done |
|------|-------------|---------------------|------|
| TASK-007 | Migrate `Song.kt` → `app/pulse/data/models/Song.kt`. Add `@Entity`, `@Serializable`. Strip any `android.*` imports. | `models/Song.kt` → `commonMain/.../models/Song.kt` | |
| TASK-008 | Migrate `Album.kt`, `Artist.kt`, `Playlist.kt`, `PlaylistPreview.kt` | `models/Album.kt` etc. → `commonMain` | |
| TASK-009 | Migrate junction/map models: `SongAlbumMap`, `SongArtistMap`, `SongPlaylistMap`, `SortedSongPlaylistMap`, `PlaylistWithSongs`, `SongWithContentLength` | `models/*.kt` → `commonMain` | |
| TASK-010 | Migrate supplementary models: `Event`, `EventWithSong`, `Format`, `Info`, `Lyrics`, `Mood`, `PipedSession`, `SearchQuery`, `QueuedMediaItem` | `models/*.kt` → `commonMain` | |
| TASK-011 | Migrate `core/data/enums/`: `AlbumSortBy`, `ArtistSortBy`, `BuiltInPlaylist`, `PlaylistSortBy`, `SongSortBy`, `SortOrder`, `CoilDiskCacheSize`, `ExoPlayerDiskCacheSize` → `app/pulse/data/enums/` | `core/data/src/.../enums/` → `commonMain` | |
| TASK-012 | Verify models compile in `commonMain` (no Android dependencies remain). `./gradlew :shared:compileKotlinMetadata` | — | |

---

### Phase 1-C: Playback Interfaces

**GOAL-003**: Define the reactive `PulsePlayer` contract and `PlayerController`
orchestrator in `commonMain`.

| Task | Description | Destination File | Done |
|------|-------------|-----------------|------|
| TASK-013 | Create `PlayerState.kt` with sealed class `PlayerState { Idle, Loading, Playing, Paused, Ended, Error }` | `commonMain/.../player/PlayerState.kt` | |
| TASK-014 | Create `PlaybackPosition.kt` with `data class PlaybackPosition(val positionMs: Long, val durationMs: Long, val percent: Float)` | `commonMain/.../player/PlaybackPosition.kt` | |
| TASK-015 | Create `InterruptionState.kt` enum: `GAINED, LOST, LOST_TRANSIENT, LOST_LONG` | `commonMain/.../player/InterruptionState.kt` | |
| TASK-016 | Create `PulsePlayer.kt` interface with `StateFlow<PlayerState>`, `StateFlow<Track?>`, `StateFlow<PlaybackPosition>`, `StateFlow<Boolean> isBuffering`, and all command functions (`play`, `pause`, `resume`, `stop`, `seekTo`, `skipNext`, `skipPrevious`, `setQueue`, `handleInterruption`) | `commonMain/.../player/PulsePlayer.kt` | |
| TASK-017 | Create `PlayerController.kt` stub in `commonMain`. Holds `PulsePlayer` reference, manages queue `List<Song>`, exposes queue state as `StateFlow<List<Song>>`. Queue mutation logic (shuffle, reorder) lives here, not in `PulsePlayer`. | `commonMain/.../player/PlayerController.kt` | |
| TASK-018 | Create `NoOpPlayer.kt` implementing `PulsePlayer` with no-op stubs. Used as placeholder until platform implementations exist. | `commonMain/.../player/NoOpPlayer.kt` | |

---

### Phase 1-D: Platform Capabilities

**GOAL-004**: Define the `PlatformCapabilities` interface and provide it via
`CompositionLocal` so shared UI never branches on platform directly.

| Task | Description | Destination File | Done |
|------|-------------|-----------------|------|
| TASK-019 | Create `PlatformCapabilities.kt` interface with: `supportsDynamicColor: Boolean`, `supportsBackgroundService: Boolean`, `supportsEqualizer: Boolean`, `supportsSystemMediaControls: Boolean` | `commonMain/.../platform/PlatformCapabilities.kt` | |
| TASK-020 | Define `LocalPlatformCapabilities` as `staticCompositionLocalOf<PlatformCapabilities>` with `error("No PlatformCapabilities provided")` default | same file or `PlatformCapabilitiesLocal.kt` | |
| TASK-021 | Create `expect` declarations: `expect fun defaultPlatformCapabilities(): PlatformCapabilities` | `commonMain/.../platform/PlatformCapabilities.kt` | |
| TASK-022 | Implement `actual` for Android (`androidMain`): `supportsDynamicColor = Build.VERSION.SDK_INT >= 31`, `supportsBackgroundService = true`, `supportsEqualizer = true`, `supportsSystemMediaControls = true` | `androidMain/.../platform/PlatformCapabilities.android.kt` | |
| TASK-023 | Implement `actual` for iOS (`iosMain`): `supportsDynamicColor = false`, `supportsBackgroundService = false` (AVPlayer in-process), `supportsEqualizer = false`, `supportsSystemMediaControls = true` | `iosMain/.../platform/PlatformCapabilities.ios.kt` | |
| TASK-024 | Implement `actual` for Desktop (`jvmMain`): `supportsDynamicColor = false`, `supportsBackgroundService = false`, `supportsEqualizer = false`, `supportsSystemMediaControls = true` (MPRIS/SMTC Phase 4) | `jvmMain/.../platform/PlatformCapabilities.jvm.kt` | |

---

### Phase 1-E: Room KMP Persistence

**GOAL-005**: Establish the shared database with real entities from `pulse`,
validated on all 3 platforms.

| Task | Description | Destination File | Done |
|------|-------------|-----------------|------|
| TASK-025 | Create `PulseDatabase.kt` in `commonMain` as `@Database` with entities from TASK-007–011. Reference `pulse/app/src/main/kotlin/app/pulse/android/Database.kt` for schema version and migration history. | `commonMain/.../data/db/PulseDatabase.kt` | |
| TASK-026 | Migrate DAOs: extract DAO interfaces from `pulse/Database.kt` (they are likely inner interfaces or separate files — verify) into `commonMain/.../data/db/dao/` — one file per entity group (`SongDao`, `AlbumDao`, `ArtistDao`, `PlaylistDao`). | `commonMain/.../data/db/dao/*.kt` | |
| TASK-027 | Define `expect fun getDatabaseBuilder(context: Any?): RoomDatabase.Builder<PulseDatabase>` in `commonMain` | `commonMain/.../data/db/DatabaseFactory.kt` | |
| TASK-028 | Implement `actual` for Android using `Room.databaseBuilder(context, ...)` | `androidMain/.../data/db/DatabaseFactory.android.kt` | |
| TASK-029 | Implement `actual` for iOS using `BundledSQLiteDriver` + `NSHomeDirectory()` path | `iosMain/.../data/db/DatabaseFactory.ios.kt` | |
| TASK-030 | Implement `actual` for Desktop (JVM) using `BundledSQLiteDriver` + `System.getProperty("user.home")` path | `jvmMain/.../data/db/DatabaseFactory.jvm.kt` | |
| TASK-031 | **Validation**: Write a `commonTest` unit test that instantiates `PulseDatabase` in-memory and performs a round-trip insert/query on `SongDao`. Run `./gradlew :shared:testDebugUnitTest` and `:shared:iosSimulatorArm64Test`. | `commonTest/.../data/db/PulseDatabaseTest.kt` | |

---

### Phase 1-F: Final Verification

| Task | Description | Done |
|------|-------------|------|
| TASK-032 | `./gradlew :shared:assemble` — all 3 targets compile cleanly | |
| TASK-033 | `./gradlew :shared:allTests` — `PulseDatabaseTest` and `PlayerController` queue tests pass | |
| TASK-034 | `./gradlew :composeApp:assembleDebug` — Android app builds and launches (shows scaffold, no regression) | |
| TASK-035 | Confirm `pulse/` Android app still builds and runs unmodified (`./gradlew :app:assembleDebug` from `pulse/`) | |

---

## 3. Explicitly Out of Scope for Phase 1

- Provider migration (`providers:innertube`, `providers:piped`, etc.) → Phase 2
- UI screen migration → Phase 3
- `AVPlayer` / LibVLC implementations → Phase 2 spike
- `server/` module in `PulseKMP` — leave as-is, not part of migration
- `web` / `wasmJs` targets — removed in TASK-001/002, not revisited

---

## 4. Key File Inventory

| File | Origin | Phase 1 Action |
|------|--------|---------------|
| `pulse/gradle/libs.versions.toml` | Source | Read; sync versions to PulseKMP |
| `pulse/app/src/.../models/*.kt` | Source | Copy + adapt (TASK-007–010) |
| `pulse/core/data/src/.../enums/*.kt` | Source | Copy + adapt (TASK-011) |
| `pulse/app/src/.../Database.kt` | Source | Read only; schema reference (TASK-025–026) |
| `pulse/app/src/.../utils/PlayerState.kt` | Source | Reference for state modelling (TASK-013) |
| `PulseKMP/shared/build.gradle.kts` | Destination | Rewrite (TASK-001, 004, 005) |
| `PulseKMP/gradle/libs.versions.toml` | Destination | Sync (TASK-003) |
| `PulseKMP/shared/src/commonMain/.../player/` | Destination | Create (TASK-013–018) |
| `PulseKMP/shared/src/commonMain/.../data/` | Destination | Create (TASK-025–030) |
| `PulseKMP/shared/src/commonMain/.../platform/` | Destination | Create (TASK-019–024) |

---

## 5. Risks & Assumptions

| ID | Type | Detail |
|----|------|--------|
| RISK-001 | Binary compat | Kotlin 2.3.x + Room KMP alpha — pin exact versions in `libs.versions.toml` and do not use `+` wildcards |
| RISK-002 | Schema drift | `pulse/Database.kt` likely has migrations — verify schema version before TASK-025; carry the same version number into KMP to avoid future migration conflicts |
| RISK-003 | KSP multi-target | Room's KSP config for `iosArm64`/`iosSimulatorArm64` requires explicit KSP task wiring — follow Room KMP sample project setup exactly |
| RISK-004 | `QueuedMediaItem` uses Media3 | `QueuedMediaItem.kt` wraps `androidx.media3.common.MediaItem` — this cannot move to `commonMain` as-is. Create a KMP-safe `QueueItem(song: Song, positionMs: Long)` replacement and map to `MediaItem` in `androidMain` only. |
| ASSUMPTION-001 | DAOs in Database.kt | Assumed DAOs are defined inside or alongside `pulse/app/src/.../Database.kt`. If they are scattered across feature modules, TASK-026 scope expands. Verify before starting Phase 1-E. |
| ASSUMPTION-002 | No web target | Design spec targets Android + iOS + Desktop only. `server/` and web modules in `PulseKMP` are not part of this migration. |

---

## 6. Related Documents

- `docs/specs/2026-05-08-pulsekmp-migration-design.md` — master architecture spec
- `pulse/app/src/main/kotlin/app/pulse/android/Database.kt` — Room schema reference
- `pulse/app/src/main/kotlin/app/pulse/android/service/PlayerService.kt` — God Object to decompose in Phase 2
