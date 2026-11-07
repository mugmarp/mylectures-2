# Notification System Architecture & Implementation Report

This report outlines the full investigation into MuSTime's notification system (both system-level and in-app), the flaws and missing capabilities uncovered, and the complete solutions implemented.

---

## 1. Deep Investigation & Flaws Identified

### Flaw 1: Lack of Inline Dismissal and Actionable Controls in System Notifications
* **Root Cause**: While system notifications had `.setAutoCancel(true)`, they lacked actionable inline action buttons (`AddAction`). When an alarm or reminder fired on the user's phone, the user had to either tap the notification to open the app or swipe it away blindly without any quick choices.
* **Impact**: In class situations or meetings, students could not quickly snooze an alarm or dismiss it directly from the lock screen / heads-up notification shade.

### Flaw 2: Cold-Start Deep Link Intent Loss
* **Root Cause**: In `MainActivity.kt`, notification click intents were broadcast via `MutableSharedFlow(extraBufferCapacity = 1)` with `replay = 0`.
* **Impact**: When the app process was killed or cold-started by tapping a system notification, `MainActivity.onCreate` emitted into the flow before Compose's `MainScaffold` or child composables subscribed. The intent was dropped immediately, stranding the student on the default screen instead of navigating to the target class or task.

### Flaw 3: Absence of Persistent In-App Notification Center / History
* **Root Cause**: The original `NotificationCenterSheet.kt` was purely an "Active Alarms" inspector. There was no notification history, inbox, or log. Once a system notification was swiped away on Android, it was gone forever. Students had no way to review past class alerts, reminders, or overdue task notices.

### Flaw 4: Missing Real-Time In-App Foreground Alarms
* **Root Cause**: If the student was already using the app when a class alarm or task reminder triggered, Android posted a system status bar notification, but the app itself provided no heads-up in-app visual alert or confirmation inside the active screen.

### Flaw 5: Inability to Manually Cancel Active Alarms from Notification Center
* **Root Cause**: In the old `NotificationCenterSheet`, alarms were listed as static read-only cards without cancellation buttons. To turn off a class alarm, students had to navigate to the Notes tab, find the lecture note, enter edit mode, and reset the alarm picker.

### Flaw 6: Inconsistent Access Across Screens
* **Root Cause**: The notification bell icon was only present in `TimetableTopAppBar`. It was missing on `CalendarScreen`, `NotesScreen`, and `TasksScreen`.

---

## 2. Solutions Implemented & Capabilities Added

### A. Inline System Notification Actions (Dismiss & Snooze)
* **Dismiss Action**: Added an inline "Dismiss" action (`ACTION_DISMISS_CLASS_ALARM` / `ACTION_DISMISS_TASK_REMINDER`) to all notification types (`showClassAlarmNotification`, `showClassReminderNotification`, `showTaskReminderNotification`).
* **Snooze Action**: Added a 10-minute snooze action (`ACTION_SNOOZE_CLASS_ALARM`) with `AlarmScheduler.scheduleSnoozeAlarm(...)` so students can defer alarms directly from the notification shade.
* **Task Actions**: Added "Mark Done" (`ACTION_MARK_TASK_DONE`) and "Dismiss" buttons for academic task alerts.

### B. Cold-Start Deep Link Replay Guarantee
* Updated `MainActivity.kt`'s `notificationIntentFlow` to `MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)`.
* When the app cold-starts from a notification tap, `MainScaffold` immediately receives the replayed intent upon composition, switches to the target tab (`Tasks` or `Timetable`), and passes down the highlighted task or class identifier.
* In `TasksScreen.kt`, auto-scrolling with `LaunchedEffect(highlightTaskId)` scrolls directly to the highlighted task and displays an accent border.

### C. Persistent In-App Notification Store (`InAppNotificationStore`)
* Built `InAppNotificationStore` using Android SDK's `org.json` and private `SharedPreferences` (persisting up to 100 recent alerts without Room schema migrations).
* Automatically records every alarm and reminder:
  - Class Alarms (`CLASS_ALARM`)
  - Class Reminders (`CLASS_REMINDER`)
  - Task Reminders (`TASK_REMINDER`)
  - Daily Digests (`DAILY_DIGEST`)
