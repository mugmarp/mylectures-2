# Developer Guide — Lectures

**Application Name**: **Lectures**  
**Author**: **MUGENDAWALA MARK PAUL**  
**GitHub**: [@mugmarp](https://github.com/mugmarp) • **Email**: [markpaulmu@gmail.com](mailto:markpaulmu@gmail.com)

This guide assists software engineers and contributors in setting up, maintaining, and extending **Lectures**.

---

## 1. Environment Setup

### Required Tools
* **JDK**: OpenJDK 17 or Eclipse Adoptium Temurin 21
* **Android SDK**: Compile SDK 36 (minor 1), Target SDK 35, Min SDK 26 (Android 8.0 Oreo)
* **Gradle**: Gradle 9.x using Kotlin DSL (`.gradle.kts`)
* **IDE**: Android Studio Ladybug (2024.2+) or higher with Jetpack Compose support

### Build & Verification Commands
```bash
# Compile Kotlin check
gradle :app:compileDebugKotlin

# Build debug & minified release APKs
gradle assembleDebug
gradle :app:assembleRelease

# Run full unit + Robolectric + Compose UI test suite
gradle :app:testDebugUnitTest

# Verify Roborazzi visual snapshots
gradle :app:verifyRoborazziDebug

# Run code style and Android lint checks
gradle check
```
> Use `gradle` directly (not `./gradlew`).

---

## 2. Adding a New Academic Programme or Timetable Dataset

Timetable schedules are bundled inside the assets folder:
* Location: `app/src/main/assets/timetable_export.json`

### JSON Structure:
```json
[
  {
    "courseCode": "BCS 2101",
    "courseTitle": "Data Structures & Algorithms",
    "dayOfWeek": "Monday",
    "startTime": "08:00",
    "endTime": "10:00",
    "room": "LT 2",
    "lecturer": "Dr. Sarah Namubiru",
    "sessionType": "Lecture",
    "program_group": "BCS II"
  }
]
```

### Steps to Add:
1. Append new JSON entries with the corresponding `programmeGroup` string.
2. In `app/src/main/java/com/lectures/features/onboarding/FacultyData.kt`, ensure the faculty, programme name, and years are registered in the `FACULTIES` list.
3. Upon launch, `DataLoader.kt` automatically verifies hashes and updates SQLite via Room.

---

## 3. UI Styling, Material 3 Theming & Adaptive Windows

* **Theme & Window Definitions**: `app/src/main/java/com/lectures/ui/Theme.kt` and `Chrome.kt`
* **Theme & Accent Tokens**:
  * Supports **Light**, **Dark**, and **System** modes with 5 accent palettes (`AccentColorNames`: Royal Blue, Emerald, Violet, Amber, Ocean Teal).
  * Always read colors from `MaterialTheme.colorScheme` or `LocalExtendedColors.current` (e.g., `surfaceCard`, `surfaceElevated`, `borderSubtle`, `textPrimary`, `textMuted`, `successBg`, `warningBg`) rather than hardcoding hex colors in Composables.
* **Adaptive Pane-Aware Layouts**:
  * Observe `val windowInfo = rememberLecturesWindow()` (backed by `LocalLecturesWindow` inside `MainScaffold`'s rail/sidebar shell so screens measure true available pane width after the `228dp` sidebar).
  * Gate desktop/tablet split panes on `windowInfo.isDualPane` / `windowInfo.isWideWorkspace` (`>= 960dp`) instead of reading raw `LocalConfiguration.current.screenWidthDp`.
* **Component Touch Targets & Desktop Ergonomics**:
  * All interactive icons, chips, segmented tabs, and buttons must meet `>= 48dp` minimum interactive bounds (`.minimumInteractiveComponentSize()` or `defaultMinSize(minHeight = 48.dp)`) and apply `.desktopHoverable()` / `.desktopHandCursor()` where appropriate.

---

## 4. Performance Guidelines

When developing new features:
1. **Never use `IntrinsicSize.Min` or `IntrinsicSize.Max` inside `LazyColumn` items**:
   * Use `Box` with `Modifier.matchParentSize` for decorative backgrounds and accent bars to preserve single-pass layout rendering.
2. **Avoid Full-Screen Pointer Gesture Interceptors**:
   * Do not place horizontal drag listeners over root containers holding vertical scrollers.
3. **Always supply stable keys to `items` in Lazy lists**:
   * Example: `items(notes, key = { it.naturalKey }) { ... }`
4. **Wrap expensive calculations in `remember`**:
   * Date formatting, filtering, and regex operations should be memoized with their inputs.
