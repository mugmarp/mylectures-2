package com.lectures.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.onEscapeKey
import com.lectures.ui.rememberLecturesWindow

@Composable
internal fun DisclaimerDialog(onDismiss: () -> Unit) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier.widthIn(min = 420.dp, max = 560.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp).widthIn(max = 460.dp)
                }
            )
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        title = { Text("Institutional Non-Affiliation Disclaimer", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Independent Development",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lectures is an independent academic timetable and study companion designed to help students organize classes, study notes, and coursework tasks.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "2. No University Affiliation or Endorsement",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lectures is NOT an official application of, nor is it endorsed by, sponsored by, affiliated with, or in any way officially associated with Mbarara University of Science and Technology (MUST) or any of its constituent faculties, departments, or administrative offices.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "3. Informational & Convenience Purpose",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "All course codes, faculty designations, module names, and timetable slots are referenced solely for descriptive and schedule organization purposes for students. Students are advised to consult official university online portals (timetable.must.ac.ug) and administrative systems for official announcements, venue changes, and examination schedules.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "4. Academic Schedule Accuracy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Timetable data is maintained offline and locally on your device. Students should verify critical announcements and examinations with their respective departments.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
                    .testTag("dialog_disclaimer_confirm_btn")
            ) {
                Text("I Understand")
            }
        }
    )
}

@Composable
internal fun TermsDialog(onDismiss: () -> Unit) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier.widthIn(min = 420.dp, max = 560.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp).widthIn(max = 460.dp)
                }
            )
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        title = { Text("Disclaimer & Terms", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Permitted Use",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lectures is provided for personal, non-commercial academic management and learning assistance. Users agree to use the application lawfully and ethically.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "2. 'As Is' Disclaimer of Warranty",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "The software is provided on an 'AS IS' basis without warranties of any kind. While every effort is made to maintain schedule accuracy, the developer is not responsible for missed classes, test clashes, device alarm failures, or timetable discrepancies.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "3. Developer Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Developed by MUGENDAWALA MARK PAUL as an independent student tool for academic schedule tracking, vacant study hall finding, and offline coursework productivity.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "4. User Data & Notes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "All notes, summaries, checklists, custom events, and task entries are authored by the student. In Guest mode they remain on the local device; if you optionally connect a Google Account, they also sync to your private cloud workspace.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "5. Local-First Storage & Optional Cloud Sync",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Coursework, personal lecture notes, tasks, and preferences are stored locally in SQLite so the app works offline. When you sign in with Google, your records are encrypted in transit and backed up to your Google Cloud Firestore account for cross-device recovery.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
                    .testTag("dialog_terms_confirm_btn")
            ) {
                Text("I Understand")
            }
        }
    )
}

@Composable
internal fun PrivacyDialog(onDismiss: () -> Unit) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier.widthIn(min = 420.dp, max = 560.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp).widthIn(max = 460.dp)
                }
            )
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Local-First Storage & Optional Google Cloud Sync",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lectures works offline in Guest mode with your timetable schedules, lecture notes, custom events, tasks, and alarms stored locally in SQLite. If you choose to sign in with Google, Firebase Authentication links your Google name, email, and user ID so your notes, tasks, events, and app preferences can back up and sync privately via Cloud Firestore.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "2. Pseudonymous Diagnostics & Anonymous Feedback",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "To monitor timetable coverage and diagnose device compatibility, the app transmits pseudonymous diagnostics (an installation UUID, device model, Android OS version, launch count, and enrolled academic programme). Voluntary bug reports submitted through Anonymous Feedback are stored separately with your installation UUID and optional diagnostics without attaching your name or email. We never sell or monetize personal data or lecture notes.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "3. Sign-Out & Permissions Usage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Signing out clears synced personal records from this device and reverts the app to Guest mode. Notification and exact alarm permissions are requested solely to deliver local alerts before scheduled lectures and task deadlines.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
                    .testTag("dialog_privacy_confirm_btn")
            ) {
                Text("Done")
            }
        }
    )
}
