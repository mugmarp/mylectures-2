# Architecture Guide — Lectures

**Application Name**: **Lectures**  
**Author**: **Mark Paul M**  
**GitHub**: [@mugmarp](https://github.com/mugmarp) • **Email**: [markpaulmu@gmail.com](mailto:markpaulmu@gmail.com)

---

## 1. Architectural Overview

**Lectures** adheres to the official **Android Recommended App Architecture** and **Unidirectional Data Flow (UDF)** principles.

```
       User Action (Clicks, Drags, Input)
         │
         ▼
┌─────────────────┐           StateFlow (UiState)          ┌───────────────────┐
│ Jetpack Compose ├────────────────────────────────────────┤     ViewModel     │
│   (UI Layer)    │◄───────────────────────────────────────┤ (State Container) │
└─────────────────┘                                        └─────────┬─────────┘
                                                                     │ Calls Methods
                                                                     ▼
                                                           ┌───────────────────┐
                                                           │    Repository     │
                                                           │(Single Source of  │
                                                           │      Truth)       │
                                                           └─────────┬─────────┘
                                                                     │
                                            ┌────────────────────────┴────────────────────────┐
                                            ▼                                                 ▼
                                 ┌────────────────────┐                            ┌────────────────────┐
                                 │   Room Database    │                            │    AlarmManager    │
                                 │  (SQLite Storage)  │                            │ (System Alarms)    │
                                 │+ Specialization SQL│                            └────────────────────┘
                                 └────────────────────┘
```

### Key Principles:
1. **Unidirectional Data Flow (UDF)**:
   * UI elements emit user events up to the ViewModel.
   * ViewModels process business logic, interact with the Repository, and expose immutable `StateFlow<UiState>`.
   * UI Composables observe state via `.collectAsState` and update declaratively.
2. **Offline-First Storage**:
   * The app is fully operational without an internet connection.
   * Schedule data, custom activities, lecture notes, and tasks are stored in a local SQLite database via AndroidX Room.
3. **Reactive Concurrency**:
   * Asynchronous work is handled via Kotlin Coroutines.
   * Room DAOs return reactive `Flow<List<T>>` objects so any database modification automatically triggers UI updates without manual re-querying.

---

## 2. Core Architectural Layers

### A. UI Layer (`com.lectures.ui` & `com.lectures.features.*.ui`)
* **Framework**: 100% Jetpack Compose using Material Design 3 (M3) with centralized tokens in `Theme.kt` (`LocalAppTheme`, `LocalExtendedColors`, 5 accent palettes, Light/Dark/System modes).
* **Adaptive Root Shell (`MainScaffold.kt` & `AdaptiveNavigationShell.kt`)**:
  * Switches cleanly across `LecturesWindow` breakpoints (`Chrome.kt`):
    * **Phones (`< 600dp`)**: Floating bottom navigation pill dock switching between 4 primary tabs (`Timetable`, `Calendar`, `Notes`, `Tasks`), with `Settings`, `Vacant Rooms`, and `Account` pushed as animated secondary routes (`MainScreenDestination`).
    * **Tablets & Foldables (`600–1199dp`)**: Persistent leading `NavigationRail` exposing all 4 tabs plus `Vacant Rooms`, `Account`, and `Settings` as first-class rail destinations.
    * **Desktop & Chromebooks (`>= 1200dp`)**: Collapsible `228dp` `ExpandedSidebar` showing brand header, academic week, clickable programme card, workspace/utility destinations, and global keyboard shortcuts (`Ctrl+1..6`, `Ctrl+,`).
  * **Pane-Accurate Window Propagation**: Inside rail/sidebar mode, `MainScaffold` wraps `DestinationContent` in `BoxWithConstraints` and provides `LocalLecturesWindow` via `rememberContainerWindow(...)` so child screens inspect actual remaining pane width (including `windowInfo.isWideWorkspace` >= `960dp`) rather than raw display width.
* **Component Architecture**: Reusable UI components (`DedicatedPickers.kt`, `DesktopErgonomics.kt`, `TimelineClassCard`, `LectureCard`, `ActivityCard`) separate compact mobile cards from desktop workspaces (`TimetableDesktopWorkspace`, `CalendarMonthSection`, `VacantRoomsScreen` 3-pane layout).
* **Single Appearance & Theme Flow**: Theme mode and accent customization live exclusively inside `Settings → Appearance & Theme` (`SettingsDataAndAboutSection` + `SettingsComponents.AppearanceSettingsView`), eliminating redundant toggles from `AcademicProfileSheet`.

### B. ViewModel Layer (`com.lectures.features.*`)
* Dedicated lifecycle-aware `ViewModel`s (`TimetableViewModel`, `CalendarViewModel`, `NotesViewModel`, `TasksViewModel`, `SettingsViewModel`) expose immutable `StateFlow` UI state with zero `Activity`/`View` references.
* `TimetableViewModel` drives schedule queries, live countdown ticks, and temporary Preview Mode (`peekingProgramme`).
* `SettingsViewModel` manages notification lead times, daily digest scheduling, theme/accent persistence, manual timetable/cloud sync, JSON backup export, and update dialog state.

### C. Data & Repository Layer (`com.lectures.features.timetable.data`)
* **`TimetableRepository`**:
  * Single source of truth coordinating Room DAOs, DataStore/SharedPreferences, `AlarmSyncCoordinator`, `AttachmentStore`, and `SyncRepository`.
  * Employs `getLocalSchedule(programme)` which resolves programme aliases, queries Room candidate entries via `getScheduleCandidates`, filters by `entryMatchesGroup`, and runs deterministic in-memory deduplication.
* **Strict Non-Destructive Migrations (`AppDatabase` v12)**:
  * Guarantees preservation of student notes, coursework deadlines, and custom activities across upgrades.
  * Uses `fallbackToDestructiveMigrationFrom(1)` with explicit migrations `MIGRATION_2_3` through `MIGRATION_11_12`, adding `AttachmentStore` relative paths (`attachmentPath`), soft-delete tombstones (`isDeleted`), UUID deduplication (`clientUuid`), and multi-tenant isolation (`ownerId`).
* **`DataLoader`**:
  * Pre-populates the local database on initial launch from bundled academic assets (`mustime_prepopulated.db` / `timetable_export.json`) with `ETagStore` change tracking.

### D. Timetable Matching & Specialization Architecture (`com.lectures.features.timetable.domain`)
* **`TimetableMatcher`**:
  * Implements `entryMatchesGroup(entry, group)` matching both primary `program_group` and attendee `shared_with`.
  * **Un-Prefixed Group Aliases (`DIRECT_DB_GROUPS`)**:
    Maps raw database cohort labels (e.g. `MATHEMATICS`, `BIOLOGY`, `CHEMISTRY`, `PHYSICS`, `ACCOUNTING`, `SUSTAINABLE RURAL DEVELOPMENT`, `Biology-Chemistry`) directly to structured `ParsedGroup(code, subject, year)`.
  * **Canonical Cohort Normalization (`canonicalCohort`)**:
    Collapses sibling aliases (such as raw `BIOLOGY` vs prefixed `BS BIOLOGICAL III`) into a single canonical cohort string to prevent duplicate cohort clutter on shared classes.
  * **Specialization Candidate Retrieval (`TimetableDao.getScheduleCandidates`)**:
    Uses SQL parameter `:specialization` alongside `:code` and `:alias` so SQLite loads candidate entries for un-prefixed cohorts without truncation.
  * **Removal of Synthetic Parent Groups**:
    By eliminating fictitious parent groups (`BS I`, `BS II`, `BS III`, `DLT II`, `BBA III`, `BPCD III`), students enroll in their actual track, preventing artificial schedule conflicts caused by overlapping tracks.
  * **Lecture Card Subtitle Formatting**:
    * Unshared classes show only the course code (e.g. `BIO1101`).
    * Shared classes show only other sharing cohorts (e.g. `DVS2105 · BCS II`).
    * Guild/association entries (`MUCOSA`) omit cohort groups.
    * NextUp hero card suppresses cohort listings.

### E. Preview Mode Architecture
* Driven by `TimetableViewModel.peekingProgramme` (`StateFlow<String?>`).
* When active, schedule queries load the previewed cohort while background alarms, notifications, and persistent preferences remain strictly bound to the enrolled programme.
* Banner surfaces current preview status with an `Exit Preview` callback.

---

## 3. Background Processing, Alarms & Workers

* **`AlarmScheduler` & `TaskAlarmScheduler`** (`com.lectures.core.alarm`):
  * Use deterministic collision-resistant request codes rather than raw 32-bit `hashCode()`, `PendingIntent.FLAG_IMMUTABLE`, Doze-safe `setExactAndAllowWhileIdle` (with graceful inexact fallback when exact alarm permission is denied), and `BootReceiver` / `AlarmSyncCoordinator` recovery.
* **`AlarmReceiver`, `ClassAlarmReceiver`, `TaskReminderReceiver`, `TaskActionReceiver`**:
  * Guard `goAsync()` background work with bounded `withTimeout(...)` limits inside Android's broadcast execution window.
* **`TimetableSyncWorker`** (`androidx.work.WorkManager`):
  * Periodically syncs remote timetable updates (24h cadence), flushes queued anonymous feedback, and throttles release notifications to every 36 hours.

---

## 4. Obsidian-Grade Markdown Notes & Attachments

* **`MarkdownUtils` (`features/notes/MarkdownUtils.kt`)**:
  * **AST Block Parser (`parseBlocks`)**: Parses multi-line Obsidian callouts (`CalloutType`), GitHub-flavored tables (`MarkdownBlock.Table`), math blocks (`$$...$$`), H1–H6 headers, bulleted/numbered lists, checklists, fenced code blocks, and dividers into strongly typed sealed classes.
  * **Inline Parser (`renderInlineMarkdown`)**: Tokenizes inline styles into Compose `AnnotatedString` spans for bold, italic, strikethrough, highlights (`==`), WikiLinks (`[[`), and LaTeX math (`$`).
  * **Live Preview Syntax Transformation (`MarkdownLiveVisualTransformation`)**: Implements `VisualTransformation` with `OffsetMapping.Identity` for Compose `BasicTextField` with 1:1 character mapping.
* **Desktop Master-Detail & 3-Zone Editor (`NotesScreen`, `NoteEditorScreen`)**:
  * Compact/medium screens switch cleanly between the notes library and full-screen editor (`BackHandler`-managed).
  * Wide workspaces (`windowInfo.isWideWorkspace`) present a split library/preview pane on `NotesScreen`, plus a 3-mode editor (`Split` / `Write` / `Preview`), live word/read-time counter, keyboard shortcuts (`Ctrl+B`, `Ctrl+I`, `Ctrl+S`), and docked `Inspector` pane for course tags, class alarms, and attachments.
* **`AttachmentStore` (`core/storage/AttachmentStore.kt`)**:
  * Copies picked documents via zero-permission Android SAF / Photo Picker (`ActivityResultContracts.OpenDocument`) into internal `files/attachments/` storage, enforces size/filename sanitization, and deletes orphaned files cleanly when notes are deleted.

---

## 5. Google Authentication, Cloud Sync & Anonymous Feedback

* **`AuthRepository` (`core/auth/AuthRepository.kt`)**:
  * Integrates Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) and Firebase Auth without relying on unprovisioned anonymous authentication.
