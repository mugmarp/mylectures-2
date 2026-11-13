package com.lectures.features.account

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.TimetableApplication
import com.lectures.core.auth.AuthRepository
import com.lectures.core.sync.SyncMergeStats
import com.lectures.core.sync.UserSettingsSyncManager
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.LocalAppTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val authRepo = remember { AuthRepository.getInstance() }
    val currentUser by authRepo.currentUser.collectAsState()

    val isDark = LocalAppTheme.current.isDark
    val surfaceColor = if (isDark) Color(0xFF1E293B) else Color.White
    val borderSubtle = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    var isAuthenticating by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

    // Live counts
    val notesCount by (app?.database?.timetableDao()?.getAllNotes()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) })
    val eventsCount by (app?.database?.customEventDao()?.getAll()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) })
    val tasksCount by (app?.database?.assignmentDao()?.getAll()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) })

    Scaffold(
        topBar = {
            FloatingIslandTopBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("account_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Account & Cloud Sync",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Error / Success Banner
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
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { errorMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = syncMessage != null, enter = fadeIn(), exit = fadeOut()) {
                syncMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(msg, color = Color(0xFF14532D), fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { syncMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (currentUser == null) {
                // ==================== GUEST STATE ====================
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderSubtle),
                    modifier = Modifier.fillMaxWidth()
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
                            color = textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Preserved Local Data Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Local Unsynced Data",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    "${notesCount.size} Notes · ${tasksCount.size} Tasks · ${eventsCount.size} Events",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sign In with Google Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isAuthenticating = true
                                    errorMessage = null
                                    syncMessage = null
                                    val result = authRepo.signInWithGoogle(context)
                                    isAuthenticating = false
                                    result.fold(
                                        onSuccess = { user ->
                                            syncMessage = "Signed in as ${user.email ?: user.displayName}! Syncing your data..."
                                            app?.let { application ->
                                                val settingsMgr = UserSettingsSyncManager(
                                                    application.etagStore,
                                                    application.onboardingDataStore
                                                )
                                                val stats = kotlinx.coroutines.withContext(application.applicationScope.coroutineContext) {
                                                    application.syncRepository.performFullUserSync(
                                                        user.uid,
                                                        settingsMgr
                                                    )
                                                }
                                                if (stats.status == com.lectures.core.sync.SyncState.ERROR || stats.failedCount > 0) {
                                                    syncMessage = null
                                                    errorMessage = stats.errorMessage ?: "Signed in as ${user.email}, but cloud sync could not complete."
                                                } else {
                                                    syncMessage = "Connected to ${user.email}! Synced ${stats.notesCount} notes, ${stats.assignmentsCount} tasks to cloud."
                                                }
                                            }
                                        },
                                        onFailure = { err ->
                                            errorMessage = "Sign-in issue: ${err.localizedMessage ?: "User cancelled or Google Play services unavailable"}"
                                        }
                                    )
                                }
                            },
                            enabled = !isAuthenticating,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_signin_button")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Connecting to Google...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            } else {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            } else {
                // ==================== AUTHENTICATED STATE ====================
                val user = currentUser!!
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // User Monogram Avatar
                            val initial = (user.displayName?.firstOrNull() ?: user.email?.firstOrNull() ?: 'U').uppercaseChar()
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial.toString(),
                                    color = Color.White,
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
                                    color = textMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF22C55E), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Cloud Sync Active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = borderSubtle
                        )

                        // Data Dashboard
                        Text(
                            text = "Preserved Coursework & Timetable Data",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardStatItem(
                                title = "Notes",
                                count = notesCount.size,
                                icon = Icons.Outlined.Notes,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardStatItem(
                                title = "Tasks",
                                count = tasksCount.size,
                                icon = Icons.Outlined.TaskAlt,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardStatItem(
                                title = "Events",
                                count = eventsCount.size,
                                icon = Icons.Outlined.CalendarMonth,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Action Buttons: Sync Now & Sign Out
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isSyncing = true
                                        errorMessage = null
                                        syncMessage = null
                                        app?.let { application ->
                                            val settingsMgr = UserSettingsSyncManager(
                                                application.etagStore,
                                                application.onboardingDataStore
                                            )
                                            val stats = kotlinx.coroutines.withContext(application.applicationScope.coroutineContext) {
                                                application.syncRepository.performFullUserSync(
                                                    user.uid,
                                                    settingsMgr
                                                )
                                            }
                                            if (stats.status == com.lectures.core.sync.SyncState.ERROR || stats.failedCount > 0) {
                                                errorMessage = stats.errorMessage ?: "Cloud sync encountered an error (${stats.failedCount} failed)."
                                            } else {
                                                val format = SimpleDateFormat("h:mm a", Locale.getDefault())
                                                syncMessage = "Synced at ${format.format(Date())}: ${stats.notesCount} notes, ${stats.assignmentsCount} tasks up to date."
                                            }
                                        }
                                        isSyncing = false
                                    }
                                },
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("sync_now_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Syncing...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sync Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { showSignOutDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFDC2626)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
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
                    onClick = {
                        showSignOutDialog = false
                        scope.launch {
                            currentUser?.uid?.let { uid ->
                                app?.syncRepository?.stopUserSync()
                                app?.syncRepository?.purgeUserDataOnSignOut(uid)
                            }
                            authRepo.signOut(context)
                            syncMessage = "Signed out. App reverted to guest mode."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DashboardStatItem(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val bg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val border = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
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
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}
