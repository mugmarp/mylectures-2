package com.lectures.features.settings

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lectures.TimetableApplication
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.ReminderScheduler
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.update.AppUpdateDialog
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.features.account.AccountScreen
import com.lectures.features.developer.DeveloperProfileSheet
import com.lectures.features.feedback.AnonymousFeedbackSheet
import com.lectures.features.feedback.FeedbackManager
import com.lectures.features.onboarding.FacultyCatalog
import com.lectures.features.timetable.data.ETagStore
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.ThemeMode
import com.lectures.ui.components.FloatingIslandTopBar
import kotlinx.coroutines.Dispatchers

enum class SettingsSubScreen {
    MAIN,
    APPEARANCE
}

@Composable
fun SettingsScreen(
    currentProgramme: String = "",
    onReconfigureAcademicProfile: () -> Unit = {},
    onResetOnboarding: (() -> Unit)? = null,
    onOpenAccount: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    var currentSubScreen by rememberSaveable { mutableStateOf(SettingsSubScreen.MAIN) }

    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

    var isAccountScreenOpen by rememberSaveable { mutableStateOf(false) }

    val handleOpenAccount = {
        if (onOpenAccount != null) {
            onOpenAccount()
        } else {
            isAccountScreenOpen = true
        }
    }

    if (isAccountScreenOpen) {
        AccountScreen(onBack = { isAccountScreenOpen = false })
        return
    }

    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository
    val dataLoader = app?.dataLoader
    val alarmScheduler = remember(context) { TaskAlarmScheduler(context) }
    val classAlarmScheduler = remember(context) { AlarmScheduler(context) }
    val reminderScheduler = remember(context) { ReminderScheduler(context) }

    if (repository == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            repository = repository,
            dataLoader = dataLoader,
            alarmScheduler = alarmScheduler,
            classAlarmScheduler = classAlarmScheduler,
            reminderScheduler = reminderScheduler,
            ioDispatcher = Dispatchers.IO,
            application = context.applicationContext as Application
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val appTheme = LocalAppTheme.current
    val isDark = appTheme.isDark

    val onSelectThemeMode: (ThemeMode) -> Unit = { mode ->
        viewModel.setThemeMode(mode)
        appTheme.onThemeModeChange(mode)
    }

    val onSelectAccent: (Int) -> Unit = { idx ->
        viewModel.setSelectedAccent(idx)
        appTheme.onAccentChange(idx)
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportUserDataBackup(uri)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val msg = uiState.errorMessage ?: uiState.successMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedbackMessages()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearFeedbackMessages()
        }
    }

    var showResetNotesDialog by rememberSaveable { mutableStateOf(false) }
    var showResetTasksDialog by rememberSaveable { mutableStateOf(false) }
    var showDisclaimerDialog by rememberSaveable { mutableStateOf(false) }
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showDeveloperModal by rememberSaveable { mutableStateOf(false) }
    var showFeedbackModal by rememberSaveable { mutableStateOf(false) }
    var showCustomLeadDialog by rememberSaveable { mutableStateOf(false) }
    var customLeadInput by rememberSaveable { mutableStateOf("") }
    var showVersionDetailsDialog by rememberSaveable { mutableStateOf(false) }

    if (showCustomLeadDialog) {
        CustomLeadMinutesDialog(
            input = customLeadInput,
            onInputChange = { customLeadInput = it },
            onSave = { mins -> viewModel.setClassAlarmLeadMinutes(mins) },
            onDismiss = { showCustomLeadDialog = false }
        )
    }
    if (showDisclaimerDialog) DisclaimerDialog(onDismiss = { showDisclaimerDialog = false })
    if (showTermsDialog) TermsDialog(onDismiss = { showTermsDialog = false })
    if (showPrivacyDialog) PrivacyDialog(onDismiss = { showPrivacyDialog = false })

    val activeProgramme = if (uiState.currentProgramme.isNotBlank()) uiState.currentProgramme else currentProgramme
    val progCode = activeProgramme.substringBefore(" ").trim()
    val faculty = remember(progCode) {
        FacultyCatalog.faculties.find { f -> f.programmes.any { it.code.equals(progCode, ignoreCase = true) } }
    }
    val programme = remember(progCode, faculty) {
        faculty?.programmes?.find { it.code.equals(progCode, ignoreCase = true) }
    }
    val programmeDisplayName = programme?.name ?: "Academic Programme ($activeProgramme)"
    val facultyDisplayName = faculty?.name ?: "Mbarara University of Science and Technology"

    if (showResetNotesDialog) {
        ResetNotesConfirmDialog(
            onConfirm = { viewModel.resetAllNotes { showResetNotesDialog = false } },
            onDismiss = { showResetNotesDialog = false }
        )
    }
    if (showResetTasksDialog) {
        ResetTasksConfirmDialog(
            onConfirm = { viewModel.resetAllTasks { showResetTasksDialog = false } },
            onDismiss = { showResetTasksDialog = false }
        )
    }
    if (showDeveloperModal) {
        DeveloperProfileSheet(onDismiss = { showDeveloperModal = false })
    }
    if (showFeedbackModal) {
        val feedbackMgr = app.feedbackManager ?: remember(context) {
            val store = app.etagStore ?: ETagStore(context)
            FeedbackManager(context, store)
        }
        AnonymousFeedbackSheet(
            feedbackManager = feedbackMgr,
            onDismiss = { showFeedbackModal = false }
        )
    }
    if (showVersionDetailsDialog) {
        VersionDetailsDialog(
            uiState = uiState,
            isDark = isDark,
            onDismiss = { showVersionDetailsDialog = false }
        )
    }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            FloatingIslandTopBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null || currentSubScreen != SettingsSubScreen.MAIN) {
                        IconButton(onClick = {
                            if (currentSubScreen != SettingsSubScreen.MAIN) {
                                currentSubScreen = SettingsSubScreen.MAIN
                            } else {
                                onBack?.invoke()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = if (currentSubScreen == SettingsSubScreen.APPEARANCE) "Appearance & Theme" else "Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { padding ->
        if (currentSubScreen == SettingsSubScreen.APPEARANCE) {
            AppearanceSettingsView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                uiState = uiState,
                appTheme = appTheme,
                onSelectThemeMode = onSelectThemeMode,
                onSelectAccent = onSelectAccent
            )
        } else {
            var areNotificationsAllowed by remember {
                mutableStateOf(NotificationPermissionHelper.areNotificationsEnabled(context))
            }
            var canScheduleExactAlarms by remember {
                mutableStateOf(NotificationPermissionHelper.canScheduleExactAlarms(context))
            }
            var blockedChannels by remember {
                mutableStateOf(NotificationPermissionHelper.getBlockedReminderChannels(context))
            }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                areNotificationsAllowed = NotificationPermissionHelper.areNotificationsEnabled(context)
                canScheduleExactAlarms = NotificationPermissionHelper.canScheduleExactAlarms(context)
                blockedChannels = NotificationPermissionHelper.getBlockedReminderChannels(context)
            }
            var isPermissionBannerDismissed by rememberSaveable { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedVisibility(
                        visible = uiState.errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        uiState.errorMessage?.let { error ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.clearFeedbackMessages() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.successMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        uiState.successMessage?.let { success ->
                            val successBg = if (isDark) Color(0xFF064E3B) else Color(0xFFF0FDF4)
                            val successBorder = if (isDark) Color(0xFF059669) else Color(0xFFBBF7D0)
                            val successContent = if (isDark) Color(0xFFA7F3D0) else Color(0xFF166534)
                            val successIcon = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = successBg),
                                border = BorderStroke(1.dp, successBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = successIcon)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(success, color = successContent, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.clearFeedbackMessages() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = successIcon, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SectionHeader("ACADEMIC PROFILE")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onReconfigureAcademicProfile),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(18.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(programmeDisplayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$activeProgramme • $facultyDisplayName", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Switch",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsNotificationsSection(
                        context = context,
                        uiState = uiState,
                        isDark = isDark,
                        areNotificationsAllowed = areNotificationsAllowed,
                        canScheduleExactAlarms = canScheduleExactAlarms,
                        blockedChannels = blockedChannels,
                        isPermissionBannerDismissed = isPermissionBannerDismissed,
                        onDismissPermissionBanner = { isPermissionBannerDismissed = true },
                        onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                        onSelectClassLeadMinutes = { viewModel.setClassAlarmLeadMinutes(it) },
                        onOpenCustomLeadDialog = {
                            customLeadInput = uiState.classAlarmLeadMinutes.toString()
                            showCustomLeadDialog = true
                        },
                        onSelectTaskLeadHours = { viewModel.setTaskReminderLeadHours(it) },
                        onToggleVibration = { viewModel.setAlarmVibration(it) },
                        onSendTestAlert = { viewModel.sendTestNotification(context) },
                        onReArmAllAlarms = { viewModel.rescheduleAllAlarms() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsAppearanceSection(
                        uiState = uiState,
                        appTheme = appTheme,
                        onOpenAppearance = { currentSubScreen = SettingsSubScreen.APPEARANCE }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsDataAndSyncSection(
                        uiState = uiState,
                        isDark = isDark,
                        onSyncTimetable = { viewModel.syncTimetable() },
                        onOpenAccount = handleOpenAccount,
                        onSyncUserData = { viewModel.syncUserData() },
                        onExportBackup = { filename -> exportBackupLauncher.launch(filename) },
                        onShowVersionDetails = { showVersionDetailsDialog = true },
                        onCheckOrOpenUpdate = {
                            if (uiState.availableUpdateInfo != null) {
                                viewModel.openUpdateDialog()
                            } else {
                                viewModel.checkForAppUpdate()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsAboutAndDangerSection(
                        onOpenDeveloper = { showDeveloperModal = true },
                        onOpenFeedback = { showFeedbackModal = true },
                        onOpenDisclaimer = { showDisclaimerDialog = true },
                        onOpenTerms = { showTermsDialog = true },
                        onOpenPrivacy = { showPrivacyDialog = true },
                        onRequestResetNotes = { showResetNotesDialog = true },
                        onRequestResetTasks = { showResetTasksDialog = true }
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    val updateInfoToShow = uiState.availableUpdateInfo
    if (uiState.showAppUpdateDialog && updateInfoToShow != null) {
        AppUpdateDialog(
            updateInfo = updateInfoToShow,
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }
}