* **`SyncRepository` & `SyncConflictResolver` (`core/sync/`)**:
  * Bidirectional synchronization between local Room SQLite (`ownerId`-scoped) and Firestore (`/users/{uid}/notes|events|assignments|settings`), attaching snapshot error listeners, promoting guest-mode records on first sign-in, forking simultaneous note edits into conflict drafts (`"[Sync Conflict]"`), and purging signed-out user data via `purgeUserDataOnSignOut(uid)`.
* **`FeedbackManager` (`features/feedback/FeedbackManager.kt`)**:
  * Submits anonymous feedback to Firestore `/feedback/{feedbackId}` bounded by `withTimeoutOrNull(1200L)` so offline submissions queue immediately into `pending_feedback_retry_queue` for retry on launch.

---

## 6. Play-Compliant Release Checker

* **`InAppUpdateManager` (`core/update/InAppUpdateManager.kt`)**:
  * Checks `version.json` and the GitHub Releases API (with 15-minute in-memory caching), validates strict HTTPS URLs (`isSecureHttpsUrl`), prevents downgrades via `isNewerRelease`, purges legacy APK update caches (`purgeStaleUpdateFiles`), and directs students to the verified HTTPS release page via `Intent.ACTION_VIEW` without sideloading APKs or declaring `REQUEST_INSTALL_PACKAGES`.
