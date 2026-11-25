package com.lectures.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.R
import com.lectures.TimetableApplication
import com.lectures.features.timetable.ui.*
import com.lectures.ui.LocalAppTheme
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicProfileSheet(
    programme: String = "",
    enrolledProgramme: String = "",
    isPeeking: Boolean = false,
    academicYear: String = "",
    semester: String = "",
    onDismiss: () -> Unit,
    onPeekProgramme: (String) -> Unit = {},
    onResetToEnrolled: () -> Unit = {},
    onSetOfficialProgramme: (String) -> Unit = {},
    onReconfigureProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNavigateToTimetable: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository

    val savedProg by (repository?.programmePref ?: flowOf(null))
        .collectAsState(initial = repository?.getSavedProgramme() ?: repository?.getInitialProgramme())
    val effectiveEnrolled = enrolledProgramme.ifBlank { savedProg ?: repository?.getInitialProgramme() ?: "" }
    val effectiveProgramme = programme.ifBlank { effectiveEnrolled }

    val savedYear by (repository?.academicYearPref ?: flowOf("2026/2027"))
        .collectAsState(initial = repository?.getAcademicYear() ?: "2026/2027")
    val savedSem by (repository?.semesterPref ?: flowOf("Semester 1"))
        .collectAsState(initial = repository?.getSemester() ?: "Semester 1")
    val currentTimetableVersion by (repository?.timetableVersion ?: flowOf("Final Timetable"))
        .collectAsState(initial = "Final Timetable")
    val currentTimetableStatus by (repository?.timetableStatus ?: flowOf("Official Final"))
        .collectAsState(initial = "Official Final")
    val dbMetadata by (repository?.timetableMetadata ?: flowOf(null))
        .collectAsState(initial = null)

    var showChangeProgrammeDialog by remember { mutableStateOf(false) }

    val effectiveAcademicYear = academicYear.ifBlank { dbMetadata?.academicYear ?: savedYear }
    val effectiveSemester = semester.ifBlank { dbMetadata?.semester ?: savedSem }
    val academicWeek = remember { com.lectures.core.util.TimeUtil.currentAcademicWeek() }

    val versionLabel = dbMetadata?.versionLabel ?: currentTimetableVersion
    val versionStatus = dbMetadata?.status ?: currentTimetableStatus
    val isFinal = dbMetadata?.isFinal == true || versionLabel.contains("Final", ignoreCase = true)
    val versionDisplay = dbMetadata?.displaySummary ?: if (isFinal) {
        "$versionLabel · Official Release"
    } else {
        "$versionLabel · Synced ($versionStatus)"
    }

    val campusName = remember(effectiveProgramme) {
        val progCode = effectiveProgramme.substringBefore(" ").trim().uppercase(Locale.ROOT)
        when (progCode) {
            "MBR", "PHA", "BNS", "MLS", "BSP", "PHS", "DCM", "DEM", "DCAM" -> "Town Campus (MUST Hospital)"
            "EEE", "BME", "PEEM", "CVE", "MIE", "BCS", "BIT", "BSE" -> "Kihumuro Campus"
            "BS", "DLT" -> "Kihumuro Campus"
            "BPCD", "BGWH" -> "Town Campus"
            else -> "Kihumuro Campus"
        }
    }
    val appTheme = LocalAppTheme.current
    val isDark = appTheme.isDark
    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val textColor = if (isDark) Color(0xFFF1F5F9) else TextPrimaryLight
    val textSub = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val cardBg = if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) DarkBorderSubtle else BorderSubtleLight

    AdaptiveSheetOrDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        desktopMaxWidth = 600.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            // Top Header: Avatar & University
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFFF1F5F9) else Color.White)
                        .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = "Academic Emblem",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Academic Info",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Mbarara University of Science & Tech",
                        fontSize = 13.sp,
                        color = textSub,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // If Previewing, show prominent Preview Mode banner
            if (isPeeking) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF291B00) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFFB45309) else Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PREVIEW MODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
                            ) {
                                Text(
                                    text = "Temporary",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Previewing $effectiveProgramme",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF78350F)
                        )
                        Text(
                            text = if (effectiveEnrolled.isNotBlank()) "Viewing temporarily. Your alarms remain set for $effectiveEnrolled." else "Viewing temporarily.",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onResetToEnrolled()
                                onDismiss()
                                onNavigateToTimetable?.invoke()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFFB45309) else Color(0xFFD97706),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Exit Preview", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Current Programme Identity Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ENROLLED PROGRAMME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(StatusGreenLive.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (effectiveEnrolled.isNotBlank()) "Active" else "Unset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreenLive
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (effectiveEnrolled.isNotBlank()) "$effectiveEnrolled $effectiveAcademicYear" else "No Programme Selected",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = cardBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    ProfileItemRow(icon = Icons.Outlined.LocationOn, label = "Campus", value = campusName, isDark = isDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileItemRow(icon = Icons.Outlined.CalendarMonth, label = "Semester", value = "$effectiveSemester • $academicWeek", isDark = isDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileItemRow(icon = Icons.Outlined.AccessTime, label = "Timetable Version", value = versionDisplay, isDark = isDark)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Switch / Reconfigure Programme
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .desktopHoverable()
                    .clickable { showChangeProgrammeDialog = true }
                    .testTag("academic_sheet_change_programme_btn")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(accentColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Change Programme / Class Group",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Text(
                            text = "Select a different programme or class group",
                            fontSize = 12.sp,
                            color = textSub
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Switch",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Open Settings
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .desktopHoverable()
                    .clickable {
                        onDismiss()
                        onOpenSettings()
                    }
                    .testTag("academic_sheet_settings_btn")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings & Appearance",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Text(
                            text = "Theme, alerts & sync controls",
                            fontSize = 12.sp,
                            color = textSub
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Open",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showChangeProgrammeDialog) {
        QuickChangeProgrammeDialog(
            viewingProgramme = effectiveProgramme,
            enrolledProgramme = effectiveEnrolled,
            isPeeking = isPeeking,
            isDark = isDark,
            onDismiss = { showChangeProgrammeDialog = false },
            onPeekSelected = { peekGroup ->
                showChangeProgrammeDialog = false
                onDismiss()
                onPeekProgramme(peekGroup)
                onNavigateToTimetable?.invoke()
            },
            onProgrammeSelected = { newGroup ->
                showChangeProgrammeDialog = false
                onDismiss()
                onSetOfficialProgramme(newGroup)
                onNavigateToTimetable?.invoke()
            },
            onOpenFullReconfigure = {
                showChangeProgrammeDialog = false
                onDismiss()
                onReconfigureProfile()
            }
        )
    }
}

@Composable
private fun ProfileItemRow(
    icon: ImageVector,
    label: String,
    value: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDark) Color(0xFF94A3B8) else TextMutedLight,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                maxLines = 1,
                color = if (isDark) Color(0xFF94A3B8) else TextMutedLight
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color.White else TextPrimaryLight,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
