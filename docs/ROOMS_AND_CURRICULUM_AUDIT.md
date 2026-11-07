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

