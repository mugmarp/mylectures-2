# Implementation Plan: Comprehensive UI/UX Defect Remediation

This plan addresses all verified P0 and P1 UI/UX flaws and cross-screen inconsistencies identified in the audit and prioritized in user alignment.

---

## 1. User Intent & Clarifications Alignment
* **Defect Scope:** Remediate all verified P0 and P1 defects across Calendar, Timetable, Vacant Rooms, Notification Center, Settings, and Notes.
* **Calendar Tasks UI:** Replace the naive weekday matching and hardcoded light-colored card with rich due-date resolution and the full, dark-mode-compatible interactive `TaskCard` (with completion toggling, priority, and notes).
* **Settings Sound Profile:** Add live local audio playback whenever a sound profile ("Chime", "Gentle Bell", "Standard", "System Default") is selected.

---

## 2. Proposed Changes & Technical Architecture

### Component A: Calendar Task Date Intelligence & Interactive Dark Mode Cards
* **File:** `app/src/main/java/com/mustime/features/calendar/CalendarViewModel.kt`
  - Add calendar date-matching logic to verify if an `Assignment` is due on a specific year/month/day.
  - Parse assignment due dates using `TaskDateTimeParser.calculateDueMillis(it.dueDate)`. Compare year, month, and day-of-month against the active calendar day.
  - Support fallback to weekday matching only if `calculateDueMillis` returns null and `dueDate` specifies a weekday name.
  - Update `hasTasksThisDay` in the month grid to use this date matcher so month dots accurately represent tasks.
  - Expose task toggle completion method (`toggleTaskComplete(task: Assignment)`) in `CalendarViewModel`.
* **File:** `app/src/main/java/com/mustime/features/calendar/CalendarScreen.kt`
  - Update `selectedDayTasks` to use the accurate date-matching helper.
  - Replace lines 640–700 (the hardcoded `Color(0xFFFFF7ED)` task card with broken white text in Dark Mode) with `TaskCard` from `com.mustime.features.tasks.TaskCard`.
  - Pass `onToggleComplete`, `isDark`, and proper click actions so tasks in the Calendar agenda can be checked off directly.

### Component B: Timetable Live Next-Up Real-Time Clock Ticker
* **File:** `app/src/main/java/com/mustime/features/timetable/ui/TimetableScreen.kt`
  - Currently, `globalNextUp` is memoized with `remember(entries, todayName)` (line 162), which never recalculates as the clock ticks during the day.
  - Update `NextUpTicker` or introduce a 15-second tick state (`currentTickSeconds`) so `globalNextUp` recomputes its active state, live progress percentage, and countdown minutes dynamically without requiring the user to switch tabs.

### Component C: Vacant Rooms Responsive Stacked Filters & Bottom Sheet Clearance
* **File:** `app/src/main/java/com/mustime/features/rooms/ui/VacantRoomsScreen.kt`
  - Line 284 currently locks Campus and Building dropdowns into a single horizontal `Row(weight = 1f)`. Integrate `windowInfo.filtersStacked`:
    - When `windowInfo.filtersStacked` is true (narrow screens < 360dp), stack Campus and Building selectors in a `Column` with full width.
    - When false, maintain the side-by-side `Row`.
  - Add `Modifier.navigationBarsPadding()` to the sticky bottom button container in the filter bottom sheet (lines 920–928) to prevent collision with Android 3-button navigation.
  - Convert `LazyColumn(modifier = Modifier.padding(padding))` to use `contentPadding = PaddingValues(...)` to prevent hard-edge scroll clipping above the navigation bar.

### Component D: Notification Center Natural Key Delimiter & Scroll Resilience
* **File:** `app/src/main/java/com/mustime/ui/components/NotificationCenterSheet.kt`
  - Line 295: Replace `note.naturalKey.split(":")` with `note.naturalKey.split("|")` to correctly isolate course codes (e.g. `BIT101|123456` -> `BIT101`) instead of displaying the raw composite key.
  - Add `.verticalScroll(rememberScrollState())` to the root `Column` or wrap the empty state and test buttons with scroll safety so content never overflows on compact phone viewports or high font scaling.

### Component E: Settings Audio Preview Playback
* **File:** `app/src/main/java/com/mustime/core/notification/NotificationHelper.kt`
  - Implement `previewSound(context: Context, soundName: String)`:
    - Resolves the sound URI for "Chime" (`sound_chime.wav`), "Gentle Bell" (`sound_gentle_bell.wav`), "Standard" (`sound_standard.wav`), or System Default.
    - Plays the sound via `RingtoneManager.getRingtone(context, soundUri)?.play()` or `MediaPlayer` with sound effects usage attributes.
* **File:** `app/src/main/java/com/mustime/features/settings/SettingsScreen.kt`
  - In `soundOptions.forEach { soundName -> ... }`, trigger `NotificationHelper.previewSound(context, soundName)` upon clicking any sound profile chip.

### Component F: Note Editor Clearance & Accessibility Touch Targets
* **File:** `app/src/main/java/com/mustime/features/notes/NoteEditorScreen.kt`
  - Add `Modifier.navigationBarsPadding()` to the bottom column content so footer buttons and attachments remain accessible when the IME keyboard is dismissed on 3-button navigation.
  - Increase the attachment delete `IconButton` size from 26dp to a touch target meeting the 48dp accessibility standard (`Modifier.size(48.dp)` with centered 18dp icon).
* **Files:** `MarkdownHelpDialog.kt` & `NoteTemplates.kt`
  - Add `Modifier.weight(1f, fill = false)` to unconstrained `LazyColumn` items in modal bottom sheets.

---

## 3. Verification Plan

### Automated Compilation & Local Tests
1. **Compilation Check:**
   - Execute `compile_applet` to verify Kotlin compilation, Compose layout modifiers, and imports.
2. **Local Robolectric Unit Tests:**
   - Run `gradle :app:testDebugUnitTest` via `run_command` to ensure no database, parser, or scheduling regressions were introduced.

### Manual CUJ & Behavioral Verification
1. **Calendar Task Sync & Dark Mode:**
   - Create a task due on a future date in Tasks tab. Navigate to Calendar.
   - Verify task dot appears on that calendar date and task card appears in the daily agenda.
   - Switch between Light and Dark themes to verify the task title, description, and chips have crisp, high-contrast readability.
   - Tap checkbox on Calendar task card and verify completion state toggles properly.
2. **Timetable Next-Up Live State:**
   - Inspect today's timetable when a class is ongoing or upcoming within 60 minutes.
   - Verify countdown text and progress bar update with time ticks.
3. **Vacant Rooms Responsiveness:**
   - Verify Campus and Building selectors stack smoothly when viewport width is narrow (< 360dp) and filter bottom sheet button has full navigation bar clearance.
4. **Settings Sound Preview:**
   - Tap "Chime", "Gentle Bell", and "Standard" in Settings to verify immediate audible playback.
5. **Notification Center Key Formatting:**
   - Open Notifications sheet with active note alarms; verify course codes display cleanly (e.g. `BIT101` instead of `BIT101|...`).