* Exposes reactive `notifications: StateFlow<List<InAppNotificationItem>>` and `unreadCount: Flow<Int>`.
* Includes full lifecycle operations: `markAsRead`, `markAllAsRead`, `deleteNotification`, and `clearAll`.

### D. In-App Real-Time Heads-Up Banner
* Added `InAppNotificationStore.foregroundAlertFlow` which emits whenever an alarm or reminder triggers.
* Implemented a floating in-app banner in `MainScaffold.kt` with animated slide-in, notification type icon, clear message, tap-to-navigate action, manual dismiss button, and automatic 6.5s timeout.

### E. Redesigned Centralized Notification Center (`NotificationCenterSheet`)
* Added two tabs:
  1. **Active Alarms**: Displays all scheduled lecture note alarms and task reminders. Each card features a one-tap **Alarm Off** (`AlarmOff`) button to immediately cancel the `AlarmManager` wake-up and update the database.
  2. **Alert History**: Shows chronological alert logs with unread badges, timestamp formatting (e.g. "Just now", "15m ago", "Yesterday"), mark all as read, clear history, and tap-to-open target screens.

### F. Uniform Top Bar Notification Bell Across All Tabs
* Integrated the notification bell with real-time `BadgedBox` (unread red dot) on:
  - **Timetable Tab** (`TimetableTopAppBar`)
  - **Calendar Tab** (`FloatingIslandTopBar`)
  - **Academic Notes Tab** (`FloatingIslandTopBar`)
  - **Tasks & Deadlines Tab** (`FloatingIslandTopBar`)
* Allows immediate access to alarms and alert history from any screen in the application.

---

## 3. Verification
* **Applet Compilation**: Verified via `compile_applet` — build succeeded cleanly.
* **Unit & JVM Test Suite**: Verified via `gradle :app:testDebugUnitTest` — 100% test pass (BUILD SUCCESSFUL).

---

## 4. Comprehensive UI/UX Deep Audit & Polishing (All Screens)

### A. Vacant Rooms Finder (`VacantRoomsScreen.kt`)
* **Problem**: Stacking 4 bulky vertical filter tiers (Campus picker, Building picker, Category chips, Search bar, Status row) consumed ~280dp (~45% of vertical screen real-estate), pushing available rooms below the fold.
* **Solution**:
  - Replaced the multi-tier header with a unified, modern search bar (46dp height, rounded 12dp, clear button).
  - Streamlined filtering into a single, horizontal, smoothly scrollable chip row (`LazyRow`) featuring anchored dropdown chips for **Campus** and **Building**, a 1-tap **Free now** check chip, and space category pills.
  - Reduced header vertical overhead by ~180dp, bringing vacant room cards immediately above the fold on launch while preserving full dropdown selection and the deep `FilterAndScheduleSheet`.

### B. Academic Notes (`NotesScreen.kt`)
* **Problem**: Non-functional, dead-end `Icons.Default.Mic` in the search bar; hardcoded hex color values (`Color(0xFF0F172A)`, `Color(0xFFF8FAFC)`) causing dark-mode and theme drift; lack of 1-tap action on empty state.
* **Solution**:
  - Completely removed the unclickable microphone icon.
  - Migrated search bar, feedback banners, and backgrounds to semantic tokens (`MaterialTheme.colorScheme.onSurface`, `MaterialTheme.colorScheme.onSurfaceVariant`, `outlineVariant`, etc.).
  - Added an interactive **Create Note** button directly in the center empty state.

### C. Academic Calendar (`CalendarScreen.kt`)
* **Problem**: 5.dp calendar grid indicator dots with 2.dp spacing risked card edge clipping and vertical tension on narrow 360dp phone screens.
* **Solution**:
  - Optimized dot dimensions to 4.dp with 1.5.dp horizontal spacing and 1.5.dp top spacer.
  - Eliminated month-end date drift by anchoring day navigation to Day 1.
  - Precomputed month academic milestones and day contexts using `AcademicCalendarEngine.kt`.

### D. Academic Tasks (`TasksScreen.kt`)
* **Problem**: Developer jargon (`"AlarmManager"`) in empty states; duplicate adjacent badges on overdue high-priority cards (`[⚠️ Overdue]` and `[🚩 High]`).
* **Solution**:
  - Replaced technical jargon with student-friendly copy (`"reminders and alarms"`).
  - Contextualized empty state messages per active filter (Overdue, Completed, High Priority, Pending).
  - Consolidated redundant badges into a single high-visibility pill: `[⚠️ Overdue • High Priority]`.

