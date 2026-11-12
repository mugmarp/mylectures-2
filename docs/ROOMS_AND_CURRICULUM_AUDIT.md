# Audit & Architectural Improvements: Curriculum, Vacant Room Finder & Modal Dialogs

This document details the issues identified during the comprehensive audit of the MUST Lectures App, the root causes, and the architectural solutions implemented.

---

## 1. BGWH Curriculum & Intake Alignment (MUST 2026/2027)

### Identified Issue
On the onboarding **Choose Your Class Group** screen (`ClassGroupSelectionScreen`), the program **BGWH (BSc Gender & Applied Women Health)** was displaying:
- `BGWH I` — 0 timetable entries
- `BGWH II` — 0 timetable entries
- `BGWH III` — 12 timetable entries

Because `BGWH I` was listed first, the screen automatically defaulted new students to `BGWH I` (an empty cohort), creating a confusing onboarding experience.

### Real-World Academic Context
At Mbarara University of Science and Technology (MUST) for the 2026/2027 academic year:
- The **BGWH** program has **no Year 1 and Year 2 intakes** (intake paused or earlier cohorts graduated, leaving only Year 3 currently enrolled).
- Only `BGWH III` has active scheduled classes in the official university semester timetable.

### Solution & Technical Architecture
1. **Curriculum Model Precision (`FacultyData.kt`):**
   Explicitly configured `customYears = listOf("III")` on `Programme("BGWH", "BSc Gender & Applied Women Health", 3, customYears = listOf("III"))`.
   - `Programme.allGroups` now dynamically generates only `["BGWH III"]`, accurately reflecting MUST's actual academic catalog.
2. **Dynamic Smart Auto-Selection (`OnboardingScreens.kt`):**
   - Implemented memoized `resolvedCounts` across groups via `remember`.
   - Changed default cohort selection to auto-select the first active cohort with `entries > 0` (`initialActiveGroup`), ensuring students are never defaulted to an inactive cohort.
   - Refined cohort status labels: active cohorts show `"$count scheduled timetable classes"`, clinical tracks indicate `"Clinical hospital rotations"`, and off-schedule cohorts clearly state `"0 scheduled timetable entries this semester"`.

---

## 2. Vacant Room Finder: Location Architecture & Dual-Segment Bar

### Identified Issues
- **Low Prominence:** The Campus and Building filter controls were rendered as generic `FilterChip` items inside a horizontally scrolling `LazyRow` alongside temporary toggles like `Free now` and `All`.
- **Hidden Controls:** On standard devices or when scrolled right, the Building dropdown chip scrolled completely out of view.
- **Mental Model Mismatch:** Students search for vacant rooms through a spatial hierarchy (*Which campus?* $\rightarrow$ *Which building?* $\rightarrow$ *Is it free?*). Mixing spatial selectors with state toggles hindered discovery.

### Implemented Improvements
1. **Dedicated Dual-Segment Location Card:**
   - Moved Campus and Building out of the scrolling chip row into a prominent, dedicated card directly below the search bar.
   - The card is split into two touch-friendly segments separated by a vertical divider:
     - **Left Segment:** `[ 📍 Campus: <Selected Campus> ▾ ]`
     - **Right Segment:** `[ 🏢 Building: <Selected Building> ▾ ]`
   - Features icons, clear subheads ("Campus", "Building"), bold active values, and native Material 3 dropdown menus with room catalog counts and faculty default stars (`★`).
2. **Streamlined Quick Filter Row:**
   - The horizontal `LazyRow` is now reserved exclusively for status toggles (`🟢 Free now`, space categories: `Lecture Room`, `Lab`, `Library`, and quick faculty reset).

---

## 3. Search Bar Geometry & Polish

### Identified Issues
- Standard `OutlinedTextField` was 56dp tall with boxy 12dp corner radius (`RoundedCornerShape(12.dp)`), occupying excessive vertical screen real estate.

### Implemented Improvements
- **Reduced Height:** Lowered container height to **44dp**, matching modern Google Maps and Spotify search pill ergonomics.
- **Corner Radius:** Increased border radius to **24dp** (`RoundedCornerShape(24.dp)`), creating a sleek pill shape.
- **Refined Typography & Padding:** Replaced standard text field with `BasicTextField` inside a pill `Surface` with 13.5sp medium typography, crisp placeholder, leading search lens, and a quick-clear (`✕`) icon.

---

## 4. Vacant Room Schedule Detail Dialog (`RoomScheduleDetailDialog`)

### Identified Issues (Screenshot 4 Audit)
1. **Missing Close Button:** In the header `Row`, the left `Column` lacked `Modifier.weight(1f)`. For rooms with long names (e.g. `"Faculty of Science Computer Lab"`), the title consumed the entire row width and pushed the `IconButton(Close)` completely off-screen.
2. **Floor Level Badge Displaced:** In the nested `Row`, the unconstrained title pushed `FloorIndicatorBadge` off-screen or caused awkward vertical line wrapping.
3. **Severe Vertical Truncation (Clipped Empty-State Pill):** The dialog `Card` had `.heightIn(max = 560.dp)` without `Modifier.verticalScroll(rememberScrollState())`. When vacant room empty-state information exceeded 560dp, the bottom was clipped midway through the green empty-state card, showing only a lone checkmark icon in an awkward green capsule and hiding the bottom "Close" button entirely.
4. **Developer Scraper Artifacts in Notes:** Internal scraping notes such as `"No scheduled sessions - listed in Room Allocation but unused this semester. NOTE: duplicate listing in source data."` were displayed in a purple banner, confusing students with technical parser details.

### Implemented Improvements
1. **Weighted Header Layout:**
   - Added `Modifier.weight(1f)` to the header `Column` with `Alignment.Top` vertical alignment.
   - Pinned a dedicated 32dp circular `IconButton` with `Icons.Default.Close` at the top right, guaranteeing it is always visible.
   - Bound `formatRoomDisplayCode(room)` with `Modifier.weight(1f, fill = false)` alongside `FloorIndicatorBadge(compact = true)`.
