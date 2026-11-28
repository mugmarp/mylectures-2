package com.lectures.features.account

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.TimetableApplication
import com.lectures.core.alarm.AlarmSyncCoordinator
import com.lectures.core.auth.AuthRepository
import com.lectures.core.storage.AttachmentStore
import com.lectures.core.sync.SyncState
import com.lectures.core.sync.UserSettingsSyncManager
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }

    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val authRepo = remember { AuthRepository.getInstance() }
    val currentUser by authRepo.currentUser.collectAsState()

    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val surfaceColor = ext.surfaceCard
    val borderSubtle = ext.borderSubtle

    var isAuthenticating by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var isDeletingCloudData by remember { mutableStateOf(false) }
    var syncMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteCloudDataDialog by rememberSaveable { mutableStateOf(false) }
    var alsoClearLocalOnCloudDelete by rememberSaveable { mutableStateOf(false) }

    val notesCount by (app?.database?.timetableDao()?.getAllNotes()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) })
    val eventsCount by (app?.database?.customEventDao()?.getAll()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) })
    val tasksCount by (app?.database?.assignmentDao()?.getAll()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) })

    val windowInfo = rememberLecturesWindow()
    Scaffold(
        topBar = {
            FloatingIslandTopBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (onBack == null) 16.dp else 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .desktopHandCursor()
                                .testTag("account_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = "Account & Cloud Sync",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        containerColor = ext.surfaceBase
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
                    .widthIn(max = windowInfo.maxReadingWidth)
                    .verticalScroll(scrollState)
                    .padding(horizontal = if (windowInfo.isDualPane) 24.dp else 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AccountErrorBanner(
                    errorMessage = errorMessage,
                    onDismiss = { errorMessage = null }
                )

                AccountSuccessBanner(
                    syncMessage = syncMessage,
                    onDismiss = { syncMessage = null }
                )

                AnimatedContent(
                    targetState = currentUser,
                    contentKey = { it?.uid },
                    transitionSpec = { MotionSpec.fadeTransform(reducedMotion = reducedMotion) },
                    label = "account_auth_state"
                ) { activeUser ->
                    if (activeUser == null) {
                        val onSignInClick = {
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
                                            val stats = withContext(application.applicationScope.coroutineContext) {
                                                application.syncRepository.performFullUserSync(
                                                    user.uid,
                                                    settingsMgr
                                                )
                                            }
                                            if (stats.status == SyncState.ERROR || stats.failedCount > 0) {
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
                            Unit
                        }

                        if (windowInfo.isDualPane) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("account_desktop_guest_row"),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                AccountGuestSignInCard(
                                    modifier = Modifier.weight(1.05f),
                                    includeInlinePill = false,
                                    notesCount = notesCount.size,
                                    tasksCount = tasksCount.size,
                                    eventsCount = eventsCount.size,
                                    isTightFont = windowInfo.isTightFont,
                                    isAuthenticating = isAuthenticating,
                                    onSignInClick = onSignInClick
                                )
                                AccountGuestLocalDataCard(
                                    modifier = Modifier.weight(0.95f),
                                    notesCount = notesCount.size,
                                    tasksCount = tasksCount.size,
                                    eventsCount = eventsCount.size,
                                    isTightFont = windowInfo.isTightFont
                                )
                            }
                        } else {
                            AccountGuestSignInCard(
                                modifier = Modifier.fillMaxWidth(),
                                includeInlinePill = true,
                                notesCount = notesCount.size,
                                tasksCount = tasksCount.size,
                                eventsCount = eventsCount.size,
                                isTightFont = windowInfo.isTightFont,
                                isAuthenticating = isAuthenticating,
                                onSignInClick = onSignInClick
                            )
                        }
                    } else {
                        val user = activeUser
                        val onSyncNowClick = {
                            scope.launch {
                                isSyncing = true
                                errorMessage = null
                                syncMessage = null
                                app?.let { application ->
                                    val settingsMgr = UserSettingsSyncManager(
                                        application.etagStore,
                                        application.onboardingDataStore
                                    )
                                    val stats = withContext(application.applicationScope.coroutineContext) {
                                        application.syncRepository.performFullUserSync(
                                            user.uid,
                                            settingsMgr
                                        )
                                    }
                                    if (stats.status == SyncState.ERROR || stats.failedCount > 0) {
                                        errorMessage = stats.errorMessage ?: "Cloud sync encountered an error (${stats.failedCount} failed)."
                                    } else {
                                        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
                                        syncMessage = "Synced at ${format.format(Date())}: ${stats.notesCount} notes, ${stats.assignmentsCount} tasks up to date."
                                    }
                                }
                                isSyncing = false
                            }
                            Unit
                        }

                        if (windowInfo.isDualPane) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("account_desktop_auth_row"),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                    border = BorderStroke(1.dp, borderSubtle),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                        AccountProfileHeader(user = user)
                                        AccountActionButtonsSection(
                                            isSyncing = isSyncing,
                                            isTightFont = windowInfo.isTightFont,
                                            isDeletingCloudData = isDeletingCloudData,
                                            onSyncNowClick = onSyncNowClick,
                                            onSignOutClick = { showSignOutDialog = true },
                                            onDeleteCloudDataClick = {
                                                alsoClearLocalOnCloudDelete = false
                                                showDeleteCloudDataDialog = true
                                            }
                                        )
                                    }
                                }
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                    border = BorderStroke(1.dp, borderSubtle),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        AccountDataDashboardSection(
                                            notesCount = notesCount.size,
                                            tasksCount = tasksCount.size,
                                            eventsCount = eventsCount.size,
                                            isTightFont = windowInfo.isTightFont
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                border = BorderStroke(1.dp, borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    AccountProfileHeader(user = user)
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        color = borderSubtle
                                    )
                                    AccountDataDashboardSection(
                                        notesCount = notesCount.size,
                                        tasksCount = tasksCount.size,
                                        eventsCount = eventsCount.size,
                                        isTightFont = windowInfo.isTightFont
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    AccountActionButtonsSection(
                                        isSyncing = isSyncing,
                                        isTightFont = windowInfo.isTightFont,
                                        isDeletingCloudData = isDeletingCloudData,
                                        onSyncNowClick = onSyncNowClick,
                                        onSignOutClick = { showSignOutDialog = true },
                                        onDeleteCloudDataClick = {
                                            alsoClearLocalOnCloudDelete = false
                                            showDeleteCloudDataDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(windowInfo.navBarBottom + 32.dp))
            }
        }
    }

    if (showSignOutDialog) {
        AccountSignOutDialog(
            preferDesktopDialogs = windowInfo.preferDesktopDialogs,
            onConfirmSignOut = {
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
            onDismiss = { showSignOutDialog = false }
        )
    }

    if (showDeleteCloudDataDialog) {
        AccountDeleteCloudDataDialog(
            preferDesktopDialogs = windowInfo.preferDesktopDialogs,
            isDeleting = isDeletingCloudData,
            alsoClearLocal = alsoClearLocalOnCloudDelete,
            onToggleClearLocal = { alsoClearLocalOnCloudDelete = it },
            onConfirmDelete = {
                val activeUid = currentUser?.uid
                if (activeUid.isNullOrBlank()) {
                    showDeleteCloudDataDialog = false
                    return@AccountDeleteCloudDataDialog
                }
                scope.launch {
                    isDeletingCloudData = true
                    errorMessage = null
                    syncMessage = null
                    val application = app
                    if (application == null) {
                        isDeletingCloudData = false
                        showDeleteCloudDataDialog = false
                        errorMessage = "Cloud sync service is unavailable."
                        return@launch
                    }

                    application.syncRepository.stopUserSync()
                    val deleteResult = withContext(application.applicationScope.coroutineContext) {
                        application.syncRepository.deleteAllCloudUserData(activeUid)
                    }

                    deleteResult.fold(
                        onSuccess = { deletedDocsCount ->
                            if (alsoClearLocalOnCloudDelete) {
                                withContext(Dispatchers.IO) {
                                    AttachmentStore.deleteAll(context)
                                    application.syncRepository.purgeUserDataOnSignOut(activeUid)
                                    application.database.timetableDao().deleteAllNotes()
                                    application.database.customEventDao().deleteAll()
                                    application.database.assignmentDao().deleteAll()
                                }
                                AlarmSyncCoordinator.resyncAll(
                                    context = context,
                                    repository = application.repository,
                                    force = true,
                                    dispatcher = Dispatchers.IO,
                                    reason = "account_delete_cloud_and_local"
                                )
                                authRepo.signOut(context)
                                syncMessage = "Cloud backup ($deletedDocsCount records) and local copies deleted. Switched to guest mode."
                            } else {
                                application.syncRepository.demoteUserToGuestOnCloudDelete(activeUid)
                                authRepo.signOut(context)
                                syncMessage = "Cloud backup ($deletedDocsCount records) deleted. Your local data remains saved in guest mode."
                            }
                            showDeleteCloudDataDialog = false
                        },
                        onFailure = { err ->
                            application.syncRepository.startAllSync(activeUid, application.applicationScope)
                            errorMessage = err.localizedMessage ?: "Failed to delete cloud backup data."
                            showDeleteCloudDataDialog = false
                        }
                    )
                    isDeletingCloudData = false
                }
            },
            onDismiss = { showDeleteCloudDataDialog = false }
        )
    }
}
