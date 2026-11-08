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
