package com.lectures.core.update

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lectures.BuildConfig
import com.lectures.ui.LocalAppTheme

/**
 * Material 3 Dialog displaying verified remote release notes and routing users
 * cleanly to the official HTTPS release page via Intent.ACTION_VIEW.
 */
@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    // Guard against showing a downgrade or older build
    val isEligibleUpdate = remember(updateInfo) {
        InAppUpdateManager.isNewerRelease(
            remoteVersionName = updateInfo.versionName,
            remoteVersionCode = updateInfo.versionCode,
            localVersionName = BuildConfig.VERSION_NAME,
            localVersionCode = BuildConfig.VERSION_CODE
        )
    }

    if (!isEligibleUpdate) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    val context = LocalContext.current
    val isDark = LocalAppTheme.current.isDark
    val primaryColor = MaterialTheme.colorScheme.primary

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showFullChangelog by rememberSaveable { mutableStateOf(false) }

    Dialog(onDismissRequest = {
        if (!updateInfo.isMandatory || errorMessage != null) onDismiss()
    }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header (Pinned)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(primaryColor.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "App Update Available",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "v${updateInfo.versionName} (Current: v${BuildConfig.VERSION_NAME})",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    if (!updateInfo.isMandatory || errorMessage != null) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Body (Release highlights + error banners)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "What's New in v${updateInfo.versionName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val highlights = remember(updateInfo.releaseNotes) {
                                cleanReleaseHighlights(updateInfo.releaseNotes)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                highlights.take(if (showFullChangelog) highlights.size else 4).forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .size(5.dp)
                                                .background(primaryColor, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item,
                                            fontSize = 12.5.sp,
                                            lineHeight = 17.sp,
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                                        )
                                    }
                                }

                                if (highlights.size > 4) {
                                    Text(
                                        text = if (showFullChangelog) "Show less" else "Show all (${highlights.size} highlights)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showFullChangelog = !showFullChangelog }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            errorMessage = null
                            InAppUpdateManager.openUpdatePage(context, updateInfo)
                                .onFailure { err ->
                                    errorMessage = err.localizedMessage ?: "Unable to open update page"
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Update Release", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    if (!updateInfo.isMandatory) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remind Me Later", fontSize = 13.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                        }
                    } else if (errorMessage != null) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Continue to Offline Timetable",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun cleanReleaseHighlights(raw: String): List<String> {
    if (raw.isBlank()) return listOf("Schedule updates and performance enhancements.")
    val items = mutableListOf<String>()

    val technicalKeywords = listOf(
        "prepackaged database", "inserts 1,304", "recompiles", "regular expression",
        "sha-256", "checksum", "signed with", "certificate", "download the apk",
        "install from unknown", "package:", "commitish"
    )

    for (line in raw.lines()) {
        val trimmed = line.trim()
        if (trimmed.startsWith("#") || trimmed.startsWith("```") || trimmed.isBlank()) continue

        if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
            val clean = trimmed.removePrefix("-").removePrefix("*").trim()
                .replace("**", "")
                .replace("__", "")

            val isTechnical = technicalKeywords.any { clean.contains(it, ignoreCase = true) }
            if (isTechnical) continue

            if (clean.isNotBlank() && !clean.startsWith("http", ignoreCase = true)) {
                // Shorten verbose bullet points to the core statement
                val shortened = if (clean.length > 95) {
                    val firstSentence = clean.substringBefore(". ")
                    if (firstSentence.length in 20..95) firstSentence else clean.take(90).trimEnd() + "..."
                } else {
                    clean
                }
                items.add(shortened)
            }
        }
    }

    if (items.isEmpty()) {
        val fallbackLines = raw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("http", ignoreCase = true) && !it.startsWith("```") }
            .filter { line -> technicalKeywords.none { line.contains(it, ignoreCase = true) } }
            .map { it.replace("**", "").replace("__", "") }
        items.addAll(fallbackLines.take(3))
    }

    return if (items.isNotEmpty()) items.distinct().take(4) else listOf("Bug fixes and performance improvements.")
}
