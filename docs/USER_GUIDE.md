# User Guide — Lectures

**Application Name**: **Lectures**  
**Author**: **Mark Paul M**  
**GitHub**: [@mugmarp](https://github.com/mugmarp) • **Email**: [markpaulmu@gmail.com](mailto:markpaulmu@gmail.com)

Welcome to **Lectures**! This user manual walks you through all the academic features on your Android device.

---

> ### ⚠️ Notice of Non-Affiliation
> Lectures is an independently developed academic student companion created by Mark Paul M. **Lectures is NOT an official app of, nor is it endorsed by, affiliated with, or sponsored by Mbarara University of Science and Technology (MUST).** Always check official university sources — [timetable.must.ac.ug](https://timetable.must.ac.ug) and the [student portal](https://systems.must.ac.ug) — for schedule announcements, test dates, and room changes.

---

## 1. Getting Started: Setting Up Your Academic Programme

When you first launch Lectures, you will be prompted to select your academic profile:
1. Select your **Faculty** (e.g. *Faculty of Computing and Informatics*, *Faculty of Science*, *Faculty of Medicine*).
2. Choose your **Degree or Diploma Programme** (e.g. *Bachelor of Science with Education*, *Bachelor of Computer Science*, *Higher Education Access Certificate*).
3. Choose your **Academic Class Group**:
   * For standard degrees: pick your year (`Year I`, `Year II`, `Year III`...).
   * For degrees with specialized tracks (such as `BS`, `HEAP`, `DLT`, `BBA`, or `BPCD`):
     * Select your **Year of Study**.
     * Select your **Subject Track / Specialization** (e.g. *Biological*, *Chemistry - Maths*, *Physical*, *Banking & Finance*, *Rural Development*).
4. Tap **Continue to Timetable**. The app immediately loads your full university schedule offline.

> **Tip**: You can switch or preview any programme anytime by tapping the **Profile** icon in the top right corner of the Timetable or Calendar screens, or via the Settings screen.

---

## 2. Navigating the Timetable & Reading Lecture Cards

* **Happening Now Hero**: Look at the top card on the Timetable screen. If you have an active class, it turns into a live tracker displaying a pulsating `LIVE NOW` badge, elapsed minutes, and a visual progress bar.
* **Switching Days**: Tap any day capsule (`Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`, `Sun`) to view that day's schedule. Today's date is always highlighted.
* **Reading Lecture Cards**:
  * **Unshared Lectures**: Subtitles display only the course unit code (e.g. `BIO1101`), keeping your view clean and clutter-free.
  * **Shared Lectures**: Subtitles display the course code followed by the other sharing classes (e.g. `DVS2105 · BCS II`).
  * **Association Events**: Events such as `MUCOSA` display without irrelevant class groups.
* **Setting Alarms**: Tap the **Bell** icon on any class card to set a reminder (10m, 15m, or 30m before class). Your phone will notify you with the course title and room number even if the app is closed.
* **Adding Personal Activities**: Tap the blue **Add Activity** button at the bottom right. Choose an activity category (*Study & Revision*, *Lab & Practical*, *Sports*, *Club*), tap **Start Time** and **End Time** to pick times via the circular clock, and tap **Add Activity**.

---

## 3. Preview Mode: Viewing Other Class Schedules Safely

Need to check when a friend has a lecture or verify a room without disrupting your own alarms? Use **Preview Mode**:
1. Tap the **Profile** icon in the top bar.
2. Tap **Switch Programme**.
3. Choose the faculty, programme, year, and track you want to inspect.
4. Tap the outlined **Preview <Class>** button (e.g. `Preview BCS II`).
5. A persistent amber banner will confirm you are in preview mode:
   * **Banner**: `Previewing BCS II`
   * **Subtitle**: `Viewing temporarily. Your alarms remain set for <Your Enrolled Programme>.`
6. When done, tap **Exit Preview** on the banner to immediately return to your official schedule.

---

## 4. Writing & Structuring Notes with Obsidian Markdown

Lectures features a full Obsidian-grade Markdown editor with real-time Live Preview:

1. **Focused Write, Preview & Desktop Split Modes**:
   * On phones, switch between **Write** and **Preview**. On wide tablets and desktops, use **Split** for side-by-side live editing and rendered Markdown preview, alongside a docked Inspector sidebar.
2. **Live Preview Highlighting**:
   * Type Markdown directly into the editor — syntax elements like `# Headers`, `**bold**`, `*italic*`, `~~strike~~`, and `` `code` `` are highlighted in real-time as you write.
3. **Obsidian Callouts & Admonitions**:
   * Add callout alert boxes by typing `> [!NOTE]`, `> [!TIP]`, `> [!IMPORTANT]`, `> [!WARNING]`, or `> [!DANGER]`.
   * Follow with your text on lines starting with `> `.
4. **Markdown Tables**:
   * Write clean tables using standard Markdown pipes:
     ```markdown
     | Course | Room | Day |
     |:-------|:----:|----:|
     | BCS2101| LT 2 | Mon |
     ```
   * Tables format automatically into horizontally scrollable cards with distinct headers.
5. **Math Formulas**:
   * Write inline math with single dollar signs: `$f(x) = \sin(x)$`.
   * Write centered block equations with double dollar signs: `$$E = mc^2$$`.
6. **Text Highlights & WikiLinks**:
   * Highlight key definitions with `==important term==`.
   * Create links to other courses or topics with `[[Course Code or Concept]]`.
7. **Smart List Auto-Continuation & Desktop Shortcuts**:
   * Start a checklist `- [ ] `, bullet `- `, or number `1. ` — pressing **Enter** automatically continues your list on the next line. Press Enter again on an empty line to cleanly clear it.
   * On keyboards, press `Ctrl+B` (bold), `Ctrl+I` (italic), and `Ctrl+S` (save).
8. **Cornell Template & Quick Toolbar**:
   * Tap the **Templates** button to insert a ready-to-use Cornell note-taking structure with cues, lecture notes, math blocks, and summary tables.
   * Tap `?` for a quick Markdown syntax reference cheat sheet.
9. **Class Linkage, Attachments & Alarms**:
   * Tag with your course subject, attach a timetable lecture with pre-class alerts, or attach PDF/document files saved safely inside private app storage.

---

## 5. Using the Academic Calendar & Semester Horizon

1. Tap the **Calendar** tab in the bottom floating dock.
2. **Switching Modes**:
   * **Month Planner**: View the monthly matrix with colored workload indicators (Red for Deadlines, Blue for Lectures, Green for Activities, Purple for Milestones). Tap any day to inspect its schedule and workload diagnostic pills.
   * **Semester Milestones**: Tap to open the full academic year roadmap (Orientation, Teaching Commencement, Continuous Assessment Tests, Revision Week, Final Examinations, and Recess). Tap any milestone to immediately jump to its start date on the calendar.
3. **Deadlines Due Today**:
   * Any assignments due on the selected date appear prominently in the **Deadlines Due Today** banner at the top of the agenda. Check the checkbox to mark completed.
4. **Scheduling Personal Activities**:
   * Tap **Schedule Activity** (or the `+` FAB).
   * Choose your activity category (*Study & Revision*, *Lab & Practical*, *Sports*, *Club*).
   * Set your start and end times or use the duration chips (`+1 hr`, `+1.5 hrs`, `+2 hrs`).
   * Tap **Save Activity** to add it to your agenda.
5. **Jump to Today**: Tap the **Today** chip in the top bar from any month to return to the current date.

---

## 6. Tracking Tasks, Deadlines & Weekly Progress

1. Tap the **Tasks** tab in the bottom floating dock.
2. **Weekly Progress Dashboard**:
   * View your total task completion percentage.
   * Completed tasks are highlighted in **emerald green**, while pending tasks are represented in **blue**.
   * Inspect the daily bar chart from Monday to Sunday to balance your academic workload.
3. **Adding an Assignment**:
   * Tap the **Add Task** button.
   * Select your **Course Unit** directly from the read-only dropdown containing only your enrolled courses.
   * Enter the task title and choose the category (*Assignment*, *Lab Report*, *Project*, *Exam Prep*, *Reading*).
   * Tap **Due Date & Time** to set the deadline with the scheduler picker.
   * Pick reminder lead time (e.g. 30m, 1h, 1d) and tap **Add Task**.
4. Tap the checkmark icon to mark any task complete.

---

## 7. Finding Vacant Campus Rooms

1. Tap the **Free Rooms** button in the Timetable top bar.
2. Filter by **Campus** (*Kihumuro* or *Town*) — the app automatically pre-selects your campus based on your degree.
3. Filter by **Building** or set a **Minimum Study Gap** (e.g. at least 30 minutes free).
4. Tap any room to view its complete hourly schedule, floor number, and which classes occupy it throughout the day.

---

## 8. Backing Up Coursework (Cloud Sync & JSON Export)

1. Open **Settings** (or tap **Account** in the tablet/desktop sidebar).
2. Tap **Google Account & Cloud Backup → Sign In**:
   * Authenticate via the Android Credential Manager sheet.
   * Your lecture notes, assignments, custom calendar activities, and preferences sync to your private cloud storage.
3. Tap **Sync Cloud** or **Sync Now** anytime to synchronize across devices.
4. Prefer local file backups? Tap **Export Data Backup (JSON)** in **Settings → Data & Synchronization** to save an offline JSON copy (`lectures_backup_YYYYMMDD.json`) directly via the Android document picker.

---

## 9. Appearance, Notifications, Feedback & Updates

* **Appearance & Theme**:
  * Open **Settings → Appearance & Theme** (now the single unified place for styling).
  * Choose **Light**, **Dark**, or **System**, pick from 5 accent colors (*Royal Blue*, *Emerald*, *Violet*, *Amber*, *Ocean Teal*), and preview changes live before returning.
* **Reminders & Notification Delivery**:
  * Open **Settings → Notifications & Alarms → Reminder Timing & Sound** to customize pre-lecture and task lead times, toggle the 7:30 AM Morning Schedule Digest, or jump to Android notification channel settings.
* **Anonymous Feedback**:
  * Open **Settings → Anonymous Feedback & Bugs** to report timetable/room discrepancies or suggest features anonymously (offline submissions are queued automatically for retry).
* **Checking for Releases**:
  * Open **Settings → Lectures App Releases** to check for newer versions. When an update is ready, tap to open the verified HTTPS GitHub Releases page in your browser.
* **Tablet & Desktop Navigation Shortcuts**:
  * On tablets and Chromebooks, use the leading navigation rail or collapsible sidebar, or press `Ctrl+1`–`Ctrl+4` (Timetable, Calendar, Notes, Tasks), `Ctrl+5` (Vacant Rooms), `Ctrl+6` (Account), and `Ctrl+,` (Settings).