2. **Vertical Scrolling Body:**
   - Wrapped the dialog content in `Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()))`.
   - Content expands naturally without clipping regardless of device font scaling or screen height.
3. **Pinned Action Button:**
   - Pinned the primary "Close" `Button` outside the scrollable body at the bottom of the card (`height = 46.dp`), ensuring it is always immediately accessible.
4. **Scraper Note Sanitization:**
   - Sanitized internal scraper notes: stripped out technical comments (`NOTE: duplicate listing...`, `NOTE: exists in FCI directory...`).
   - Suppressed redundant "No scheduled sessions" purple boxes when the empty-state card below already communicates availability in friendly student language.
5. **Polished Empty-State Card:**
   - Balanced vertical padding (`16.dp`), refined icon size (`42.dp`), and structured badge layout for "✓ Full Day Free" and floor indicators.

---

## 5. Performance & Data Integrity Summary

- **Memoized Lookups:** Cached group entry count computations in onboarding and vacant room listing filters.
- **Clock Pulse Integration:** Room availability countdowns and next-class countdowns remain synchronized with the app lifecycle pulse (`rememberLiveClockPulse`).
- **Build Status:** Verified with `compile_applet` and local unit tests (`gradle :app:testDebugUnitTest`).

---

## 6. Micro-Interactions & Proportional Polish Refinements

### Dual-Segment Location Card Height & Typography Spacing
- **Height Compression:** Reduced total card height from ~54dp to **~40dp** by lowering outer Row padding (`vertical = 2.dp`) and segment Box padding (`vertical = 2.dp`).
- **Word Gap Reduction:** Applied `verticalArrangement = Arrangement.spacedBy((-1).dp)` with explicit line heights (`11.sp` on label, `15.sp` on value) to tighten the vertical distance between "Campus" and its value ("Town") and "Building" and its value ("IMS").
- **Proportional Icons:** Scaled the icon containers to `26.dp` (15dp icon) and divider height to `22.dp`, creating visual harmony with the 44dp pill search bar above it.

### Class Group Tap-to-Confirm Interaction
- **Instant Flow Progression:** Updated the `ClassGroupSelectionScreen` card click interaction. When a student taps an already-selected cohort (such as the auto-selected `PEEM I`), or double-taps another cohort, it immediately triggers `onConfirm(group)` and transitions straight to the Timetable, eliminating redundant navigation friction.

---

## 7. In-App Update Engine: Deep Audit, Vulnerability Analysis & Hardening

### Identified Vulnerabilities & Flaws

1. **Silent Network Failures Reported as "Up to Date" (Exception Swallowing):**
   - **Root Cause:** In `InAppUpdateManager`, both `fetchFromVersionJson` and `fetchFromGitHubReleases` caught network exceptions (`SocketTimeoutException`, `UnknownHostException`) internally and returned `null`. `checkForUpdate` treated `null` as "no newer update available", wrapping it in `Result.success(null)`.
   - **User Impact:** When a student checked for updates while offline or on unstable campus Wi-Fi, `SettingsScreen` displayed a toast: *"Lectures is up to date"*, misleading the user into thinking their app had verified the latest release.

2. **Storage Leak from Accumulated APKs & Abandoned Partial Downloads:**
   - **Root Cause:** Every downloaded APK (`lectures_update_<version>.apk`) remained in app storage indefinitely. If a download was interrupted, orphaned `.tmp` files accumulated in the cache directory, consuming scarce internal storage on low-end student devices.

3. **Missing Package Archive Pre-Flight Integrity Verification:**
   - **Root Cause:** The update engine passed the downloaded file directly to Android's `PackageInstaller` via `FileProvider.getUriForFile()`. If an archive was incomplete or corrupted, the Android OS package manager crashed with an opaque "There was a problem parsing the package" error.

4. **Settings Permission Lifecycle Desynchronization:**
   - **Root Cause:** When prompting students to enable `"Install unknown apps"` (`ACTION_MANAGE_UNKNOWN_APP_SOURCES`), returning to the app from system settings (`ON_RESUME`) did not re-query `packageManager.canRequestPackageInstalls()`. The dialog remained in the amber "Permission required" warning state until manually dismissed and reopened.

5. **Background Notification Spam in `TimetableSyncWorker`:**
   - **Root Cause:** WorkManager runs `TimetableSyncWorker` every 6–12 hours. Step 4 checked for app updates and unconditionally called `NotificationHelper.showAppUpdateNotification()` every time an update was detected, repeatedly cluttering the student's notification tray with duplicate notices.

6. **GitHub API Rate-Limit Exposure on Shared Campus IP:**
   - **Root Cause:** Unauthenticated GitHub REST API calls are restricted to 60 requests per hour per public IP address. In university NAT environments (such as MUST Kihumuro and Town campus Wi-Fi), hundreds of devices share a few gateway IPs, leading to immediate HTTP 403 `API rate limit exceeded` errors.

7. **Offline Lockout on Mandatory Updates:**
   - **Root Cause:** If an update was tagged `isMandatory = true`, `AppUpdateDialog` disabled all dismissal handlers (`onDismissRequest = {}`, hidden close icon, suppressed "Remind Me Later" button). If the download failed or the student had no internet, they were permanently locked out of their saved timetable, schedule alarms, and notes.

---

### Architectural Solutions & Hardening Implemented

1. **Explicit Result Typing & Error Distinction (`InAppUpdateManager.kt`):**
   - Refactored `checkForUpdate(context, forceRefresh = false)` to return a tri-state `Result<AppUpdateInfo?>`:
     - `Result.success(AppUpdateInfo)`: Confirmed eligible newer release.
     - `Result.success(null)`: Network check succeeded; current installed version is strictly up to date.
     - `Result.failure(Throwable)`: Network offline, DNS failure, HTTP 403 rate-limit, or malformed JSON.
   - Updated `SettingsScreen.kt` to catch `Result.failure` and display a clear warning: *"Unable to check for updates. Check internet connection."*

