package com.lectures.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lectures.features.onboarding.FACULTIES
import com.lectures.features.timetable.domain.TimetableMatcher
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion
import java.util.Locale

@Composable
fun QuickChangeProgrammeDialog(
    viewingProgramme: String,
    enrolledProgramme: String,
    isPeeking: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onPeekSelected: (String) -> Unit = {},
    onProgrammeSelected: (String) -> Unit,
    onOpenFullReconfigure: () -> Unit
) {
    val effectiveActive = viewingProgramme.ifBlank { enrolledProgramme }
    val currentProgCode = effectiveActive.substringBefore(" ").trim()

    var selectedFacultyId by remember {
        val found = FACULTIES.find { f -> f.programmes.any { it.code.equals(currentProgCode, ignoreCase = true) } }
        mutableStateOf(found?.id ?: FACULTIES.first().id)
    }

    val selectedFaculty = remember(selectedFacultyId) {
        FACULTIES.find { it.id == selectedFacultyId } ?: FACULTIES.first()
    }

    var selectedProgrammeCode by remember(selectedFacultyId) {
        val prog = selectedFaculty.programmes.find { it.code.equals(currentProgCode, ignoreCase = true) }
            ?: selectedFaculty.programmes.first()
        mutableStateOf(prog.code)
    }

    val selectedProgramme = remember(selectedFaculty, selectedProgrammeCode) {
        selectedFaculty.programmes.find { it.code == selectedProgrammeCode } ?: selectedFaculty.programmes.first()
    }

    val parsedCurrent = TimetableMatcher.parseGroupParts(effectiveActive)
    val initialYear = parsedCurrent?.year?.ifBlank { "I" } ?: "I"

    var selectedYear by remember(selectedProgramme) {
        val defaultYear = if (initialYear in selectedProgramme.defaultYears) initialYear else selectedProgramme.defaultYears.firstOrNull() ?: "I"
        mutableStateOf(defaultYear)
    }

    val tracksForYear = remember(selectedProgramme, selectedYear) {
        selectedProgramme.tracks[selectedYear] ?: emptyList()
    }

    var selectedTrack by remember(selectedProgramme, selectedYear) {
        val found = tracksForYear.find { track ->
            parsedCurrent != null && (
                track.equals(parsedCurrent.subject, ignoreCase = true) ||
                parsedCurrent.subject.contains(track, ignoreCase = true) ||
                track.contains(parsedCurrent.subject, ignoreCase = true) ||
                (parsedCurrent.subject.startsWith("BIOLOG") && track.startsWith("BIOLOG"))
            )
        }
        mutableStateOf(found ?: tracksForYear.firstOrNull() ?: "")
    }

    val selectedClassGroup = remember(selectedProgramme, selectedYear, selectedTrack) {
        if (tracksForYear.isEmpty()) {
            "${selectedProgramme.code} $selectedYear"
        } else if (selectedProgramme.code == "HEAP") {
            "${selectedProgramme.code} $selectedTrack"
        } else {
            "${selectedProgramme.code} $selectedTrack $selectedYear"
        }
    }

    var facultyDropdownExpanded by remember { mutableStateOf(false) }
    var programmeDropdownExpanded by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    val handleDismiss = {
        facultyDropdownExpanded = false
        programmeDropdownExpanded = false
        onDismiss()
    }

    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        var entered by remember { mutableStateOf(reducedMotion || !windowInfo.preferDesktopDialogs) }
        LaunchedEffect(Unit) { entered = true }
        val dialogScale by animateFloatAsState(
            targetValue = if (entered) 1f else 0.95f,
            animationSpec = if (reducedMotion || !windowInfo.preferDesktopDialogs) snap() else MotionSpec.standardSpring(),
            label = "quick_change_dialog_scale"
        )
        val dialogAlpha by animateFloatAsState(
            targetValue = if (entered) 1f else 0f,
            animationSpec = if (reducedMotion || !windowInfo.preferDesktopDialogs) snap() else tween(MotionSpec.DIALOG_ENTER_MS),
            label = "quick_change_dialog_alpha"
        )

        val dialogModifier = Modifier
            .graphicsLayer {
                scaleX = dialogScale
                scaleY = dialogScale
                alpha = dialogAlpha
            }
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .widthIn(min = 420.dp, max = 600.dp)
                        .heightIn(max = (windowInfo.heightDp - 48).coerceAtLeast(360).dp)
                        .fillMaxWidth()
                        .onEscapeKey(handleDismiss)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                }
            )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            modifier = dialogModifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Switch Programme",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select your faculty, program and year group",
                            fontSize = 12.sp,
                            color = ext.textMuted
                        )
                    }
                    IconButton(
                        onClick = handleDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Faculty Dropdown Selector
                Text(
                    text = "1. Faculty",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))

                ProfileSelectorBox(
                    expanded = facultyDropdownExpanded,
                    onExpandedChange = { expand ->
                        programmeDropdownExpanded = false
                        facultyDropdownExpanded = expand
                    },
                    isDark = isDark,
                    primaryColor = primaryColor,
                    triggerContent = {
                        Text(
                            text = selectedFaculty.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    },
                    dropdownContent = {
                        FACULTIES.forEach { faculty ->
                            val isSelected = faculty.id == selectedFacultyId
                            ProfileDropdownOptionRow(
                                isSelected = isSelected,
                                isDark = isDark,
                                primaryColor = primaryColor,
                                title = faculty.name,
                                subtitle = null,
                                onClick = {
                                    selectedFacultyId = faculty.id
                                    selectedProgrammeCode = faculty.programmes.first().code
                                    facultyDropdownExpanded = false
                                }
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Degree / Diploma Programme Dropdown Selector
                Text(
                    text = "2. Programme (${selectedFaculty.programmes.size} available)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))

                ProfileSelectorBox(
                    expanded = programmeDropdownExpanded,
                    onExpandedChange = { expand ->
                        facultyDropdownExpanded = false
                        programmeDropdownExpanded = expand
                    },
                    isDark = isDark,
                    primaryColor = primaryColor,
                    triggerContent = {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedProgramme.code} · ${selectedProgramme.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "${selectedProgramme.years} Year Curriculum",
                                fontSize = 11.sp,
                                color = ext.textMuted
                            )
                        }
                    },
                    dropdownContent = {
                        selectedFaculty.programmes.forEach { prog ->
                            val isSelected = prog.code == selectedProgrammeCode
                            ProfileDropdownOptionRow(
                                isSelected = isSelected,
                                isDark = isDark,
                                primaryColor = primaryColor,
                                title = "${prog.code} · ${prog.name}",
                                subtitle = "${prog.years} Years Curriculum",
                                onClick = {
                                    selectedProgrammeCode = prog.code
                                    programmeDropdownExpanded = false
                                }
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Year of Study & Specialization Track Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Class Group",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = primaryColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = selectedClassGroup,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Year of Study",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = ext.textMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedProgramme.defaultYears.forEach { yr ->
                        val isSelected = yr == selectedYear
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedYear = yr },
                            label = {
                                Text(
                                    text = yr,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.desktopHandCursor()
                        )
                    }
                }

                if (tracksForYear.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Subject Track / Specialization",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ext.textMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tracksForYear.forEach { trk ->
                            val isSelected = trk == selectedTrack
                            val displayLabel = when (trk.uppercase(Locale.ROOT)) {
                                "BIOLOGICAL", "BIOLOGY" -> "Biological"
                                "CHEM MATHS" -> "Chemistry - Maths"
                                "PHYSICAL" -> "Physical"
                                "MATHEMATICS" -> "Mathematics"
                                "CHEMISTRY" -> "Chemistry"
                                "PHYSICS" -> "Physics"
                                "ACCOUNTING" -> "Accounting"
                                "BANKING AND FINANCE" -> "Banking & Finance"
                                "HUMAN RESOURCE" -> "Human Resource"
                                "MARKETING" -> "Marketing"
                                "PROCUREMENT" -> "Procurement"
                                "SUSTAINABLE RURAL DEVELOPMENT" -> "Rural Development"
                                "URBAN POLICY" -> "Urban Policy"
                                "BIOLOGY-CHEMISTRY" -> "Biology - Chemistry"
                                "CHEMISTRY- MATHS" -> "Chemistry - Maths"
                                "PHYSICS-MATHS" -> "Physics - Maths"
                                else -> trk
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTrack = trk },
                                label = {
                                    Text(
                                        text = displayLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.desktopHandCursor()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val chosenGroup = selectedClassGroup
                val isDifferentFromViewing = chosenGroup.trim() != viewingProgramme.trim()
                val isDifferentFromEnrolled = chosenGroup.trim() != enrolledProgramme.trim()
                val canReturnToEnrolled = isPeeking && !isDifferentFromEnrolled

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val previewEnabled = isDifferentFromViewing || canReturnToEnrolled
                    OutlinedButton(
                        onClick = {
                            facultyDropdownExpanded = false
                            programmeDropdownExpanded = false
                            onPeekSelected(chosenGroup)
                        },
                        enabled = previewEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor(enabled = previewEnabled),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (previewEnabled) ext.warningBorder else ext.borderSubtle
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ext.warningText,
                            disabledContentColor = ext.textMuted.copy(alpha = 0.6f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                canReturnToEnrolled -> "Return to My Programme ($enrolledProgramme)"
                                isDifferentFromViewing -> "Preview $chosenGroup"
                                else -> "Currently Viewing"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            facultyDropdownExpanded = false
                            programmeDropdownExpanded = false
                            onProgrammeSelected(chosenGroup)
                        },
                        enabled = isDifferentFromEnrolled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor(enabled = isDifferentFromEnrolled),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = ext.surfaceBase,
                            disabledContentColor = ext.textMuted.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = if (isDifferentFromEnrolled) "Set as My Programme" else "Enrolled Programme",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = {
                        facultyDropdownExpanded = false
                        programmeDropdownExpanded = false
                        onOpenFullReconfigure()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .desktopHandCursor()
                ) {
                    Text("Open Full Onboarding / Reconfigure", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileSelectorBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isDark: Boolean,
    primaryColor: Color,
    triggerContent: @Composable RowScope.() -> Unit,
    dropdownContent: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalExtendedColors.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val triggerWidth = maxWidth
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ext.surfaceBase,
            border = BorderStroke(1.dp, ext.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .desktopHoverable()
                .clickable { onExpandedChange(!expanded) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                triggerContent()
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select option",
                    tint = primaryColor
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .width(triggerWidth)
                .heightIn(max = 240.dp)
                .background(ext.surfaceCard, RoundedCornerShape(12.dp))
                .border(1.dp, ext.borderSubtle, RoundedCornerShape(12.dp))
        ) {
            dropdownContent()
        }
    }
}

@Composable
private fun ProfileDropdownOptionRow(
    isSelected: Boolean,
    isDark: Boolean,
    primaryColor: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    val ext = LocalExtendedColors.current
    DropdownMenuItem(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .desktopHandCursor()
            .background(
                if (isSelected) (if (isDark) primaryColor.copy(alpha = 0.28f) else primaryColor.copy(alpha = 0.16f))
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) primaryColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = if (isSelected) primaryColor.copy(alpha = 0.85f) else ext.textMuted
                        )
                    }
                }
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = primaryColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "Selected",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        onClick = onClick
    )
}