* **`AppUpdateDialog` (`core/update/AppUpdateDialog.kt`)**:
  * Curates release highlights cleanly and preserves dialog visibility across rotation.

---

## 7. Automated Verification & Visual Regression Architecture

The test suite under `app/src/test/java/com/lectures/` covers six integrated verification layers:
* **Phase 1–2**: Firestore rules parity, multi-user sync & conflict resolution (`Phase1SecurityAndSyncAuditTest`, `SyncConflictResolverTest`), Room schema migrations v2–v12, and DataStore/prefs consistency (`Phase2PersistenceAndMigrationTest`, `DatabaseMigrationTest`).
* **Phase 3–4**: Deterministic alarm/boot recovery, permission non-nagging, Calendar/Room/Markdown/Attachment/`InAppUpdateManager` boundary suites (`Phase3AlarmAndNotificationTest`, `Phase4DomainAndUpdaterTest`).
* **Phase 5 & Roborazzi**: Compose CUJs across phone (`360x800`), foldable (`674x841`), and desktop (`1280x800` + `fontScale = 1.5f`) qualifiers (`Phase5UiAndAdaptiveTabletTest`, `RoborazziVisualSnapshotTest`).
* **Phase 6**: R8/ProGuard keep rules, manifest/export/Play Policy compliance, and release APK assembly (`Phase6BuildHardeningAndPlayPolicyTest`).
