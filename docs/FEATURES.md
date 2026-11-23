# Features Specification — Lectures

**Application Name**: **Lectures**  
**Author**: **Mark Paul M**  
**GitHub**: [@mugmarp](https://github.com/mugmarp) • **Email**: [markpaulmu@gmail.com](mailto:markpaulmu@gmail.com)

This document details the functional specifications, user interface behaviors, and business rules implemented across all screens in **Lectures**.

---

## 1. 📅 Timetable & Live Class Tracker

### Primary Components:
* **Top App Bar**:
  * Displays the enrolled Academic Programme (e.g. `BCS Year 2 Semester 1`) with an Academic Schedule subtitle.
  * Fast-access action buttons: **Campus Vacant Rooms** icon, **Academic Profile Switcher**, and **Settings**.
* **Intelligent Timetable Filtering & Track Support**:
  * Cross-program and cross-faculty shared lectures are seamlessly surfaced via `entryMatchesGroup` and canonical alias mapping.
  * **Specialised Academic Tracks**: Programmes with curriculum specialisations support dedicated tracks:
    * `BS` (Science Education):
      * Year I: `BS BIOLOGICAL I`, `BS CHEM MATHS I`, `BS PHYSICAL I`
      * Year II: `BS BIOLOGICAL II`, `BS CHEM MATHS II`, `BS PHYSICAL II`
      * Year III: `BS BIOLOGICAL III`, `BS CHEMISTRY III`, `BS MATHEMATICS III`, `BS PHYSICS III`
      * *Elimination of synthetic parent groups*: Fictitious general parent groups (`BS I`, `BS II`, `BS III`) have been removed to prevent duplicate course overlaps and false schedule conflicts.
    * `HEAP` (Higher Education Access Certificate): Full support under the Faculty of Science for all 3 official combinations: `Biology-Chemistry`, `Chemistry- Maths`, and `Physics-Maths`.
    * `DLT` (Science Laboratory Technology): Year I (`DLT I`) and Year II tracks: `DLT BIOLOGY II`, `DLT CHEMISTRY II`, `DLT PHYSICS II`. Synthetic empty `DLT II` has been eliminated.
    * `BBA` (Business Administration): Year I (`BBA I`), Year II (`BBA II`), and Year III specializations: `ACCOUNTING`, `BANKING AND FINANCE`, `HUMAN RESOURCE`, `MARKETING`, `PROCUREMENT`. Synthetic empty `BBA III` has been eliminated.
    * `BPCD` (Planning & Community Development): Year I (`BPCD I`), Year II (`BPCD II`), and Year III tracks: `SUSTAINABLE RURAL DEVELOPMENT`, `URBAN POLICY`. Synthetic empty `BPCD III` has been eliminated.
  * **Dynamic Zero-Drift Class Counts**: Entry counts in onboarding and profile switching are derived directly from the timetable database (`filterTimetable`) rather than stale hardcoded maps.
  * **Contextual Guidance & Tailored Cohorts**:
    * Clinical medical programmes (`DCM`, `DEM`, `DCAM`) clearly inform students of hospital clinical rotations for unscheduled terms.
    * For programmes where specific academic years are absent from the central timetable (such as `BGWH` which only has Year III scheduled in this export), the onboarding and profile switcher restrict selection to genuine class groups (`BGWH III`), preventing confusing 0-entry choices.
  * **Optimized Switch Programme Dialog UX**:
    * **Year of Study Segmented Chips**: Display concise Roman numeral labels (`I`, `II`, `III`, `IV`, `V`) under the "Year of Study" heading, eliminating redundant "Year" repetitions and preventing multi-line text wrapping (e.g., `Ye\nar\nIV`).
    * **Decluttered Faculty Selection**: Stripped the redundant "Faculty of " prefix from all selectors and dropdown listings (e.g. `Applied Sciences & Technology`, `Computing & Informatics`, `Medicine`).
    * **Short-Form Degree Titles**: Adopted standardized academic short forms (`BSc`, `BEng`, `B.`, `Dip.`) preventing truncated dropdown choices on mobile screens.
    * **Seamless Navigation to Timetable**: Previewing or switching a programme from any app screen (Calendar, Notes, Tasks, or Settings) immediately dismisses the dialog/sheet and routes the user straight to the Timetable tab with the target schedule displayed.
* **Lecture Card Cohort & Subtitle Rules**:
  * **Unshared Lectures**: Subtitle displays **only the course unit code** (e.g. `BIO1101`), never repeating the student's own enrolled class.
  * **Shared Lectures**: Subtitle displays the course code followed by only the **other** cohorts sharing the session (e.g., `DVS2105 · BCS II` when enrolled in BSE II). The word "Shared" is omitted for conciseness.
  * **Canonical Alias Deduplication**: Duplicate sibling aliases from the university database (such as raw `BIOLOGY` vs prefixed `BS BIOLOGICAL III`) collapse into a single canonical cohort, eliminating repetitive 7-cohort clutter on shared education courses like `EDU3101`.
  * **MUCOSA / Association Events**: Cohort groups are completely omitted from subtitles.
  * **NextUpCard**: Cohort and shared class listings remain suppressed to preserve a clean, glanceable hero view.
  * **Data Anomaly Corrections**:
    * Dr. William Wasswa transferred from room column to lecturer field.
    * `NSAMBU` course code placeholder normalized to `BBA3105 · Taxation II · Dr. Nsambu Kijjambu Frederick`.
* **Academic Week Indicator**: The profile sheet and timetable header display the current academic week (e.g. `Week 7 · Sem 1`), computed from the semester start date.
* **Dynamic Campus Resolution**: Campus is derived from the enrolled programme — Kihumuro for Engineering/Computing/Science, Town for Medicine/Nursing/Business — not hardcoded.
* **Timetable Version & Status**: The profile sheet and Settings display the current timetable release (e.g. `Final Timetable · Official Release` or `Draft 2 · Active Draft`), synced from Firestore metadata on launch and refreshable via Settings → Sync Now.
* **Sound Profiles**: Four notification sound options — System Default (uses device notification sound), Chime, Gentle Bell, and Standard — applied to all three notification channels immediately on selection.
* **Anonymous Feedback**: Settings → Anonymous Feedback & Bugs opens a sheet with category chips (Bug Report, Timetable Discrepancy, Feature Request, Room Finder Issue, General), a message field, and optional diagnostics. Submissions are sent to Firestore `/feedback/` with an anonymous install UUID; offline submissions are queued and retried on next launch.
* **Happening Now / Next Up Hero Card**:
  * Dynamically computes the active class session in real-time.
  * **Live Mode**: Displays an animated glowing `LIVE NOW` badge, real-time progress bar (percentage completed), elapsed time ("35m elapsed"), and remaining time ("45m left · Ends 15:30").
  * **Next Up Mode**: Displays when the next class will occur (e.g. `"in 20m"` or `"Tomorrow at 09:00"`).
  * Quick Actions: Set alert bell, open class notes, or view full session details.
* **7-Day Capsule Selector**:
  * Displays pills for `Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`, and `Sun`.
  * Highlights the selected day with distinct primary container styling.
  * Marks today's date with a persistent badge indicator.
* **Category Filters**:
  * Filter pills for `All Sessions`, `Lectures`, `Labs & Practicals`, and `Student Associations`.
* **Session Timeline Cards (`TimelineClassCard`)**:
  * Left color-coded accent stripe matching session type:
    * **Lecture / Clinical**: Blue (`#0052CC`)
    * **Lab / Practical**: Green (`#10B981`)
    * **Student Association**: Purple (`#8B5CF6`)
  * Course Code, Course Title, Time Window, Venue (Room/Hall), and Lecturer name.
  * Integrated notification bell to set or cancel class alarms.

---

## 2. 👁️ Preview Mode & Structured Program Switching

### A. Academic Profile Switcher Hierarchy (`AcademicProfileSheet.kt`)
* **Two-Step Clean Hierarchy**:
  * **Step 1: Faculty Selection**: Dropdown menu covering all academic faculties.
  * **Step 2: Degree / Diploma Programme**: Dropdown displaying all valid degrees with curriculum length.
  * **Step 3: Year of Study**: Horizontal selector chips (`Year I`, `Year II`, `Year III`, etc.).
  * **Step 4: Subject Track / Specialization**: Rendered as responsive `FlowRow` chips with full, human-readable labels (`Biological`, `Chemistry - Maths`, `Physical`, `Banking & Finance`, `Sustainable Rural Dev`, etc.) whenever a year offers tracks.
  * **No Text Truncation**: Completely eliminates single-row vertical letter wrapping and clipped 3-column chip text.
* **Header Badge**: Displays the active resolved cohort (e.g. `BS MATHEMATICS III`).

### B. Preview Mode Specifications
* **Action Button in Dialog**: `Preview <Group>` (e.g. `Preview BCS II`).
* **Persistent Top Banner**: `Previewing <Group>`.
* **Subtitle Explanation**: `Viewing temporarily. Your alarms remain set for <EnrolledProgramme>.`
* **Exit Affordance**: Prominent `Exit Preview` button restoring the student's enrolled schedule with a single tap.
* **Safety Lock**: Background alarms and notifications remain strictly anchored to the enrolled programme during temporary previewing.

---

## 3. 📝 Obsidian-Grade Markdown Lecture Notes

### A. Notes Dashboard (`NotesScreen`)
* **Real-Time Search & Course Filtering**: Search across note titles and content with horizontal course pill selectors (`All Notes`, `BCS 2101`, `BIT 2104`...).
* **Master-Detail Desktop Workspace (`>= 960dp` pane width)**: Side-by-side notes library and interactive preview pane with 1-click **Open Split Editor** launch and proper FAB bottom-clearance.
* **Note Cards**: Displays note title, preview snippet with stripped markdown, course code badge, linked timetable session badge, attachment indicator, and class alarm status.
* **Bottom Navigation Clearance**: Floating Action Button clears both mobile floating pill docks and tablet/desktop side rails cleanly.

### B. Dedicated Markdown Editor (`NoteEditorScreen`) & Live Preview
* **Segmented Editor Modes**: Switch between **`Split`** (side-by-side live editor and rendered Markdown preview on wide workspaces), **`Write`**, and **`Preview`** with 48dp accessible tab touch targets.
* **Docked Desktop Inspector & Keyboard Shortcuts**: Dedicated sidebar/drawer for course tagging, pre-class session attachment, reminder lead time, and persistent file attachments (`AttachmentStore`), plus `Ctrl+B` (Bold), `Ctrl+I` (Italic), and `Ctrl+S` (Save).
* **Live Preview Syntax Highlighting (`MarkdownLiveVisualTransformation`)**:
  * Real-time 1:1 mapped syntax coloring inside the Compose `BasicTextField` without cursor drift or jump.
  * Formats headers (`#` to `######`), bold (`**`), italics (`*`), strikethrough (`~~`), inline code (`` ` ``), highlights (`==`), WikiLinks (`[[`), and callouts (`>`).
* **Obsidian Callouts & Admonitions**:
  * Renders `> [!NOTE]`, `> [!TIP]`, `> [!IMPORTANT]`, `> [!WARNING]`, `> [!DANGER]`, `> [!SUCCESS]`, `> [!INFO]`, `> [!QUESTION]`, `> [!EXAMPLE]`, and `> [!QUOTE]`.
  * Displayed inside custom themed Material 3 cards with distinct accent borders, background tints, and iconography.
* **Markdown Tables**:
  * Formats standard GitHub-Flavored Markdown tables with column alignment markers (`:---`, `:---:`, `---:`).
  * Horizontally scrollable container with distinct header tint and alternating row styling.
* **LaTeX-Style Math Equations**:
  * Inline math (`$formula$`) and centered block math (`$$equation$$`) rendered in serif italic typography with a 1-tap formula copy button.
* **Text Highlighting & Internal WikiLinks**:
  * Warm amber highlight tint for `==important text==`.
  * Tappable primary underlined links for `[[Lecture Topic]]`.
* **Smart Enter Key Auto-Continuation**:
  * Automatically continues checklist items (`- [ ] `), bullet points (`- `, `* `), numbered lists (`1. `), and callouts (`> `).
  * Clears the prefix when Enter is pressed on an empty item.
* **1-Tap Copyable Code Blocks**:
  * Fenced code blocks (` ```kotlin `) render with syntax language badge and 1-tap clipboard copy button.
* **Quick Action Toolbar & Read-Time Stats**:
  * Insert Markdown syntax with 1 tap: Bold, Italic, Strikethrough, Heading, Bullets, Checklists, Highlight (`==`), Callout (`[!Note]`), Table, Math (`$$`), and Link (`[[]]`), paired with live word/read-time estimation.
* **Cornell Note-Taking Template**:
  * Pre-structured template with cues, notes, math block, summary table, and action items.
* **Session, Attachment & Pre-Class Alarm Linkage**:
  * Link notes to specific timetable lectures with pre-class reminder alarms (10m, 15m, 30m, 45m, 60m) and internal app-scoped document attachments (`files/attachments/`).

---

## 4. 📆 Academic Calendar & Semester Horizon

The Academic Calendar is the student's unified macro-planning horizon, bridging official university academic milestones, coursework deadlines, scheduled classes, and personal study sessions.

* **Dual-Mode Planner Switcher**:
  * **Month Planner**: Interactive monthly matrix displaying workload indicators, multi-category badges, and a day agenda.
  * **Semester Milestones**: Chronological roadmap of the academic year (Orientation, Teaching Commencement, Add/Drop Deadlines, Continuous Assessment Tests, Revision Week, Final Examinations, and Recess).
* **Academic Calendar Engine (`AcademicCalendarEngine`)**:
  * Automatically resolves teaching weeks (Weeks 1 to 17), semester phase (`Teaching`, `Mid-Sem Tests`, `Revision`, `Exams`, `Recess`), and university milestones for any date.
  * Context-aware: Suppresses recurring timetable classes on official non-instructional days (recess and public holidays), preventing misleading class display.
* **Month Matrix Multi-Category Badging**:
  * **🔴 Deadlines / Assignments Due**: Prominent red/amber indicator for assignments due on that date.
  * **🔵 Scheduled Lectures**: Primary blue indicator for instructional class days.
  * **🟢 Personal Activities**: Forest green indicator for study sessions, lab prep, and revision blocks.
  * **🟣 Official Milestones / Holidays**: Purple badge for university dates and national holidays.
* **Daily Workload Diagnostic**:
  * Instant metric breakdown for the selected day: Total Lecture Contact Hours (`Xh Ym`), Pending Deadlines count (`X Due`), and Planned Activities count.
* **Priority "Deadlines Due Today" Section**:
  * Deadlines are hoisted above the chronological timeline with priority chips, remaining time, and checkbox completion, ensuring urgent coursework is never missed.
* **Chronological Day Agenda**:
  * Clean, typography-first Material 3 cards for classes and custom activities.
  * Tap to view session details, campus floor badges, or edit study activities.
* **Today Quick-Jump & Month Navigation**:
  * Dedicated "Today" chip in the top bar to instantly jump back to today's date from any month.
  * Smooth month and day transition controls.

---

## 5. ✅ Tasks, Deadlines & Dashboard

* **Add Task Course Unit Selector**:
  * Implemented as a read-only `ExposedDropdownMenuBox` populating exclusively from the student's enrolled academic courses.
  * Eliminates free text typing, typos, or malformed course unit codes.
* **Task Progress Dashboard (`TaskProgressDashboard.kt`)**:
  * **Visual Color Contrast**: Pending tasks are represented in blue (`Color(0xFF3B82F6)` / `Color(0xFFDBEAFE)`), while completed tasks are represented in emerald green (`Color(0xFF10B981)`).
  * **Progress Bar**: Dual-color bar where completed progress fills in emerald green over the blue pending background.
  * **Weekly Breakdown Stacked Bar Chart**: Visualizes completed vs pending tasks for each day from Monday to Sunday.
* **Notification Permission Prompt**:
  * Includes an explicit `X` dismissal button (`isNotificationBannerDismissed`) and a direct fallback to Android App Notification Settings (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`).
* **Bottom Navigation Clearance**:
  * Content padding has been extended with an explicit spacer (`listBottomPadding + 96.dp`) so task items and action buttons are never obscured behind the bottom dock or FAB.

---

## 6. 🏫 Campus Vacant Rooms Finder

* **Coverage**: **93 rooms across 7 buildings and 2 campuses**, derived from the university's
  own room allocation timetable (2026/2027 Semester I).
* **3-Pane Wide Workspace (`>= 960dp` pane width)**: Displays a dedicated Filter Rail, multi-column Room Grid, and docked Room Schedule Inspector side-by-side without blocking dialogs.
* **Compact Pill Search Bar**: Sleek 44dp height with a 24dp border radius pill shape, featuring instant room code search (e.g. `L1`, `LR2`, `Lab`, `IMS`) with one-tap clear affordance.
* **Dual-Segment Location Bar**: A prominent elevated card located directly below the search bar that cleanly decouples spatial location from status attributes:
  * **Left Segment (`📍 Campus`)**: Interactive dropdown selecting `All Campuses`, `Kihumuro Campus`, or `Town Campus` with live building counts and faculty default indicator (`★`).
  * **Right Segment (`🏢 Building`)**: Interactive dropdown dynamically repopulating buildings based on the selected campus (e.g. `Science Block`, `FCI Building`, `IMS`, `FAST`, `Clinical / Hospital`) with room counts per venue.
* **Streamlined Quick Filter Row**: Dedicated horizontal strip reserved for status filters (`🟢 Free now`, space categories: `Lecture Room`, `Lab`, `Library`, and quick faculty reset).
* **Campus Auto-Detection**: On open, the campus is inferred from the enrolled programme
  (BCS/BIT/BSE/BME/EEE/PEEM/CVE/MIE → Kihumuro; MBR/PHA/BNS/MLS/BSP/PHS/BS/DLT/BBA/BSAF/ECO/BPSM/BSAL/BGWH/BPCD → Town).
  A tappable "Reset to Faculty" affordance restores the inferred value whenever the student navigates away.
* **Suggestion Ranking**: Results are ordered by *how much new information* a suggestion carries —
  lecture rooms and labs first, libraries last. Then by longest free window, then by room code.
* **Status Indicators**:
  * **Vacant Now** — green, with time until the next class (`Free until 14:30 (1h 45m)`)
  * **Occupied** — red, with the active course code, programme group, and end time
  * **Permanently Vacant** — blue, for rooms with no scheduled sessions this semester
* **Unclipped Room Schedule Detail Dialog (`RoomScheduleDetailDialog`)**:
  * **Pinned Header**: Room title with floor badge (`FloorIndicatorBadge`) and a pinned circular close button (`IconButton`) guaranteed to stay visible without header distortion or truncation.
  * **Scrollable Body**: Dedicated `verticalScroll` area preventing content clipping across all screen sizes and display scalings.
  * **Polished Empty-State**: Replaces raw scraper notes with clean, student-friendly copy and status badges (`✓ Full Day Free`, `Quiet Study Friendly`).
  * **Pinned Action Button**: Full-width primary "Close" button pinned at the base of the dialog, always immediately reachable.

---

## 7. ⚙️ Settings, Unified Theming & Legal Regulations

* **Adaptive Dual-Pane Settings (`>= 840dp`)**: Displays a persistent left category rail (*Overview & Profile*, *Notifications & Alarms*, *Appearance & Theme*, *Data & Cloud Sync*, *About & Data Reset*) alongside a focused detail workspace.
* **Academic Profile Management**:
  * Clean, typography-first card displaying enrolled degree, cohort code, and faculty with rapid **Switch** reconfiguration.
* **Single Unified Appearance & Theme Flow**:
  * Theme controls live exclusively in **Settings → Appearance & Theme**, eliminating redundant quick toggles elsewhere in the app.
  * Visual card previews for **Light**, **Dark**, and **System** modes, paired with 5 Material 3 accent color swatches (*Royal Blue*, *Emerald*, *Violet*, *Amber*, *Ocean Teal*) and a **Live Study Preview** card confirming WCAG contrast.
* **Notifications, Digest & Sound Delivery**:
  * Master **Class & Task Reminders** switch, class lead time presets (10m–60m + Custom), task lead times (1h–1d), daily 7:30 AM **Morning Schedule Digest** toggle, exact-alarm diagnostic banner, and 1-tap Android channel sound/vibration settings.
* **Developer, Feedback, Legal & Data Controls**:
  * Independent developer attribution (**MUGENDAWALA MARK PAUL**), non-affiliation disclaimer, terms of service, local-first privacy policy, anonymous feedback sheet with offline retry queue (`pending_feedback_retry_queue`), offline JSON data export (`lectures_backup_YYYYMMDD.json`), and guarded note/task reset dialogs.

---

## 8. ☁️ Google Account & Cloud Synchronization

* **Offline-First Guest Mode**: The entire app functions with zero network connection and no account required.
* **Jetpack Credential Manager Sign-In**: Optional Google Sign-In with official Google Identity services and dedicated Account workspace screen (`AccountScreen`).
* **Bidirectional Cloud Sync**: Automatically backs up lecture notes, assignments, custom calendar activities, and schedule preferences to owner-scoped Firestore documents (`/users/{uid}/...`).
* **Smart Conflict Resolution**: Client timestamp tracking merges offline changes cleanly, preserves concurrent note edits as `[Sync Conflict]` drafts, and enforces strict `ownerId` isolation on sign-out.
* **Offline Export & Sync Controls**: Preserved local record counts, on-demand **Sync Now** / **Sync Cloud**, and local JSON backup export.

---

## 9. 🔄 Play-Compliant Release Checker

* **Remote Version Inspection**: Checks `version.json` and the GitHub Releases API with smart 15-minute in-memory caching to avoid rate limits.
* **Strict HTTPS & Downgrade Prevention**: Enforces HTTPS release URLs (`isSecureHttpsUrl`) and semantic version comparison (`isNewerRelease`) against `BuildConfig.VERSION_NAME` / `VERSION_CODE`.
* **Play-Compliant Browser Redirect**: Displays student-friendly release highlights in `AppUpdateDialog` and opens the verified HTTPS GitHub Releases page via `Intent.ACTION_VIEW` — without sideloading APK installers or declaring `REQUEST_INSTALL_PACKAGES`.
* **Legacy Cleanup & Background Throttling**: Purges any stale cached update files on launch and throttles background worker release notifications to every 36 hours.

---

## 10. 📱 Adaptive Viewport, Tablet & Desktop Engine

* **Multi-Tier Window Metrics (`Chrome.kt` & `LocalLecturesWindow`)**: Adapts continuously across `COMPACT` (`< 600dp`), `MEDIUM` (`600–839dp`), `EXPANDED` (`840–1199dp`), `LARGE` (`1200–1599dp`), and `EXTRA_LARGE` (`>= 1600dp`), propagating true post-sidebar pane width (`LocalLecturesWindow`, `isWideWorkspace >= 960dp`) to nested screens.
* **Responsive Shell & Keyboard Navigation**: Switches from the mobile bottom floating pill dock (`< 600dp`) to a leading `NavigationRail` (`600–1199dp`) or collapsible `228dp` `ExpandedSidebar` (`>= 1200dp`), with desktop hover/hand-cursor feedback (`DesktopErgonomics.kt`) and shortcuts (`Ctrl+1..6`, `Ctrl+,`).
* **FlowRow & Compact Header Adaptation**: Prevents clipping under large display/font scaling (`1.5x`) and 320dp narrow screens using `FlowRow` wrapping, icon-compact headers, `.imePadding()`, and vertically scrollable dialogs/pickers.
