---
goal: Migrate core foundation (models and dependencies) from pulse to PulseKMP for Android-first compilation
version: 1.0
date_created: 2026-05-08
status: 'Planned'
tags: [migration, android, kmp, foundation]
---

# Introduction

![Status: Planned](https://img.shields.io/badge/status-Planned-blue)

This plan establishes the **Foundation Bridge** between the standalone `pulse` Android application and the `PulseKMP` multiplatform project. The goal is to synchronize the build system to the latest "bleeding edge" versions (Kotlin 2.5.0) and migrate core domain models into the shared module to achieve a successful Android build.

## 1. Requirements & Constraints

- **REQ-001**: `PulseKMP` must match the "bleeding edge" versions of `pulse` (Kotlin 2.5.0, AGP 9.2.0).
- **REQ-002**: Core models (`Song`, `Album`, `Artist`, `Playlist`) must reside in `shared/commonMain`.
- **CON-001**: Shared models must NOT depend on Android-specific classes (e.g., `Parcelize`, `Uri`).
- **PAT-001**: Use `expect/actual` for `PlatformCapabilities` to handle platform-specific UI flags.

## 2. Implementation Steps

### Implementation Phase 1: Build System Synchronization

- GOAL-001: Align PulseKMP versions with the standalone pulse project.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-001 | Sync `PulseKMP/gradle/libs.versions.toml` with `pulse/gradle/libs.versions.toml` (Kotlin 2.5.0, AGP 9.2.0, KSP 2.3.4, Room 2.8.4, Ktor 3.4.2). | | |
| TASK-002 | Update `PulseKMP/gradle.properties` to ensure JVM target is set to 25 and experimental features like context parameters are enabled. | | |
| TASK-003 | Run `./gradlew help` in `PulseKMP` to verify build script configuration success. | | |

### Implementation Phase 2: Domain Model Migration

- GOAL-002: Migrate and sanitize core models into the shared module.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-004 | Create package `app.pulse.shared.domain.models` in `PulseKMP/shared/src/commonMain/kotlin`. | | |
| TASK-005 | Copy `Song.kt`, `Album.kt`, `Artist.kt`, and `Playlist.kt` from `pulse/app/src/main/kotlin/app/pulse/android/models/` to the new shared package. | | |
| TASK-006 | Remove `@Parcelize`, `Parcelable` implementation, and any `android.net.Uri` references from the migrated models. Replace `Uri` with `String`. | | |
| TASK-007 | Migrate playback-related enums (e.g., `Quality`, `RepeatMode`) from `pulse/core/data` to `shared/commonMain`. | | |

### Implementation Phase 3: Platform Capabilities

- GOAL-003: Establish the UI capability abstraction.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-008 | Define `PlatformCapabilities` interface in `shared/commonMain/.../platform/PlatformCapabilities.kt`. | | |
| TASK-009 | Implement `actual` for Android in `shared/androidMain/.../platform/PlatformCapabilities.android.kt` checking API levels for Dynamic Color. | | |
| TASK-010 | Provide `LocalPlatformCapabilities` via `CompositionLocal` in `PulseKMP/composeApp/src/commonMain/kotlin/com/elza/pulsekmp/App.kt`. | | |

### Implementation Phase 4: Verification

- GOAL-004: Validate Android compilation.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-011 | Update `App.kt` in `composeApp` to import and reference a migrated model (e.g., `Song`) to verify visibility. | | |
| TASK-012 | Execute `./gradlew :composeApp:assembleDebug` and verify successful Android APK generation. | | |

## 3. Alternatives

- **ALT-001**: Use a shared "Bridge" module instead of `shared`. Rejected: `shared` is the standard KMP pattern.
- **ALT-002**: Maintain `Parcelize` with `kotlin-parcelize` plugin in common. Rejected: Adds unnecessary complexity to iOS/Desktop targets.

## 4. Dependencies

- **DEP-001**: Room KMP (for future persistence phase).
- **DEP-002**: Kotlin Serialization (for model serialization).

## 5. Files

- **FILE-001**: `PulseKMP/gradle/libs.versions.toml`
- **FILE-002**: `PulseKMP/shared/src/commonMain/kotlin/app/pulse/shared/domain/models/*`
- **FILE-003**: `PulseKMP/shared/src/commonMain/kotlin/app/pulse/shared/platform/PlatformCapabilities.kt`

## 6. Testing

- **TEST-001**: Build verification of the `shared` module for Android and JVM.
- **TEST-002**: Basic smoke test: ensure `composeApp` launches on an Android emulator.

## 7. Risks & Assumptions

- **RISK-001**: Kotlin 2.5.0 is highly experimental; some KMP libraries may not yet support it.
- **ASSUMPTION-001**: The user wants to strictly follow the standalone project's versioning even if it's unstable.

## 8. Related Specifications / Further Reading

- `docs/superpowers/specs/2026-05-08-pulsekmp-migration-design.md`
- `plan/migration-pulsekmp-foundation-v1.md` (Original plan reference)