### E. Settings & Developer Profile (`SettingsScreen.kt`, `DeveloperProfileSheet.kt`)
* **Problem**: Danger Zone positioned immediately adjacent to primary navigation cards; outdated lab URLs; unclear developer branding copy.
* **Solution**:
  - Moved `DANGER ZONE` (Reset Notes, Reset Tasks) to the bottom of the screen with a 28dp separation gap.
  - Discarded `tirallab.page` and simplified Developer header row to `Developer` and `Profile`.
  - Refined Developer Profile hero bio: `"BSE student at MUST. Built the Lectures app as a fast, offline-first companion for class schedules and vacant campus rooms."`

---

## 5. Deep Performance, Navigation Speed & Animation Overhaul

### A. Fluid Sub-Screen Navigation Transitions (`MainScaffold.kt`)
* **Problem**: Navigating to `SettingsScreen`, `VacantRoomsScreen`, or `AccountScreen` used abrupt hard cuts (`if/else` replacements), causing visual flickering and jarring context switching.
* **Solution**:
  - Encapsulated screen destinations into `MainScreenDestination` (`TABS`, `SETTINGS`, `VACANT_ROOMS`, `ACCOUNT`).
  - Implemented Android 14/15-style directional push transitions using `AnimatedContent`:
    - Entering sub-screen: forward slide from right (`slideInHorizontally + fadeIn(tween(220))`) paired with depth slide out to left (`slideOutHorizontally(-width / 3) + fadeOut(tween(150))`).
    - Popping back to tabs: reverse slide from left (`slideInHorizontally(-width / 3) + fadeIn(tween(220))`) paired with slide out to right (`slideOutHorizontally(width) + fadeOut(tween(150))`).

### B. Directional Tab Switching Animation (`MainScaffold.kt`)
* **Problem**: Tapping between bottom dock tabs (Timetable, Calendar, Notes, Tasks) cut instantaneously without spatial continuity.
* **Solution**:
  - Integrated `AnimatedContent` on `selectedTab` with directional detection:
    - Forward tab transitions (e.g. Timetable -> Notes): slide in from right (+14% offset) + subtle fade.
    - Backward tab transitions (e.g. Tasks -> Timetable): slide in from left (-14% offset) + subtle fade.
    - Utilizes `spring(stiffness = Spring.StiffnessMediumLow)` for snappy, 60fps transitions without blocking the UI thread.

### C. Tactile Floating Dock Feedback (`MainScaffold.kt`)
* **Problem**: Selected tabs swapped instantly without tactile indication or smooth color transition.
* **Solution**:
  - Added `animateColorAsState(tween(180))` for active pill container background.
  - Added `animateFloatAsState(spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium))` scaling active icons to `1.08x` with `graphicsLayer`.

### D. Real-Time In-App Foreground Toast Banner (`MainScaffold.kt`)
* **Problem**: If an alarm or reminder triggered while the user was actively using the app, `InAppNotificationStore.foregroundAlertFlow` was never collected in `MainScaffold.kt`, leaving the student without a heads-up alert.
* **Solution**:
  - Added reactive collection of `InAppNotificationStore.foregroundAlertFlow`.
  - Implemented top-anchored floating heads-up alert banner with `slideInVertically + fadeIn`, 5-second auto-dismiss timer, 1-tap navigation to the alert's target tab, and manual dismiss button.

### E. List Performance & Placements Animation (`animateItem()`)
* **Problem**: Modifying tasks, swiping calendar days, reordering notes, or filtering rooms caused abrupt visual pops in `LazyColumn` lists.
* **Solution**:
  - Added Compose 1.7+ `Modifier.animateItem()` across:
    - `TimetableScreen.kt` (`TimelineClassCard` & `ActivityCard`)
    - `TasksScreen.kt` (`TaskCard`)
    - `NotesScreen.kt` (`NoteCardItem`)
    - `VacantRoomsScreen.kt` (`RoomVacancyCard`)
    - `CalendarScreen.kt` (`TaskCard` & `CalendarTimelineItem` cards)
  - Enables smooth spring-based reordering and entrance/exit animations for all list mutations.