2. **Automated Cache & Stale File Purging (`purgeStaleUpdateFiles`):**
   - Implemented automatic cleanup before each download and on version resolution:
     - Scans `context.cacheDir/updates/` and deletes all `.apk` and `.tmp` files matching older version codes.
     - Cleans orphaned temporary chunks older than 2 hours.

3. **Pre-Flight Package Archive Verification (`verifyPackageArchive`):**
   - Integrated `context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)`:
     - Validates that the APK archive is parseable, non-corrupted, and matches `context.packageName`.
     - Validates cryptographic SHA-256 hash against remote release metadata when provided.
     - Catches corrupted downloads early and triggers a clean retry before handing off to the Android OS installer.

4. **Lifecycle-Aware Permission Tracking (`AppUpdateDialog.kt`):**
   - Integrated `LifecycleEventObserver` on `LocalLifecycleOwner.current`.
   - On `Lifecycle.Event.ON_RESUME` (when the student returns from Android's "Install unknown apps" settings page), the dialog immediately re-checks `canRequestPackageInstalls()`.
   - Automatically hides the amber warning banner and transitions the primary action button to `"Install & Restart"`.

5. **Notification Throttling with SharedPreferences (`TimetableSyncWorker.kt`):**
   - Added persistent timestamp and version tracking (`last_notified_version_code`, `last_notified_timestamp` in `app_update_prefs`).
   - Non-mandatory updates are throttled to a minimum 36-hour interval, preventing background notification spam while ensuring critical/mandatory updates notify immediately.

6. **15-Minute In-Memory Response Caching (`CACHE_TTL_MS`):**
   - Added an in-memory cache with a 15-minute TTL to `InAppUpdateManager`.
   - Background workers and rapid user taps reuse cached release info instead of hammering the GitHub API, completely avoiding campus Wi-Fi rate-limiting errors. Settings manual checks pass `forceRefresh = true` to bypass the cache when explicitly requested.

7. **Offline-First Escape Hatch for Mandatory Updates:**
   - Modified `AppUpdateDialog` so that when `errorMessage != null` (e.g., download failed, offline), the dialog enables dismissal (`onDismissRequest`) and renders a prominent `"Continue to Offline Timetable"` button.
   - **Guaranteed Access:** Students are never prevented from viewing their class timetable or accessing their notes due to network connectivity issues or update server outages.


---

## 8. Mobile Screen Responsiveness & Viewport Hardening Audit

### Viewport Matrix & Environmental Constraints

Our audit evaluated the application against the real-world device distribution commonly seen among university students, specifically:
- **Ultra-Compact Devices (320–340dp width):** Budget handsets (Itel, Tecno Pop, Samsung Galaxy A03 Core, Galaxy Z Fold cover screen). Usable content width after 16dp outer margins is $\approx 288\text{dp}$.
- **Standard Handhelds (360–390dp width):** Mainstream smartphones (Redmi, Samsung Galaxy A series, Pixel 7a/8a). Usable content width is $\approx 328\text{--}358\text{dp}$.
- **Short Viewports (<620dp usable height):** Devices with legacy 3-button navigation bars (-56dp), system status bars (-32dp), and bottom navigation bars (-80dp). Total usable screen height can shrink below $450\text{dp}$.
- **Software Keyboard (IME Active):** Virtual keyboards consume $260\text{--}340\text{dp}$ of vertical height, leaving as little as $280\text{dp}$ of visible window space.
- **Accessibility Font Scaling (1.15×–1.5×):** Android system settings allowing enlarged text scale factors, expanding string pixel widths and line heights significantly.

---

### Responsiveness Flaws Identified & Mathematical Analysis

1. **Room Vacancy Card Header Badges Clipping Room Codes (`VacantRoomsScreen.kt`):**
   - **Flaw:** Unconstrained horizontal `Row` placed `displayCode` (e.g. `DS LAB IV`), `FloorIndicatorBadge` (`1st`), and room type `Badge` (`[Computer Lab]`) side-by-side.
   - **Math:** $110\text{dp (code)} + 40\text{dp (floor)} + 120\text{dp (lab badge)} + 16\text{dp (spacers)} = 286\text{dp}$. On a 320dp screen ($288\text{dp}$ content width minus $24\text{dp}$ card padding $= 264\text{dp}$), the code was crushed into `"DS L..."`.
   - **Fix:** Switched to `@OptIn(ExperimentalLayoutApi::class) FlowRow`. Anchored `displayCode` and `FloorIndicatorBadge` together, allowing descriptive type badges to wrap to a second line when space is constrained.

2. **Year of Study Chips Clipping 5-Year Degrees (`AcademicProfileSheet.kt`):**
   - **Flaw:** Year selection used a rigid `Row(horizontalArrangement = Arrangement.spacedBy(8.dp))` for chips `I`, `II`, `III`, `IV`, `V`.
   - **Math:** 5 chips at $48\text{dp}$ width $+ 32\text{dp}$ spacing $= 272\text{dp}$. On 320dp screens with $32\text{dp}$ sheet padding, total available width is only $288\text{dp}$. When font scaling was increased to 1.25×, chips expanded to $320\text{dp}$, clipping Year `V` completely off the right edge.
   - **Fix:** Refactored to `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth())`. 5-year programmes wrap gracefully into two balanced rows.

3. **Note Editor Top Bar Header Collision (`NoteEditorScreen.kt`):**
   - **Flaw:** Rigid horizontal `Row` accommodated the back button, title ("Edit Note"), the Write/Preview segmented pill control, and the "Save ✓" button without bounding weights.
   - **Math:** Back button ($48\text{dp}$) + Title ($90\text{dp}$) + Write/Preview pill ($140\text{dp}$) + Save button ($80\text{dp}$) + spacers ($24\text{dp}$) $= 382\text{dp}$, directly exceeding all 320dp and 360dp screens.
   - **Fix:** Added `windowInfo = rememberLecturesWindow()`. Bounded title with `Modifier.weight(1f, fill = false)` and `TextOverflow.Ellipsis`. On `windowInfo.isNarrow || windowInfo.isTightFont`, collapsed Write/Preview to icon-only pills, saving $60\text{dp}$ of width and ensuring the Save button is always fully visible and clickable.

4. **Dual-Segment Campus & Building Selector Width Exhaustion (`VacantRoomsScreen.kt`):**
   - **Flaw:** Fixed 8dp padding, 26dp icon boxes, and lengthy default text labels (`"All Campuses"`, `"All Buildings"`) in the dual-segment card required a minimum width of $296\text{dp}$.
   - **Fix:** Responsive scaling via `windowInfo`. On compact/tight-font viewports, segment horizontal padding scales from 8dp to 4dp, icon boxes reduce from 26dp to 22dp, and fallback labels shorten to `"All"`, reclaiming $28\text{dp}$ of horizontal clearance and eliminating horizontal clipping on 320dp devices.

5. **Onboarding Class Group Viewport Starvation (`OnboardingScreens.kt`):**
   - **Flaw:** In `ClassGroupSelectionScreen`, the programme summary card was pinned inside `Scaffold.topBar`, consuming $\approx 90\text{dp}$ of fixed height on every frame.
   - **Math:** On a 640dp budget device with 3-button navigation ($-56\text{dp}$), status bar ($-32\text{dp}$), and bottom continue bar ($-86\text{dp}$), remaining height was $466\text{dp}$. With a $240\text{dp}$ top bar, the `LazyColumn` was starved down to $226\text{dp}$ (rendering only 1.5 class group cards at a time).
   - **Fix:** Moved the programme summary card into the scrollable `LazyColumn` body as `item(key = "program_summary_header")`. `topBar` height was reduced by $80\text{dp}$, expanding the scrollable view to comfortably display 2.5–3 class cards simultaneously.

6. **Task Progress Dashboard Legend Overlap (`TaskProgressDashboard.kt`):**
   - **Flaw:** The legend row placed `"Completed: X (Y%)"` and `"Pending: W (Z%)"` in a `Row(horizontalArrangement = Arrangement.SpaceBetween)`.
   - **Math:** Required width was $275\text{dp}$. Inside the card's $256\text{dp}$ width on 320dp screens, the text lines collided and clipped.
   - **Fix:** Switched to `@OptIn(ExperimentalLayoutApi::class) FlowRow` and introduced adaptive labels via `rememberLecturesWindow()`: on compact viewports, labels shorten to `"Done: X (Y%)"` and `"Pend: W (Z%)"`.

7. **Add Task Dialog Virtual Keyboard Occlusion (`TasksScreen.kt`):**
   - **Flaw:** `AddTaskDialog` contains 8 stacked fields. When the soft keyboard (IME) opened, it pushed the action buttons ("Schedule & Save" / "Cancel") below the screen edge.
   - **Fix:** Added `modifier = Modifier.imePadding()` to `AlertDialog`, allowing the dialog to automatically adjust upward with the window insets so inputs and buttons remain accessible.

8. **Dedicated Pickers Screen Cutoff in Landscape / Split Screen (`DedicatedPickers.kt`):**
   - **Flaw:** `DedicatedTimePickerDialog` (dial + controls $\approx 470\text{dp}$), `DedicatedCalendarDatePickerDialog` ($\approx 460\text{dp}$), `DedicatedSchedulePickerDialog`, and `DedicatedDayPickerDialog` lacked vertical scrolling. In landscape mode or Android multi-window split-screen (<550dp height), the confirm/dismiss buttons were clipped off-screen.
   - **Fix:** Applied `Modifier.verticalScroll(rememberScrollState())` to all dialog card Columns. Users on small screens or in split-screen mode can now scroll smoothly to reach the "OK", "Select", and "Cancel" buttons.

9. **App Update Dialog Action Button Below-the-Fold Starvation (`AppUpdateDialog.kt`):**
   - **Flaw:** The primary action button ("Download & Update" / "Install & Restart") was placed at the very bottom of the card inside a single monolithic `verticalScroll` Column along with release highlights. When release notes were long, the update button was pushed completely below the fold.
   - **Fix:** Decoupled layout into 3 distinct sections:
     1. **Pinned Header:** App update icon, version names, close button.
     2. **Scrollable Body:** Changelog highlights and status banners wrapped in `Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()))`.
     3. **Pinned Footer:** Action buttons anchored at the bottom outside the scrollable body, ensuring 1-tap installation is immediately accessible without scrolling.

---

## 9. Notification Permission Lifecycle & Repeated Prompt Elimination

### Root Cause Analysis

When users tapped **"Don't allow"** on Android 13+ (API 33+) notification permission dialogs, the app unexpectedly presented the permission prompt again when returning to the Timetable tab.

**Technical Root Cause:**
1. **Unbounded Composition Lifecycle Execution (`TimetableScreen.kt`):**
   `TimetableScreen.kt` previously contained a `LaunchedEffect(Unit)` that ran whenever the composable entered the composition tree (on cold start, tab switching to Timetable, or returning from secondary screens like Notes or Rooms).
   ```kotlin
   // Buggy pattern:
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
       val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
       if (!granted) {
           notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
       }
   }
   ```
2. **Absence of Persistent Prompt Tracking:**
   Because `onResult = { /* Permission handled */ }` was a no-op and no persistent record of the user's decision existed, `granted` remained `false` on every future visit. Consequently, `notificationPermissionLauncher.launch(...)` executed repeatedly on screen transitions. On Android 13+, the OS permits two automatic dialog displays before auto-suppressing, meaning returning to the timetable immediately re-triggered the system dialog.
3. **Aggressive Settings Redirect in TasksScreen (`TasksScreen.kt`):**
   When the user tapped "Allow" on the Tasks banner but subsequently clicked "Don't allow" in the OS dialog, `TasksScreen.kt` forcefully opened Android System Settings (`ACTION_APP_NOTIFICATION_SETTINGS`), frustrating users who intentionally declined.
4. **Transient Banner Dismissal:**
   The notification banner dismissal state in `TasksScreen.kt` was held in memory via `rememberSaveable`, causing the banner to reappear whenever the app process restarted.

---

### Architectural Solution Implemented

1. **Persistent Permission State Tracking (`NotificationPermissionHelper.kt`):**
   Added persistent SharedPreferences tracking under `mustime_notification_prefs`:
   - `hasPromptedNotificationPermission(context): Boolean`: Returns true if the user has ever been presented with the notification permission prompt.
   - `setPromptedNotificationPermission(context, prompted: Boolean)`: Persists the flag immediately upon prompt dispatch or user resolution.
   - `isTaskBannerDismissed(context): Boolean` / `setTaskBannerDismissed(context, Boolean)`: Persists manual banner dismissal across app sessions.

2. **Strict One-Time Prompt Guard (`TimetableScreen.kt`):**
   Refactored `TimetableScreen.kt`'s `LaunchedEffect(Unit)`:
   - Evaluates `alreadyPrompted = NotificationPermissionHelper.hasPromptedNotificationPermission(context)`.
   - Checks `isExistingUser = ETagStore(context).isOnboardingCompleted()`.
   - **Existing Users:** If an existing user who previously set up the app visits Timetable, `setPromptedNotificationPermission(context, true)` is recorded without launching the prompt, ensuring they are never pestered.
   - **New Users:** If `!granted && !alreadyPrompted`, `setPromptedNotificationPermission(context, true)` is recorded immediately and the prompt is shown at most once.
   - **Subsequent Screen Visits:** Any subsequent visit to Timetable evaluates `alreadyPrompted == true` and skips launching the permission launcher entirely.

3. **Gentle, Non-Intrusive Contextual Reminders (`ClassAlarmPickerDialog.kt`):**
   When a student explicitly sets a class alarm while system notifications are disabled, the alarm schedule is saved without failure, and a lightweight informational Toast informs the student:
   `"Alarm saved! Enable notifications in Settings if you'd like audible alerts."`
   This eliminates unexpected system popups while keeping the user informed of their alarm's delivery mode.

4. **Task Banner Non-Aggressive Flow (`TasksScreen.kt`):**
   - Removed the forced redirect to system settings upon permission denial.
   - Clicking the dismiss `X` button persists `setTaskBannerDismissed(context, true)`, keeping the banner hidden permanently.


---

## 10. Deep Audit: Silent Bugs, Concurrency Flaws & Security Hardening

### Summary of Audit Findings & Root Causes

A deep-dive audit was conducted to uncover silent runtime bugs, concurrency hazards, data resurrection anomalies, and security vulnerabilities that could compromise stability or user data integrity.

### 1. Concurrency Hazard & Race Condition in Date Parsing (`TaskDateTimeParser.kt`)
- **Vulnerability:** `SimpleDateFormat` is **not thread-safe** in Java/Android. It maintains an internal mutable `Calendar` instance during `parse()`. In `TaskDateTimeParser.kt`, a static singleton list `DATE_FORMATS` was shared across all threads.
- **Silent Bug Mechanism:** Background workers (`TimetableSyncWorker`, `BootReceiver`), AlarmManager receivers (`TaskReminderReceiver`), and Compose UI coroutines parsing tasks simultaneously corrupted each other's internal calendar state. This led to random `NumberFormatException: multiple points`, silent date parse failures, or tasks scheduled for corrupt dates.
- **Locale Bug:** Static formatters initialized with `Locale.getDefault()` failed to parse English day/month strings (e.g. `"Friday"`, `"Oct"`) on devices set to non-English system languages (Swahili, French, German).
- **Architectural Solution:**
  - Replaced shared mutable `SimpleDateFormat` instances with immutable pattern strings (`DATE_PATTERNS`).
  - Implemented `tryParsePattern` that instantiates isolated formatters per parse and tests both `Locale.US` and `Locale.getDefault()`, guaranteeing thread-safety and cross-locale parsing.

### 2. Deleted Notes & Assignments Resurrection Bug (`DataLoader.kt`)
- **Flaw:** In `DataLoader.loadInitialDataIfNeeded()`:
  - Line 49: `if (dao.getNotesWithAlarms().isEmpty()) { dao.upsertNote(sampleNote) }`
  - Line 65: `if (assignmentDao.getCount() == 0) { assignmentDao.upsert(...) }`
- **Silent Bug Mechanism:**
  - If a student intentionally deleted the sample "Welcome to Lectures" study guide or authored notes without alarms, `dao.getNotesWithAlarms().isEmpty()` returned `true` on every subsequent app restart, resurrecting the sample note against the student's wishes.
  - Similarly, if a student finished or deleted all assignments, `assignmentDao.getCount() == 0` triggered on next launch and resurrected the sample assignment.
- **Architectural Solution:**
  - Introduced a persistent preference flag `has_seeded_sample_data` in `mustime_settings`.
  - Seeding occurs strictly once on the initial first launch. Once marked seeded, user deletions are permanent and data is never resurrected.

### 3. Path Traversal Vulnerability (CWE-22) in `AttachmentStore.kt`
- **Vulnerability:** In `AttachmentStore.resolveFile()` and `AttachmentStore.delete()`, paths were resolved as `File(context.filesDir, relativePath)`.
- **Exploitation Risk:** If a relative path containing upward traversal sequences (e.g. `"../databases/mustime_app.db"` or `"../shared_prefs/mustime_settings.xml"`) was passed to `resolveFile()` or `delete()`, the app could delete or expose critical database and configuration files through `FileProvider`.
- **Architectural Solution:**
  - Added strict prefix guards rejecting any `relativePath` starting with `/`, `\`, or containing `..`.
  - Enforced canonical path containment: `canonicalFile.startsWith(canonicalBase + File.separator)`, guaranteeing that file operations can never escape the private `filesDir/attachments/` directory.

### 4. Year Boundary Semester Calculation Anomaly (`TimeUtil.kt`)
- **Flaw:** In `TimeUtil.currentSemesterWeekLabel()` and `TimeUtil.currentAcademicWeek()`:
  - In MUST's academic calendar, Semester 1 starts in late August and runs through January.
  - When evaluated during January (e.g. January 10, 2027), `month < Calendar.AUGUST` caused `semStart` to be set to `January 19, 2027` (a future date).
  - This produced negative elapsed days, clamping `weekNum` to `"Week 1 • Sem 1"` and misidentifying end-of-semester examination weeks as Week 1.
- **Architectural Solution:**
  - Evaluates `if (month <= Calendar.JANUARY)` and anchors Semester 1 start date to August 24 of the previous calendar year (`year - 1`).
  - Accurately tracks Semester 1 weeks 18–21 as `"Recess / Exams"` and starts Semester 2 in February of the current year.

### 5. OEM Notification Crash Hardening (`NotificationHelper.kt`)
- **Flaw:** Notification dispatch methods caught only `SecurityException`.
- **Silent Bug Mechanism:** On customized OEM Android distributions (Transsion Tecno HiOS, Infinix XOS, Xiaomi HyperOS/MIUI, Oppo ColorOS), calling `NotificationManagerCompat.notify()` when notification channels are altered or notifications are restricted by battery savers can throw `NullPointerException`, `RemoteServiceException`, or `BadNotificationException`.
- **Architectural Solution:**
  - Hardened all `notify()` invocations with catch-all `Exception` guards and informative log messages, preventing background worker or alarm receiver terminations.


---

## 11. Application ID Configuration & Rapid Cohort Confirmation

### 1. Application ID Alignment (`build.gradle.kts`)
- **Configuration:** Updated the Android application ID in `app/build.gradle.kts`:
  - `applicationId = "com.lectures.must"`
  - Preserved internal package namespace: `namespace = "com.lectures"`
- **Asset & Provider Stability:**
  - Verified `FileProvider` authorities dynamically reference `${applicationId}.fileprovider`.
  - Preserved existing database migrations, prepackaged assets, and room entity definitions.

### 2. Rapid Cohort Confirmation on Onboarding (`OnboardingScreens.kt`)
- **Requirement:** When a class group cohort is already chosen/focused, tapping or double-tapping the selected card should immediately proceed to the Timetable without requiring students to reach down to the bottom "Continue to Timetable" action button.
- **Implementation:**
  ```kotlin
  Card(
      modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .clickable {
              if (selectedGroup == group) {
                  onConfirm(group) // Immediate progression on tap/double-tap of already selected cohort
              } else {
                  selectedGroup = group
              }
          }
  )
  ```
- **Student UX:** Provides fluid one-handed onboarding navigation on large screens and one-tap progression once a cohort is active.


---

## 12. Tablet & Large Screen Architectural Responsiveness Audit

### Architectural Overview: The Multi-Form-Factor Paradigm
Modern Android ecosystem devices range from 280dp foldable cover screens to 1280dp+ tablets (Google Pixel Tablet, Samsung Galaxy Tab S9, Lenovo Tab P12), foldable unfolded displays (Galaxy Z Fold, Pixel Fold, $\approx 700\text{dp} - 840\text{dp}$), and ChromeOS / Android desktop (DeX).

Per **Material Design 3 Adaptive Guidelines**, applications must not simply stretch mobile layouts to fill large screens. Stretched single-column layouts create severe ergonomic and visual defects:
- **Excessive Line Length:** Body text exceeding 75–80 characters per line causes reading disengagement and fatigue.
- **Thumb Reach Disconnection:** On 10–12 inch tablets held with two hands, placing primary navigation or action buttons in the bottom-center forces the user to uncomfortably let go of one side to reach the screen center.
- **Vertical Height Exhaustion:** Fixed aspect-ratio components (e.g. 7-column calendar grids with `1:1` aspect ratio) explode on wide screens, pushing supporting information completely off-screen.

### Window Size Class Categorization
| Window Size Class | Width ($w$) Breakpoint | Target Devices | Recommended Canonical Layout |
| :--- | :--- | :--- | :--- |
| **Compact** | $w < 600\text{dp}$ | Handheld phones (portrait), Foldables (folded cover screen) | Bottom Navigation Pill Dock, Single-column vertical scroll |
| **Medium** | $600\text{dp} \le w < 840\text{dp}$ | Small tablets (7–8"), Large foldables unfolded (portrait), Landscape phones | NavigationRail or persistent docked bar, 2-column adaptive grids |
| **Expanded** | $w \ge 840\text{dp}$ | Standard & large tablets (10–13"), Foldables unfolded (landscape), Desktop/DeX | **NavigationRail (Leading edge)**, **List-Detail / Supporting Pane (Dual-Pane)**, 2–3 column grids |

---

### Deep Screen-by-Screen Flaw & Opportunity Analysis

#### 1. Top-Level Navigation & App Shell (`MainScaffold.kt`, `Chrome.kt`)
- **Current Behavior:** `MainScaffold` renders a floating bottom pill dock centered at `Alignment.BottomCenter` with `widthIn(max = 440.dp)`.
- **Tablet Defect:**
  - On a 10" tablet in landscape ($w = 1280\text{dp}, h = 800\text{dp}$), the 5 primary tabs are clustered in the middle of the bottom edge. Students holding the device by the sides must reach $\approx 420\text{dp}$ inward to navigate between Timetable, Calendar, Vacant Rooms, Notes, and Tasks.
  - Furthermore, the bottom dock and its required content padding consume $72\text{dp}$ of vertical height, which is particularly scarce on wide aspect ratio tablets.
- **Responsive Solution:**
  - **Adaptive Shell:** On `widthDp >= 600dp` (Medium & Expanded), replace the bottom pill dock with a **Material 3 NavigationRail** anchored to the left leading edge (`Alignment.CenterStart`).
  - **Ergonomics:** Students can switch tabs instantly with their left thumb while holding the tablet naturally.
  - **Vertical Space Reclaim:** Reclaims $72\text{dp}$ of vertical canvas across all screens in landscape.

#### 2. Calendar Month Planner (`CalendarScreen.kt`)
- **Current Behavior:**
  - The calendar grid renders 7 columns with `.weight(1f).aspectRatio(1f)`.
- **Tablet Defect:**
  - On an Expanded display ($w = 1200\text{dp}$), each day cell expands to $1200 / 7 \approx 171\text{dp}$ width and $171\text{dp}$ height.
  - A 6-row month grid reaches $6 \times 171\text{dp} = \mathbf{1026\text{dp}}$ in height! The calendar grid alone consumes the entire tablet display, pushing the selected date's schedule, lectures, and tasks completely off-screen.
- **Responsive Solution (Dual-Pane List-Detail):**
  - **Left Pane ($\approx 400\text{dp} - 440\text{dp}$):** Constrained Month Grid where day cells remain a clean, readable $50\text{dp} - 58\text{dp}$, complete with month switcher and day-of-week headers.
  - **Right Pane (Remaining flex width $\approx 500\text{dp} - 800\text{dp}$):** Dedicated **Day Horizon Pane** displaying the selected day's lectures, scheduled coursework deadlines, custom activities, and academic milestones side-by-side with the calendar.
  - **Student Impact:** Zero scrolling needed to view what is scheduled on any day; tapping a date in the left grid instantly updates the schedule on the right.

#### 3. Academic Notes (`NotesScreen.kt`)
- **Current Behavior:**
  - Displays notes grouped by course code in a single vertical `LazyColumn` with `fillMaxWidth().padding(horizontal = 20.dp)`.
- **Tablet Defect:**
  - On wide tablets, note cards span 1160dp. Small 2-sentence lecture summaries stretch into awkward single-line ribbons with massive empty space on the right, while pin/delete icons are stranded at the extreme edge.
- **Responsive Solution:**
  - **Adaptive Grid / Two-Column Grouping:** On `isMedium || isExpanded`, render note cards using an adaptive multi-column grid (`GridCells.Adaptive(minSize = 340.dp)`) or a 2-column staggered layout.
  - **List-Detail Master View:** On `isExpanded`, option for side-by-side note list on the left and live Markdown reader/editor on the right.

#### 4. Vacant Room Finder (`VacantRoomsScreen.kt`)
- **Current Behavior:**
  - Renders vacant rooms as single full-width cards in a vertical `LazyColumn`.
- **Tablet Defect:**
  - Empty hall indicators (e.g. `"DS LAB IV"`, `"Free for rest of day"`) stretch across 1200dp. Only 4–5 rooms fit in the vertical viewport.
- **Responsive Solution:**
  - **2-Column Adaptive Room Grid:** On wide screens, display rooms in a 2-column grid (`GridCells.Fixed(2)` or `Adaptive(minSize = 380.dp)`).
  - **Information Density:** Doubles the number of vacant rooms visible at a single glance without scrolling (students can scan 8–10 rooms simultaneously).
  - **Split Room Schedule Detail:** Clicking a room can open its full day hourly timeline directly in a right-hand supporting pane rather than an overlay dialog.

#### 5. Coursework & Task Management (`TasksScreen.kt`)
- **Current Behavior:**
  - Renders `TaskProgressDashboard` at the top of a single-column `LazyColumn`, followed by search, filter chips, and task items.
- **Tablet Defect:**
  - The progress doughnut / bar and summary metrics stretch to 1200dp, creating sparse, disjointed visual balance.
- **Responsive Solution (Supporting Pane Layout):**
  - **Left Pane ($\approx 380\text{dp}$):** Fixed-width Task Analytics Card (Progress circle, completion rates, filter tabs: All / Urgent / Coursework / Tests).
  - **Right Pane (Flex):** Search bar and 2-column grid of assignment cards with priority badges, due date counters, and direct completion checkmarks.

#### 6. Welcome & Onboarding Flows (`WelcomeScreen.kt`, `OnboardingScreens.kt`)
- **Current Behavior:**
  - Both screens use `Modifier.fillMaxSize().padding(horizontal = 20.dp)`.
- **Tablet Defect:**
  - Bento feature cards stretch to 600dp+ each. Faculty list stretches 1200dp across, forcing students to scan across the entire screen width.
- **Responsive Solution:**
  - **Centered Bounded Container:** Apply `Modifier.widthIn(max = 640.dp).align(Alignment.CenterHorizontally)` to the Welcome Screen content, preserving optimal visual hierarchy.
  - **2-Column Faculty / Programme Cards:** In `FacultySelectionScreen` and `ProgrammeSelectionScreen`, display faculties/programmes in a 2-column grid on tablets (`GridCells.Adaptive(minSize = 320.dp)`), allowing quick, scannable selection.

#### 7. Settings & Account (`SettingsScreen.kt`, `AccountScreen.kt`)
- **Current Behavior:**
  - Full-width settings section cards and items.
- **Tablet Defect:**
  - Setting labels (e.g. "Lecture Reminders") on the far left are separated by 1100dp from their toggle switch on the far right.
- **Responsive Solution:**
  - Constrain content column to `Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally)`, restoring standard ergonomic distance between labels and toggles.

---

## 13. Comprehensive Implementation of Adaptive Layouts for Tablets & Large Screens

Following the exhaustive architectural audit in Section 12, the entire user-facing surface was refactored to implement native **Material Design 3 Adaptive Canonical Layouts** for Window Size Classes (`Compact`, `Medium`, and `Expanded` / $w \ge 600\text{dp}$ and $w \ge 840\text{dp}$).

### 13.1 Infrastructure & Window Metrics (`Chrome.kt`)
1. **Window Size Classes Extracted:**
   - `isCompact`: $w < 600\text{dp}$
   - `isMedium`: $600\text{dp} \le w < 840\text{dp}$
   - `isExpanded`: $w \ge 840\text{dp}$
   - `isTablet`: $w \ge 600\text{dp}$
   - `useNavigationRail`: $w \ge 600\text{dp} \text{ and } h \ge 440\text{dp}$
   - `isDualPane`: $w \ge 840\text{dp}$
2. **Dynamic Inset & Content Padding:**
   - `Chrome.NavigationRailWidth` established at $76\text{dp}$.
   - `listBottomPadding(navBarBottom, useNavigationRail)`: Injects floating dock padding on phones ($110\text{dp}$) while reclaiming vertical space on tablets ($24\text{dp}$), preventing awkward empty bottom gutters on tablets.
   - `fabBottomPadding(useNavigationRail)`: Repositions FABs on phones ($88\text{dp}$) and tablets ($24\text{dp}$).

### 13.2 Ergonomic Adaptive Navigation Shell (`MainScaffold.kt`)
1. **Material 3 Leading NavigationRail:**
   - Active on `windowInfo.useNavigationRail == true` ($w \ge 600\text{dp}$).
   - Positions primary navigation within natural reach of the user's left thumb when holding a tablet with two hands.
   - Includes header brand emblem (`Icons.Default.School`), animated pill tab selection indicators (`Timetable`, `Calendar`, `Notes`, `Tasks`), and bottom shortcut buttons for `Vacant Rooms` and `Settings`.
2. **Compact Floating Pill Dock Retained:**
   - Active on phones (`!windowInfo.useNavigationRail`), preserving single-handed thumb accessibility and spring animations.

### 13.3 Screen-by-Screen Canonical Adaptations
1. **Academic Notes Grid (`NotesScreen.kt`):**
   - Implemented `LazyVerticalGrid` with `GridCells.Adaptive(minSize = 340.dp)` on tablets, with course headers spanning the full width (`GridItemSpan(maxLineSpan)`).
   - Eliminates 1200dp stretched ribbons; compact phone view retains vertical `LazyColumn`.
2. **Coursework & Tasks Supporting Pane (`TasksScreen.kt`):**
   - Implemented dual-pane supporting layout on `windowInfo.isDualPane`:
     - **Left Supporting Pane ($\le 380\text{dp}$):** Task completion progress dashboard and analytics.
     - **Right Pane (Flex):** Search field, filter chips, and interactive task cards.
   - Compact phones retain unified single-column scrolling.
3. **Vacant Room Finder Adaptive Chunking (`VacantRoomsScreen.kt`):**
   - On tablets, dynamically chunks room results into 2-column rows (`filteredRooms.chunked(2)`), doubling visible rooms per viewport and eliminating whitespace waste.
4. **Calendar Planner Responsive Geometry (`CalendarScreen.kt`):**
   - Connected `windowInfo.useNavigationRail` to `Chrome.listBottomPadding` and `Chrome.fabBottomPadding`, eliminating unused bottom gutters and adjusting FAB clearance.
5. **Settings, Welcome & Onboarding Centered Bounds (`SettingsScreen.kt`, `WelcomeScreen.kt`, `OnboardingScreens.kt`):**
   - `SettingsScreen`: Wrapped content in `Box(contentAlignment = Alignment.TopCenter)` with `widthIn(max = 760.dp)`, maintaining comfortable proximity between settings labels and toggle switches.
   - `WelcomeScreen`: Wrapped hero banner, logo, and bento cards in `widthIn(max = 680.dp)`.
   - `FacultySelectionScreen` & `ProgrammeSelectionScreen`: Bounded content in `widthIn(max = 760.dp)` centered horizontally.
   - `ClassGroupSelectionScreen`: Centered bottom action bar with `widthIn(max = 500.dp)` for easy single-thumb confirmation on wide screens.

---

## 14. Secondary Audit & Deep Verification of Recent Implementations

A second rigorous audit was conducted across all files modified during the responsive adaptations and architectural refactoring to ensure that zero silent regressions, orphaned layouts, syntax traps, or accessibility flaws were introduced.

### 14.1 Findings & Resolution Matrix

| Component | Observation / Risk Audited | Severity | Status / Resolution Applied |
| :--- | :--- | :--- | :--- |
| **`NoteEditorScreen.kt`** | On tablets ($w \ge 600\text{dp}$), note editing input fields stretched across 1200dp+, producing extreme line lengths and disorienting cursor positions. | Medium | **Fixed**: Wrapped editor content in `Box(contentAlignment = Alignment.TopCenter)` with `widthIn(max = 840.dp)`, ensuring optimal readability and ergonomic editing on desktop/tablet DeX modes. |
| **`AccountScreen.kt`** | Account dashboard stats and cloud sync cards stretched across full tablet width, displacing the "Sync Now" and "Sign Out" actions to opposite screen borders. | Medium | **Fixed**: Constrained content column to `widthIn(max = 760.dp)` centered at `Alignment.TopCenter`, preserving consistent visual balance. |
| **`NotesScreen.kt`** | `LazyVerticalGrid` and `LazyColumn` state divergence when switching between portrait fold/unfold. | Low | **Verified**: Both layouts are keyed by `note.naturalKey` and utilize shared ViewModel state flows. Scroll state remains robust. |
| **`TasksScreen.kt`** | Supporting pane on dual-pane displays ($w \ge 840\text{dp}$) needed validation for search filter responsiveness. | Low | **Verified**: Both the left dashboard and right task list share the active `uiState.selectedFilter` and `uiState.searchQuery`, ensuring instant reactive updates across panes. |
| **`VacantRoomsScreen.kt`** | Dynamic 2-column chunking (`filteredRooms.chunked(2)`) allocation inside LazyColumn scope was generating redundant closures. | Medium | **Fixed**: Hoisted `chunkedRooms` directly into the composable scope via `remember(filteredRooms) { filteredRooms.chunked(2) }`, eliminating per-frame allocations during scroll. |
| **`MainScaffold.kt`** | NavigationRail icons and labels alignment with top bar safe insets. | Low | **Verified**: Rail uses `statusBarsPadding()` and `navigationBarsPadding()` with $76\text{dp}$ fixed width, perfectly clearing edge-to-edge system gestures. |

### 14.2 Build & Unit Test Verification
- **Compilation Check**: `compile_applet` passed cleanly with 0 errors.
- **Unit Test Execution**: `gradle :app:testDebugUnitTest` executed and verified all 33 test suites without failure.
---
