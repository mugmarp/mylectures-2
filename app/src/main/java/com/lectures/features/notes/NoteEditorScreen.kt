package com.lectures.features.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.TimetableApplication
import com.lectures.core.storage.AttachmentStore
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.ui.Chrome
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.AcademicProfileSheet
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.rememberLecturesWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NUMBERED_LIST_LINE_REGEX = Regex("^(\\d+)\\.\\s*")
private val WEEKDAY_IN_KEY_REGEX = Regex("(Mon|Tue|Wed|Thu|Fri|Sat|Sun)", RegexOption.IGNORE_CASE)

enum class NoteEditorTab {
    WRITE,
    SPLIT,
    PREVIEW
}

private enum class DesktopInspectorDrawer {
    DETAILS,
    TEMPLATES,
    GUIDE
}

private fun continueMarkdownListOnNewline(oldText: String, newVal: TextFieldValue): TextFieldValue {
    val newText = newVal.text
    if (newText.length == oldText.length + 1 && newVal.selection.start == newVal.selection.end) {
        val cursor = newVal.selection.start
        if (cursor > 0 && newText[cursor - 1] == '\n') {
            val textBeforeNewline = newText.substring(0, cursor - 1)
            val lastLine = textBeforeNewline.substringAfterLast('\n')
            val trimmedLastLine = lastLine.trim()
            when {
                trimmedLastLine in listOf("- [ ]", "- [x]", "- [X]") || trimmedLastLine in listOf("-", "*", "•") -> {
                    val prefixStart = cursor - 1 - lastLine.length
                    val cleanedText = newText.substring(0, prefixStart) + "\n" + newText.substring(cursor)
                    return TextFieldValue(cleanedText, TextRange(prefixStart + 1))
                }
                lastLine.startsWith("- [ ] ") || lastLine.startsWith("- [x] ") || lastLine.startsWith("- [X] ") -> {
                    val continuation = lastLine.takeWhile { it == ' ' || it == '\t' } + "- [ ] "
                    val autoText = newText.substring(0, cursor) + continuation + newText.substring(cursor)
                    return TextFieldValue(autoText, TextRange(cursor + continuation.length))
                }
                lastLine.startsWith("- ") || lastLine.startsWith("* ") -> {
                    val bulletChar = if (lastLine.startsWith("- ")) "- " else "* "
                    val continuation = lastLine.takeWhile { it == ' ' || it == '\t' } + bulletChar
                    val autoText = newText.substring(0, cursor) + continuation + newText.substring(cursor)
                    return TextFieldValue(autoText, TextRange(cursor + continuation.length))
                }
                NUMBERED_LIST_LINE_REGEX.find(lastLine) != null -> {
                    val num = NUMBERED_LIST_LINE_REGEX.find(lastLine)?.groupValues?.get(1)?.toIntOrNull()
                    if (trimmedLastLine == "${num}.") {
                        val prefixStart = cursor - 1 - lastLine.length
                        val cleanedText = newText.substring(0, prefixStart) + "\n" + newText.substring(cursor)
                        return TextFieldValue(cleanedText, TextRange(prefixStart + 1))
                    } else if (num != null) {
                        val indent = lastLine.takeWhile { it == ' ' || it == '\t' }
                        val continuation = "$indent${num + 1}. "
                        val autoText = newText.substring(0, cursor) + continuation + newText.substring(cursor)
                        return TextFieldValue(autoText, TextRange(cursor + continuation.length))
                    }
                }
                lastLine.startsWith("> ") || lastLine.startsWith(">") -> {
                    val continuation = "> "
                    val autoText = newText.substring(0, cursor) + continuation + newText.substring(cursor)
                    return TextFieldValue(autoText, TextRange(cursor + continuation.length))
                }
            }
        }
    }
    return newVal
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    initialNote: LectureNote? = null,
    initialTab: NoteEditorTab = NoteEditorTab.WRITE,
    availableCourses: List<CourseOption>,
    timetableClasses: List<TimetableEntry>,
    onSettingsClick: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {},
    onNavigateToTimetable: (() -> Unit)? = null,
    onPeekProgramme: ((String) -> Unit)? = null,
    onSetOfficialProgramme: ((String) -> Unit)? = null,
    onAttachmentError: ((String) -> Unit)? = null,
    onWikiLinkClick: ((String) -> Unit)? = null,
    onSave: (
        naturalKey: String?,
        courseCode: String,
        title: String,
        content: String,
        alarmMinutes: Int?,
        tag: String?,
        attachedClass: String?,
        attachmentName: String?,
        attachmentPath: String?
    ) -> Unit,
    onCancel: () -> Unit
) {
    val noteKey = initialNote?.naturalKey ?: "__new__"
    val initialCourse = initialNote?.naturalKey?.split("|")?.firstOrNull()
        ?: availableCourses.firstOrNull()?.code
        ?: "GEN101"
    val initialTitleVal = initialNote?.title ?: ""
    val initialContentVal = initialNote?.content ?: ""
    val initialAlarm = initialNote?.alarmMinutes
    val initialTag = initialNote?.tag ?: "Lecture"
    val initialClassText = initialNote?.attachedClass ?: ""
    val initialAttachment = initialNote?.attachmentName ?: ""
    val initialAttachmentPath = initialNote?.attachmentPath

    val matchedInitialClass = remember(noteKey, timetableClasses) {
        if (initialNote == null) null
        else timetableClasses.firstOrNull { it.naturalKey == initialNote.naturalKey }
            ?: initialNote.attachedClass?.takeIf { it.isNotBlank() }?.let { attached ->
                val codePart = attached.substringBefore(":").trim()
                val timePart = attached.substringAfter("·", "").trim()
                timetableClasses.firstOrNull { entry ->
                    entry.courseCode.equals(codePart, ignoreCase = true) &&
                        (timePart.isBlank() || (attached.contains(entry.day, ignoreCase = true) && attached.contains(entry.startTime)))
                }
            }
    }
    val initialLocation = matchedInitialClass?.room?.trim()
        ?.takeIf { it.isNotBlank() && it.uppercase(Locale.ROOT) !in listOf("TBA", "TBD", "NONE", "N/A") }
        ?.let { "📍 $it" } ?: ""
    val initialLecturer = matchedInitialClass?.lecturer?.trim().orEmpty()

    var selectedCourse by rememberSaveable(noteKey) { mutableStateOf(initialCourse) }
    var title by rememberSaveable(noteKey) { mutableStateOf(initialTitleVal) }
    var contentValue by rememberSaveable(noteKey, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = initialContentVal,
                selection = TextRange(initialContentVal.length)
            )
        )
    }
    var selectedAlarmMinutes by rememberSaveable(noteKey) { mutableStateOf<Int?>(initialAlarm) }
    var tag by rememberSaveable(noteKey) { mutableStateOf(initialTag) }
    var attachedClassText by rememberSaveable(noteKey) { mutableStateOf(initialClassText) }
    var attachedLocation by rememberSaveable(noteKey) { mutableStateOf(initialLocation) }
    var lecturer by rememberSaveable(noteKey) { mutableStateOf(initialLecturer) }
    var attachmentName by rememberSaveable(noteKey) { mutableStateOf(initialAttachment) }
    var attachmentPath by rememberSaveable(noteKey) { mutableStateOf(initialAttachmentPath) }
    val windowInfo = rememberLecturesWindow()
    val useDesktopSplitWorkspace = windowInfo.isDualPane
    var currentTab by rememberSaveable(noteKey, initialTab) {
        mutableStateOf(if (useDesktopSplitWorkspace) NoteEditorTab.SPLIT else initialTab)
    }
    var desktopDrawer by rememberSaveable(noteKey) { mutableStateOf(DesktopInspectorDrawer.DETAILS) }
    var showDesktopInspector by rememberSaveable(noteKey) { mutableStateOf(windowInfo.isWideWorkspace) }
    val editorFocusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val isDark = LocalAppTheme.current.isDark
    val primaryColor = MaterialTheme.colorScheme.primary
    val coroutineScope = rememberCoroutineScope()
    var isCopyingAttachment by remember { mutableStateOf(false) }
    val stagedNewPaths = remember(noteKey) { mutableStateListOf<String>() }

    val cleanupOrphanedAttachments: (keepPath: String?) -> Unit = { keepPath ->
        val candidates = (stagedNewPaths + listOfNotNull(initialAttachmentPath)).distinct()
            .filter { it != keepPath }
        if (candidates.isNotEmpty()) {
            val appContext = context.applicationContext
            val cleanupScope = (appContext as? TimetableApplication)?.applicationScope
                ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
            cleanupScope.launch(Dispatchers.IO) {
                candidates.forEach { AttachmentStore.delete(appContext, it) }
            }
        }
    }

    val discardAndCancel: () -> Unit = {
        cleanupOrphanedAttachments(initialAttachmentPath)
        onCancel()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fallback = "Whiteboard_${System.currentTimeMillis() % 10000}.jpg"
            isCopyingAttachment = true
            coroutineScope.launch {
                val (displayName, copiedPath) = withContext(Dispatchers.IO) {
                    val name = AttachmentStore.queryDisplayName(context, uri) ?: fallback
                    val path = AttachmentStore.copyIntoPrivateStorage(context, uri, fallback)
                    name to path
                }
                isCopyingAttachment = false
                if (copiedPath != null) {
                    attachmentName = displayName
                    attachmentPath = copiedPath
                    stagedNewPaths.add(copiedPath)
                } else {
                    onAttachmentError?.invoke("Could not read the selected photo")
                }
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fallback = "Slides_${selectedCourse.ifBlank { "Lecture" }}.pdf"
            isCopyingAttachment = true
            coroutineScope.launch {
                val (displayName, copiedPath) = withContext(Dispatchers.IO) {
                    val name = AttachmentStore.queryDisplayName(context, uri) ?: fallback
                    val path = AttachmentStore.copyIntoPrivateStorage(context, uri, fallback)
                    name to path
                }
                isCopyingAttachment = false
                if (copiedPath != null) {
                    attachmentName = displayName
                    attachmentPath = copiedPath
                    stagedNewPaths.add(copiedPath)
                } else {
                    onAttachmentError?.invoke("Could not read the selected file")
                }
            }
        }
    }

    var showClassPicker by rememberSaveable(noteKey) { mutableStateOf(false) }
    var showCoursePickerModal by rememberSaveable(noteKey) { mutableStateOf(false) }
    var showTagPicker by rememberSaveable(noteKey) { mutableStateOf(false) }
    var showTemplatesSheet by rememberSaveable(noteKey) { mutableStateOf(false) }
    var pendingTemplate by remember { mutableStateOf<NoteTemplate?>(null) }
    var pendingWikiLink by remember { mutableStateOf<String?>(null) }
    var showHelpSheet by rememberSaveable(noteKey) { mutableStateOf(false) }
    var showDiscardConfirmDialog by rememberSaveable(noteKey) { mutableStateOf(false) }
    var showProfileSheet by rememberSaveable(noteKey) { mutableStateOf(false) }
    var showCustomReminderDialog by rememberSaveable(noteKey) { mutableStateOf(false) }

    val canScheduleClassAlert = attachedClassText.isNotBlank() ||
        WEEKDAY_IN_KEY_REGEX.containsMatchIn(initialNote?.naturalKey.orEmpty())

    val isModified = selectedCourse != initialCourse ||
        title != initialTitleVal ||
        contentValue.text != initialContentVal ||
        selectedAlarmMinutes != initialAlarm ||
        tag != initialTag ||
        attachedClassText != initialClassText ||
        attachmentName != initialAttachment ||
        attachmentPath != initialAttachmentPath

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val handleBackPress: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        if (isModified) {
            showDiscardConfirmDialog = true
        } else {
            discardAndCancel()
        }
    }

    BackHandler(enabled = true, onBack = handleBackPress)

    val applyMarkdown: (String, String, String) -> Unit = { prefix, suffix, defaultPlaceholder ->
        val text = contentValue.text
        val selection = contentValue.selection
        if (selection.start != selection.end) {
            val selectedText = text.substring(selection.start, selection.end)
            val newText = text.substring(0, selection.start) + prefix + selectedText + suffix + text.substring(selection.end)
            val newCursor = selection.end + prefix.length + suffix.length
            contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
        } else {
            val newText = text.substring(0, selection.start) + prefix + defaultPlaceholder + suffix + text.substring(selection.start)
            val newStart = selection.start + prefix.length
            val newEnd = newStart + defaultPlaceholder.length
            contentValue = TextFieldValue(text = newText, selection = TextRange(newStart, newEnd))
        }
    }

    val insertLinePrefix: (String) -> Unit = { linePrefix ->
        val text = contentValue.text
        val insertPos = contentValue.selection.start
        val needsLeadingNewline = insertPos > 0 && text[insertPos - 1] != '\n'
        val insertion = if (needsLeadingNewline) "\n$linePrefix" else linePrefix
        val newText = text.substring(0, insertPos) + insertion + text.substring(insertPos)
        contentValue = TextFieldValue(text = newText, selection = TextRange(insertPos + insertion.length))
    }

    val insertSnippet: (String) -> Unit = { snippet ->
        val text = contentValue.text
        val insertPos = contentValue.selection.start
        val needsLeadingNewline = insertPos > 0 && text[insertPos - 1] != '\n' && !snippet.startsWith("\n")
        val insertion = if (needsLeadingNewline) "\n$snippet" else snippet
        val newText = text.substring(0, insertPos) + insertion + text.substring(insertPos)
        contentValue = TextFieldValue(text = newText, selection = TextRange(insertPos + insertion.length))
    }

    val insertDateStamp: () -> Unit = {
        val formatter = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        insertSnippet("📅 **${formatter.format(Date())}**\n")
    }

    val toggleChecklistInPreview: (Int) -> Unit = { lineIndex ->
        contentValue = contentValue.copy(text = MarkdownUtils.toggleChecklistAt(contentValue.text, lineIndex))
    }

    val wordsCount = remember(contentValue.text) {
        contentValue.text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }
    val charsCount = remember(contentValue.text) { contentValue.text.length }
    val checklistProgress = remember(contentValue.text) {
        MarkdownUtils.countChecklistProgress(contentValue.text)
    }

    val scrollState = rememberScrollState()

    if (showDiscardConfirmDialog) {
        DiscardNoteConfirmDialog(
            onDiscard = {
                showDiscardConfirmDialog = false
                discardAndCancel()
            },
            onDismiss = { showDiscardConfirmDialog = false }
        )
    }

    if (showCoursePickerModal) {
        NoteCoursePickerDialog(
            selectedCourse = selectedCourse,
            availableCourses = availableCourses,
            onSelectCourse = { selectedCourse = it },
            onDismiss = { showCoursePickerModal = false }
        )
    }

    if (showTagPicker) {
        NoteTagPickerDialog(
            currentTag = tag,
            onSelectTag = { tag = it },
            onDismiss = { showTagPicker = false }
        )
    }

    if (showClassPicker) {
        NoteClassPickerDialog(
            attachedClassText = attachedClassText,
            selectedCourse = selectedCourse,
            timetableClasses = timetableClasses,
            onDetachClass = {
                attachedClassText = ""
                attachedLocation = ""
                lecturer = ""
                if (!WEEKDAY_IN_KEY_REGEX.containsMatchIn(initialNote?.naturalKey.orEmpty())) {
                    selectedAlarmMinutes = null
                }
            },
            onAttachClass = { classText, locText, lectName ->
                attachedClassText = classText
                attachedLocation = locText
                lecturer = lectName
            },
            onDismiss = { showClassPicker = false }
        )
    }

    val applyTemplate: (NoteTemplate) -> Unit = { tpl ->
        if (title.isBlank() || title == "New Note") {
            title = tpl.defaultTitle.replace("[Course Code]", selectedCourse)
        }
        tag = tpl.tag
        val topicVal = title.takeIf { it.isNotBlank() && !it.startsWith("Lecture:") } ?: selectedCourse
        val lecturerVal = lecturer.ifBlank { "Lecturer" }
        val roomVal = attachedLocation.removePrefix("📍").trim().ifBlank { "Lecture Hall" }
        val filledContent = tpl.content
            .replace("[Course Code]", selectedCourse)
            .replace("[Topic / Unit Title]", topicVal)
            .replace("[Topic / Unit]", topicVal)
            .replace("[Lecturer Name]", lecturerVal)
            .replace("[Lecture Room]", roomVal)
            .replace("[Date]", SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()))
        contentValue = TextFieldValue(text = filledContent, selection = TextRange(filledContent.length))
    }

    if (showTemplatesSheet) {
        NoteTemplatesBottomSheet(
            onDismiss = { showTemplatesSheet = false },
            onSelectTemplate = { tpl ->
                if (contentValue.text.isBlank()) applyTemplate(tpl) else pendingTemplate = tpl
            }
        )
    }

    if (pendingTemplate != null) {
        val targetTemplate = pendingTemplate!!
        NoteTemplateConfirmDialog(
            targetTemplate = targetTemplate,
            onConfirmReplace = {
                applyTemplate(targetTemplate)
                pendingTemplate = null
            },
            onDismiss = { pendingTemplate = null }
        )
    }

    if (showHelpSheet) {
        MarkdownHelpBottomSheet(
            onDismiss = { showHelpSheet = false },
            onInsertSnippet = insertSnippet
        )
    }

    val saveNoteAction: () -> Unit = {
        if (!isCopyingAttachment) {
            focusManager.clearFocus()
            keyboardController?.hide()
            val finalAttachmentPath = attachmentPath
            onSave(
                initialNote?.naturalKey,
                selectedCourse,
                title.ifBlank { "Academic Note: $selectedCourse" },
                contentValue.text,
                if (canScheduleClassAlert) selectedAlarmMinutes else null,
                tag,
                attachedClassText,
                attachmentName,
                finalAttachmentPath
            )
            cleanupOrphanedAttachments(finalAttachmentPath)
        }
    }

    val handleWikiLinkRequest: (String) -> Unit = { linkTitle ->
        if (isModified) {
            pendingWikiLink = linkTitle
        } else {
            cleanupOrphanedAttachments(initialAttachmentPath)
            onWikiLinkClick?.invoke(linkTitle)
        }
    }

    if (pendingWikiLink != null) {
        val targetLink = pendingWikiLink!!
        UnsavedWikiLinkNavigationDialog(
            targetTitle = targetLink,
            onSaveAndOpen = {
                pendingWikiLink = null
                saveNoteAction()
                onWikiLinkClick?.invoke(targetLink)
            },
            onDiscardAndOpen = {
                pendingWikiLink = null
                cleanupOrphanedAttachments(initialAttachmentPath)
                onWikiLinkClick?.invoke(targetLink)
            },
            onDismiss = { pendingWikiLink = null }
        )
    }

    val effectiveTab = if (!useDesktopSplitWorkspace && currentTab == NoteEditorTab.SPLIT) NoteEditorTab.WRITE else currentTab

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                val cmd = event.isCtrlPressed || event.isMetaPressed
                val canEditText = effectiveTab != NoteEditorTab.PREVIEW
                when {
                    cmd && event.key == Key.S -> {
                        saveNoteAction()
                        true
                    }
                    cmd && canEditText && event.key == Key.B -> {
                        applyMarkdown("**", "**", "Bold text")
                        true
                    }
                    cmd && canEditText && event.key == Key.I -> {
                        applyMarkdown("*", "*", "Italic text")
                        true
                    }
                    cmd && canEditText && event.key == Key.K -> {
                        applyMarkdown("[[", "]]", "Course Note")
                        true
                    }
                    cmd && event.key == Key.Slash -> {
                        if (useDesktopSplitWorkspace) {
                            if (showDesktopInspector && desktopDrawer == DesktopInspectorDrawer.GUIDE) {
                                desktopDrawer = DesktopInspectorDrawer.DETAILS
                            } else {
                                showDesktopInspector = true
                                desktopDrawer = DesktopInspectorDrawer.GUIDE
                            }
                        } else {
                            showHelpSheet = !showHelpSheet
                        }
                        true
                    }
                    event.key == Key.Escape -> {
                        when {
                            pendingWikiLink != null -> {
                                pendingWikiLink = null
                                true
                            }
                            useDesktopSplitWorkspace && showDesktopInspector && desktopDrawer != DesktopInspectorDrawer.DETAILS -> {
                                desktopDrawer = DesktopInspectorDrawer.DETAILS
                                true
                            }
                            !showTemplatesSheet && !showHelpSheet && !showCoursePickerModal && !showTagPicker && !showClassPicker && !showCustomReminderDialog && !showDiscardConfirmDialog -> {
                                handleBackPress()
                                true
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            } else false
        },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = Chrome.TopBarGlassAlpha),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (windowInfo.isNarrow) 8.dp else 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            IconButton(
                                onClick = handleBackPress,
                                modifier = Modifier
                                    .size(48.dp)
                                    .desktopHandCursor()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (initialNote == null) "New Note" else "Edit Note",
                                fontSize = if (windowInfo.isNarrow) 15.sp else 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isModified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                if (windowInfo.isTablet) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        modifier = Modifier.testTag("note_unsaved_badge")
                                    ) {
                                        Text(
                                            text = "Unsaved",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .testTag("note_unsaved_badge")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (windowInfo.isNarrow) 6.dp else 8.dp)
                        ) {
                            val isCompactToggle = windowInfo.isNarrow || windowInfo.isTightFont
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (useDesktopSplitWorkspace) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .desktopHandCursor()
                                                .clickable { currentTab = NoteEditorTab.SPLIT }
                                                .padding(3.dp)
                                                .testTag("note_editor_split_tab"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .defaultMinSize(minHeight = 36.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(if (effectiveTab == NoteEditorTab.SPLIT) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "Split",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (effectiveTab == NoteEditorTab.SPLIT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .desktopHandCursor()
                                            .clickable {
                                                if (!useDesktopSplitWorkspace) {
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                }
                                                currentTab = NoteEditorTab.WRITE
                                            }
                                            .padding(3.dp)
                                            .testTag("note_editor_write_tab"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 36.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(if (effectiveTab == NoteEditorTab.WRITE) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .padding(horizontal = if (isCompactToggle) 8.dp else 10.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Write",
                                                    tint = if (effectiveTab == NoteEditorTab.WRITE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                if (!isCompactToggle) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        "Write",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (effectiveTab == NoteEditorTab.WRITE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .desktopHandCursor()
                                            .clickable {
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                currentTab = NoteEditorTab.PREVIEW
                                            }
                                            .padding(3.dp)
                                            .testTag("note_editor_preview_tab"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 36.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(if (effectiveTab == NoteEditorTab.PREVIEW) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .padding(horizontal = if (isCompactToggle) 8.dp else 10.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Visibility,
                                                    contentDescription = "Preview",
                                                    tint = if (effectiveTab == NoteEditorTab.PREVIEW) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                if (!isCompactToggle) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        "Preview",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (effectiveTab == NoteEditorTab.PREVIEW) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (windowInfo.isTablet) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        if (clipboard != null) {
                                            clipboard.setPrimaryClip(ClipData.newPlainText(title.ifBlank { selectedCourse }, contentValue.text))
                                            Toast.makeText(context, "Markdown copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .desktopHandCursor()
                                        .testTag("note_editor_copy_md_btn")
                                ) {
                                    Icon(
                                        Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy Markdown",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (useDesktopSplitWorkspace) {
                                IconButton(
                                    onClick = { showDesktopInspector = !showDesktopInspector },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .desktopHandCursor()
                                        .testTag("note_editor_inspector_toggle")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.MenuOpen,
                                        contentDescription = if (showDesktopInspector) "Hide Inspector" else "Show Inspector",
                                        tint = if (showDesktopInspector) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = saveNoteAction,
                                enabled = !isCopyingAttachment,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = if (windowInfo.isNarrow) 10.dp else 14.dp, vertical = 6.dp),
                                modifier = Modifier.desktopHandCursor().testTag("note_save_button")
                            ) {
                                Text("Save ✓", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                }
            }
        }
    ) { innerPadding ->
        if (useDesktopSplitWorkspace) {
            val openUrlAction: (String) -> Unit = { rawUrl ->
                val safeUrl = if (rawUrl.startsWith("http://", ignoreCase = true) || rawUrl.startsWith("https://", ignoreCase = true)) {
                    rawUrl
                } else {
                    "https://$rawUrl"
                }
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
                } catch (_: Exception) {
                    onAttachmentError?.invoke("Unable to open link")
                }
            }
            val openAttachmentAction: () -> Unit = {
                val path = attachmentPath
                val uri = if (path != null) AttachmentStore.contentUriFor(context, path) else null
                if (uri == null) {
                    onAttachmentError?.invoke("Attachment file is missing")
                } else {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        onAttachmentError?.invoke("No app can open this file")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1520.dp)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT/CENTER EDITOR PANE
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(
                                        "Enter note title or lecture topic...",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = { showCoursePickerModal = true },
                                label = { Text(if (selectedCourse.isNotBlank()) "📚 $selectedCourse" else "📚 Select Course", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.desktopHandCursor()
                            )
                            AssistChip(
                                onClick = { showTagPicker = true },
                                label = { Text("🏷️ $tag", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.desktopHandCursor()
                            )
                            AssistChip(
                                onClick = {
                                    if (showDesktopInspector && desktopDrawer == DesktopInspectorDrawer.TEMPLATES) {
                                        desktopDrawer = DesktopInspectorDrawer.DETAILS
                                    } else {
                                        showDesktopInspector = true
                                        desktopDrawer = DesktopInspectorDrawer.TEMPLATES
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp)) },
                                label = { Text("Templates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.desktopHandCursor()
                            )
                            AssistChip(
                                onClick = {
                                    if (showDesktopInspector && desktopDrawer == DesktopInspectorDrawer.GUIDE) {
                                        desktopDrawer = DesktopInspectorDrawer.DETAILS
                                    } else {
                                        showDesktopInspector = true
                                        desktopDrawer = DesktopInspectorDrawer.GUIDE
                                    }
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                label = { Text("Guide", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.desktopHandCursor()
                            )
                            AssistChip(
                                onClick = { showClassPicker = true },
                                label = {
                                    Text(
                                        if (attachedClassText.isNotBlank()) "📍 ${attachedClassText.substringBefore(" · ").take(18)}" else "📍 Attach Class",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.desktopHandCursor()
                            )
                            AssistChip(
                                onClick = {
                                    if (canScheduleClassAlert) {
                                        showCustomReminderDialog = true
                                    } else {
                                        showClassPicker = true
                                    }
                                },
                                label = {
                                    val alertText = if (selectedAlarmMinutes != null) "⏰ ${selectedAlarmMinutes}m alert" else "⏰ Add Alert"
                                    Text(alertText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.desktopHandCursor()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                if (effectiveTab != NoteEditorTab.PREVIEW) {
                                    MarkdownFormattingToolbar(
                                        isDark = isDark,
                                        applyMarkdown = applyMarkdown,
                                        insertLinePrefix = insertLinePrefix,
                                        insertSnippet = insertSnippet,
                                        insertDateStamp = insertDateStamp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                val liveTransformation = remember(isDark, primaryColor) {
                                    MarkdownLiveVisualTransformation(isDark, primaryColor)
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    if (effectiveTab == NoteEditorTab.WRITE || effectiveTab == NoteEditorTab.SPLIT) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .verticalScroll(rememberScrollState())
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    try { editorFocusRequester.requestFocus() } catch (_: Exception) {}
                                                }
                                        ) {
                                            BasicTextField(
                                                value = contentValue,
                                                onValueChange = { contentValue = continueMarkdownListOnNewline(contentValue.text, it) },
                                                visualTransformation = liveTransformation,
                                                textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 360.dp)
                                                    .focusRequester(editorFocusRequester)
                                                    .testTag("note_content_editor"),
                                                decorationBox = { innerTextField ->
                                                    if (contentValue.text.isEmpty()) {
                                                        Text(
                                                            "Start taking lecture notes, revision points, or checklists...\n• Live Markdown highlighting & live right preview\n• Ctrl+S to save · Ctrl+B/I/K to format",
                                                            fontSize = 14.sp,
                                                            lineHeight = 22.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )
                                        }
                                    }

                                    if (effectiveTab == NoteEditorTab.SPLIT) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (effectiveTab == NoteEditorTab.PREVIEW || effectiveTab == NoteEditorTab.SPLIT) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .verticalScroll(rememberScrollState())
                                                .testTag("note_live_preview_pane")
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("LIVE PREVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                                                if (checklistProgress != null) {
                                                    val (done, total) = checklistProgress
                                                    val ext = LocalExtendedColors.current
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (done == total) ext.successBg else MaterialTheme.colorScheme.primaryContainer)
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            "☑ $done / $total",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (done == total) ext.successText else MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            MarkdownViewer(
                                                markdown = contentValue.text,
                                                onToggleChecklist = toggleChecklistInPreview,
                                                onWikiLinkClick = handleWikiLinkRequest,
                                                onUrlClick = openUrlAction
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ctrl+S Save · Ctrl+B/I/K Format · Esc Back", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("$wordsCount words · $charsCount chars", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // RIGHT DOCKED INSPECTOR PANE (Collapsible; Details / Inline Templates / Inline Formatting Guide)
                    if (showDesktopInspector) {
                        val inspectorWidth = if (windowInfo.isExtraLargeWidth) 340.dp else 300.dp
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .width(inspectorWidth)
                                .fillMaxHeight()
                                .testTag("note_editor_inspector")
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        DesktopInspectorDrawer.DETAILS to "Details",
                                        DesktopInspectorDrawer.TEMPLATES to "Templates",
                                        DesktopInspectorDrawer.GUIDE to "Guide"
                                    ).forEach { (drawerMode, label) ->
                                        val active = desktopDrawer == drawerMode
                                        FilterChip(
                                            selected = active,
                                            onClick = { desktopDrawer = drawerMode },
                                            label = {
                                                Text(
                                                    label,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.weight(1f).desktopHandCursor()
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                when (desktopDrawer) {
                                    DesktopInspectorDrawer.DETAILS -> {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .verticalScroll(rememberScrollState())
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (attachedClassText.isNotBlank()) {
                                                AttachedTimetableClassCard(
                                                    attachedClassText = attachedClassText,
                                                    attachedLocation = attachedLocation,
                                                    lecturer = lecturer,
                                                    onClick = { showClassPicker = true }
                                                )
                                            }
                                            PreClassAlertReminderCard(
                                                canScheduleClassAlert = canScheduleClassAlert,
                                                selectedAlarmMinutes = selectedAlarmMinutes,
                                                onOpenClassPicker = { showClassPicker = true },
                                                onSelectAlarmMinutes = { selectedAlarmMinutes = it },
                                                onOpenCustomDialog = { showCustomReminderDialog = true }
                                            )
                                            NoteAttachmentsSection(
                                                isCopyingAttachment = isCopyingAttachment,
                                                attachmentName = attachmentName,
                                                attachmentPath = attachmentPath,
                                                onPickDocument = {
                                                    documentPickerLauncher.launch(
                                                        arrayOf(
                                                            "application/pdf",
                                                            "application/msword",
                                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                            "application/vnd.ms-powerpoint",
                                                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                            "text/*"
                                                        )
                                                    )
                                                },
                                                onPickPhoto = {
                                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                },
                                                onOpenAttachment = openAttachmentAction,
                                                onRemoveAttachment = {
                                                    attachmentName = ""
                                                    attachmentPath = null
                                                }
                                            )
                                        }
                                    }
                                    DesktopInspectorDrawer.TEMPLATES -> {
                                        NoteTemplatesContent(
                                            onDismiss = { desktopDrawer = DesktopInspectorDrawer.DETAILS },
                                            onSelectTemplate = { tpl ->
                                                if (contentValue.text.isBlank()) applyTemplate(tpl) else pendingTemplate = tpl
                                            },
                                            modifier = Modifier.weight(1f).padding(14.dp)
                                        )
                                    }
                                    DesktopInspectorDrawer.GUIDE -> {
                                        MarkdownHelpContent(
                                            onDismiss = { desktopDrawer = DesktopInspectorDrawer.DETAILS },
                                            onInsertSnippet = insertSnippet,
                                            modifier = Modifier.weight(1f).padding(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                "Enter note title or lecture topic...",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                        innerTextField()
                    }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        AssistChip(
                            onClick = { showCoursePickerModal = true },
                            label = {
                                Text(
                                    text = if (selectedCourse.isNotBlank()) "📚 $selectedCourse" else "📚 Select Course",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showTagPicker = true },
                            label = { Text("🏷️ $tag", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showTemplatesSheet = true },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp)) },
                            label = { Text("Templates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showHelpSheet = true },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Guide", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showClassPicker = true },
                            label = {
                                val labelText = if (attachedClassText.isNotBlank()) {
                                    "📍 ${attachedClassText.substringBefore(" · ").take(18)}"
                                } else {
                                    "📍 Attach Class"
                                }
                                Text(labelText, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                if (canScheduleClassAlert) {
                                    showCustomReminderDialog = true
                                } else {
                                    showClassPicker = true
                                }
                            },
                            label = {
                                val alertText = if (selectedAlarmMinutes != null) "⏰ ${selectedAlarmMinutes}m alert" else "⏰ Add Alert"
                                Text(alertText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .let { mod ->
                                if (currentTab == NoteEditorTab.WRITE) {
                                    mod.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        try { editorFocusRequester.requestFocus() } catch (_: Exception) {}
                                    }
                                } else mod
                            }
                    ) {
                        if (currentTab == NoteEditorTab.WRITE) {
                            MarkdownFormattingToolbar(
                                isDark = isDark,
                                applyMarkdown = applyMarkdown,
                                insertLinePrefix = insertLinePrefix,
                                insertSnippet = insertSnippet,
                                insertDateStamp = insertDateStamp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val liveTransformation = remember(isDark, primaryColor) {
                                MarkdownLiveVisualTransformation(isDark, primaryColor)
                            }

                            BasicTextField(
                                value = contentValue,
                                onValueChange = { contentValue = continueMarkdownListOnNewline(contentValue.text, it) },
                                visualTransformation = liveTransformation,
                                textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp)
                                    .focusRequester(editorFocusRequester)
                                    .testTag("note_content_editor"),
                                decorationBox = { innerTextField ->
                                    if (contentValue.text.isEmpty()) {
                                        Text(
                                            "Start taking lecture notes, revision points, or checklists...\n• Live Markdown highlighting in editor\n• Use ==highlight==, > [!NOTE], and | Tables |\n• Tap 'Templates' for full academic note outlines\n• Switch to 'Preview' tab anytime for rich rendered notes",
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("RICH PREVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                                if (checklistProgress != null) {
                                    val (done, total) = checklistProgress
                                    val ext = com.lectures.ui.LocalExtendedColors.current
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (done == total) ext.successBg else MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "☑ $done / $total completed",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (done == total) ext.successText else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            MarkdownViewer(
                                markdown = contentValue.text,
                                onToggleChecklist = toggleChecklistInPreview,
                                onWikiLinkClick = handleWikiLinkRequest,
                                onUrlClick = { rawUrl ->
                                    val safeUrl = if (rawUrl.startsWith("http://", ignoreCase = true) || rawUrl.startsWith("https://", ignoreCase = true)) {
                                        rawUrl
                                    } else {
                                        "https://$rawUrl"
                                    }
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
                                    } catch (_: Exception) {
                                        onAttachmentError?.invoke("Unable to open link")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Markdown & Visual formatting", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$wordsCount words · $charsCount chars", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (attachedClassText.isNotBlank()) {
                    AttachedTimetableClassCard(
                        attachedClassText = attachedClassText,
                        attachedLocation = attachedLocation,
                        lecturer = lecturer,
                        onClick = { showClassPicker = true }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                PreClassAlertReminderCard(
                    canScheduleClassAlert = canScheduleClassAlert,
                    selectedAlarmMinutes = selectedAlarmMinutes,
                    onOpenClassPicker = { showClassPicker = true },
                    onSelectAlarmMinutes = { selectedAlarmMinutes = it },
                    onOpenCustomDialog = { showCustomReminderDialog = true }
                )

                Spacer(modifier = Modifier.height(14.dp))

                NoteAttachmentsSection(
                    isCopyingAttachment = isCopyingAttachment,
                    attachmentName = attachmentName,
                    attachmentPath = attachmentPath,
                    onPickDocument = {
                        documentPickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/vnd.ms-powerpoint",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "text/*"
                            )
                        )
                    },
                    onPickPhoto = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onOpenAttachment = {
                        val path = attachmentPath
                        val uri = if (path != null) AttachmentStore.contentUriFor(context, path) else null
                        if (uri == null) {
                            onAttachmentError?.invoke("Attachment file is missing")
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                onAttachmentError?.invoke("No app can open this file")
                            }
                        }
                    },
                    onRemoveAttachment = {
                        attachmentName = ""
                        attachmentPath = null
                    }
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
        }
    }

    if (showProfileSheet) {
        val enrolledProg = (LocalContext.current.applicationContext as? TimetableApplication)?.repository?.getInitialProgramme() ?: ""
        AcademicProfileSheet(
            programme = enrolledProg,
            enrolledProgramme = enrolledProg,
            onDismiss = { showProfileSheet = false },
            onPeekProgramme = { peekGroup ->
                showProfileSheet = false
                onPeekProgramme?.invoke(peekGroup)
                onNavigateToTimetable?.invoke()
            },
            onSetOfficialProgramme = { newGroup ->
                showProfileSheet = false
                onSetOfficialProgramme?.invoke(newGroup)
                onNavigateToTimetable?.invoke()
            },
            onReconfigureProfile = {
                showProfileSheet = false
                onReconfigureAcademicProfile()
            },
            onOpenSettings = {
                showProfileSheet = false
                onSettingsClick()
            },
            onNavigateToTimetable = {
                showProfileSheet = false
                onNavigateToTimetable?.invoke()
            }
        )
    }

    if (showCustomReminderDialog) {
        CustomPreClassAlertDialog(
            initialMinutesText = selectedAlarmMinutes?.toString() ?: "45",
            onApply = { parsed -> selectedAlarmMinutes = parsed },
            onDismiss = { showCustomReminderDialog = false }
        )
    }
}
