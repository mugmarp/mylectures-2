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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import java.io.File

/**
 * Material 3 Dialog that manages the complete in-app update lifecycle:
 * displays release notes, downloads the APK directly within the app with progress,
 * and launches native 1-tap installation without sending the user to a browser.
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
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalAppTheme.current.isDark
    val primaryColor = MaterialTheme.colorScheme.primary

    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadedApk by remember { mutableStateOf(InAppUpdateManager.getDownloadedUpdateApk(context, updateInfo)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionPrompt by remember { mutableStateOf(false) }
    var showFullChangelog by remember { mutableStateOf(false) }

    // Automatically detect when user returns from system 'Install unknown apps' settings screen
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (InAppUpdateManager.canRequestPackageInstalls(context)) {
                    showPermissionPrompt = false
                }
                if (downloadedApk == null) {
                    downloadedApk = InAppUpdateManager.getDownloadedUpdateApk(context, updateInfo)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun startDownload() {
        errorMessage = null
        isDownloading = true
        downloadProgress = 0f

        coroutineScope.launch {
            val result = InAppUpdateManager.downloadUpdate(
                context = context,
                updateInfo = updateInfo,
                onProgress = { progress -> downloadProgress = progress }
            )

            isDownloading = false
            result.onSuccess { file ->
                downloadedApk = file
                // If permission is already granted, proceed directly
                if (InAppUpdateManager.canRequestPackageInstalls(context)) {
                    InAppUpdateManager.triggerInstall(context, file)
                } else {
                    showPermissionPrompt = true
                }
            }.onFailure { error ->
                errorMessage = error.localizedMessage ?: "Failed to download update"
            }
        }
    }

    fun executeInstall() {
        val apk = downloadedApk ?: InAppUpdateManager.getDownloadedUpdateApk(context, updateInfo)
        if (apk == null || !apk.exists()) {
            startDownload()
            return
        }

        if (!InAppUpdateManager.canRequestPackageInstalls(context)) {
            showPermissionPrompt = true
            InAppUpdateManager.openInstallPermissionSettings(context)
            Toast.makeText(context, "Enable 'Allow from this source', then tap Install", Toast.LENGTH_LONG).show()
        } else {
            val installResult = InAppUpdateManager.triggerInstall(context, apk)
            installResult.onFailure { error ->
                errorMessage = "Install failed: ${error.localizedMessage}"
            }
        }
    }

    Dialog(onDismissRequest = {
        // If not mandatory, OR if mandatory but an error/offline condition occurred,
        // permit dismissing so students are NEVER locked out of their saved timetable!
        if ((!updateInfo.isMandatory || errorMessage != null) && !isDownloading) onDismiss()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                    if ((!updateInfo.isMandatory || errorMessage != null) && !isDownloading) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Body (Release highlights + progress / status banners)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Release Highlights Card
                    Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "What's New in v${updateInfo.versionName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            if (updateInfo.fileSizeBytes > 0) {
                                val sizeMb = String.format("%.1f MB", updateInfo.fileSizeBytes / (1024f * 1024f))
                                Text(
                                    text = sizeMb,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

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

                Spacer(modifier = Modifier.height(14.dp))

                // Download Progress or State Pill
                if (isDownloading) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Downloading update in-app...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
                            )
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = primaryColor,
                            trackColor = primaryColor.copy(alpha = 0.2f)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else if (downloadedApk != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF22C55E).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Update downloaded & verified. Ready to install.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Error Banner if download failed
                if (errorMessage != null) {
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
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Permission Note if user hasn't granted "Install Unknown Apps"
                if (showPermissionPrompt && !InAppUpdateManager.canRequestPackageInstalls(context)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "To update, please toggle 'Allow from this source' in Settings, then return to install.",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons (Pinned at bottom, always visible & accessible)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (downloadedApk != null) {
                        Button(
                            onClick = { executeInstall() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install & Restart", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else if (!isDownloading) {
                        Button(
                            onClick = { startDownload() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (errorMessage != null) "Retry Download" else "Download & Update",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (!updateInfo.isMandatory && !isDownloading) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remind Me Later", fontSize = 13.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                        }
                    } else if (updateInfo.isMandatory && errorMessage != null && !isDownloading) {
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
