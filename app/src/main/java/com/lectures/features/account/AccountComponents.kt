package com.lectures.features.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseUser
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.onEscapeKey

@Composable
internal fun AccountErrorBanner(
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
        errorMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun AccountSuccessBanner(
    syncMessage: String?,
    onDismiss: () -> Unit
) {
    val ext = LocalExtendedColors.current
    AnimatedVisibility(visible = syncMessage != null, enter = fadeIn(), exit = fadeOut()) {
        syncMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ext.successBg,
                border = BorderStroke(1.dp, ext.successBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ext.successIcon)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(msg, color = ext.successText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = ext.successIcon, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun AccountGuestSignInCard(
    modifier: Modifier = Modifier,
    includeInlinePill: Boolean,
    notesCount: Int,
    tasksCount: Int,
    eventsCount: Int,
    isTightFont: Boolean,
    isAuthenticating: Boolean,
    onSignInClick: () -> Unit
) {
    val ext = LocalExtendedColors.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Cloud Backup & Sync",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Keep your notes, tasks, and coursework backed up safely.",
                fontSize = 13.sp,
                color = ext.textMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (includeInlinePill) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val stackGuestData = maxWidth < 260.dp || (isTightFont && maxWidth < 340.dp)
                        val labelRow: @Composable () -> Unit = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Local Unsynced Data",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        val countsText = "$notesCount Notes · $tasksCount Tasks · $eventsCount Events"
                        if (stackGuestData) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                labelRow()
                                Text(
                                    countsText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                labelRow()
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    countsText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSignInClick,
                enabled = !isAuthenticating,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .desktopHandCursor(enabled = !isAuthenticating)
                    .testTag("google_signin_button")
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Connecting to Google...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
internal fun AccountGuestLocalDataCard(
    modifier: Modifier = Modifier,
    notesCount: Int,
    tasksCount: Int,
    eventsCount: Int,
    isTightFont: Boolean
) {
    val ext = LocalExtendedColors.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Local Unsynced Data",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Signing in automatically preserves and merges your existing local academic records into your account.",
                fontSize = 12.sp,
                color = ext.textMuted,
                lineHeight = 17.sp
            )
            ResponsiveDashboardStatsGrid(
                notesCount = notesCount,
                tasksCount = tasksCount,
                eventsCount = eventsCount,
                isTightFont = isTightFont
            )
        }
    }
}

@Composable
internal fun AccountProfileHeader(user: FirebaseUser) {
    val ext = LocalExtendedColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        val initial = (user.displayName?.firstOrNull() ?: user.email?.firstOrNull() ?: 'U').uppercaseChar()
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName ?: "Student Account",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = user.email ?: user.uid,
                fontSize = 13.sp,
                color = ext.textMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(ext.successIcon, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Cloud Sync Active",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ext.successText
                )
            }
        }
    }
}

@Composable
internal fun AccountDataDashboardSection(
    notesCount: Int,
    tasksCount: Int,
    eventsCount: Int,
    isTightFont: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Preserved Coursework & Timetable Data",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        ResponsiveDashboardStatsGrid(
            notesCount = notesCount,
            tasksCount = tasksCount,
            eventsCount = eventsCount,
            isTightFont = isTightFont
        )
    }
}

@Composable
internal fun AccountActionButtonsSection(
    isSyncing: Boolean,
    isTightFont: Boolean,
    onSyncNowClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val syncNowButton: @Composable (Modifier) -> Unit = { btnMod ->
        Button(
            onClick = onSyncNowClick,
            enabled = !isSyncing,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = btnMod
                .defaultMinSize(minHeight = 48.dp)
                .desktopHandCursor(enabled = !isSyncing)
                .testTag("sync_now_button")
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Syncing...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    val signOutButton: @Composable (Modifier) -> Unit = { btnMod ->
        OutlinedButton(
            onClick = onSignOutClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ext.dangerText
            ),
            border = BorderStroke(1.dp, ext.dangerBorder),
            modifier = btnMod
                .defaultMinSize(minHeight = 48.dp)
                .desktopHandCursor()
                .testTag("sign_out_button")
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackButtons = maxWidth < 260.dp || (isTightFont && maxWidth < 340.dp)
        if (stackButtons) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                syncNowButton(Modifier.fillMaxWidth())
                signOutButton(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                syncNowButton(Modifier.weight(1f))
                signOutButton(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun AccountSignOutDialog(
    preferDesktopDialogs: Boolean,
    onConfirmSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !preferDesktopDialogs),
        modifier = Modifier
            .then(if (preferDesktopDialogs) Modifier.widthIn(min = 400.dp, max = 520.dp) else Modifier)
            .onEscapeKey { onDismiss() },
        containerColor = ext.surfaceCard,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(ext.dangerBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = ext.dangerIcon,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = { Text("Sign Out of Account?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Text(
                "Your personal data will remain safely backed up in the cloud. You can sign in anytime on this or another device to restore it.",
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmSignOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
            ) {
                Text("Confirm Sign Out", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun ResponsiveDashboardStatsGrid(
    notesCount: Int,
    tasksCount: Int,
    eventsCount: Int,
    isTightFont: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackStats = maxWidth < 270.dp || (isTightFont && maxWidth < 350.dp)
        if (stackStats) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardStatItem(
                    title = "Notes",
                    count = notesCount,
                    icon = Icons.AutoMirrored.Outlined.Notes,
                    modifier = Modifier.fillMaxWidth()
                )
                DashboardStatItem(
                    title = "Tasks",
                    count = tasksCount,
                    icon = Icons.Outlined.TaskAlt,
                    modifier = Modifier.fillMaxWidth()
                )
                DashboardStatItem(
                    title = "Events",
                    count = eventsCount,
                    icon = Icons.Outlined.CalendarMonth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardStatItem(
                    title = "Notes",
                    count = notesCount,
                    icon = Icons.AutoMirrored.Outlined.Notes,
                    modifier = Modifier.weight(1f)
                )
                DashboardStatItem(
                    title = "Tasks",
                    count = tasksCount,
                    icon = Icons.Outlined.TaskAlt,
                    modifier = Modifier.weight(1f)
                )
                DashboardStatItem(
                    title = "Events",
                    count = eventsCount,
                    icon = Icons.Outlined.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DashboardStatItem(
    title: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ext.surfaceBase,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = ext.textMuted
            )
        }
    }
}
