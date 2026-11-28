package com.lectures.features.settings

import android.app.Application
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lectures.TimetableApplication
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.update.AppUpdateDialog
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.features.account.AccountScreen
import com.lectures.features.developer.DeveloperProfileSheet
import com.lectures.features.feedback.AnonymousFeedbackSheet
import com.lectures.features.feedback.FeedbackManager
import com.lectures.features.onboarding.FacultyCatalog
import com.lectures.features.timetable.data.ETagStore
import com.lectures.ui.AppThemeState
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.ThemeMode
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    OVERVIEW("Overview & Profile", "Programme & quick status", Icons.Outlined.School),
    NOTIFICATIONS("Notifications & Alarms", "Lead times, digest & delivery", Icons.Outlined.NotificationsActive),
    APPEARANCE("Appearance & Theme", "Dark mode & accent colors", Icons.Outlined.Palette),
    DATA_SYNC("Data, Sync & Reset", "Cloud backup, export & reset", Icons.Outlined.CloudSync),
    ABOUT_DANGER("About, Updates & Legal", "Releases, developer & privacy", Icons.Outlined.Info)
}

@Composable
fun SettingsScreen(
    currentProgramme: String = "",
    onReconfigureAcademicProfile: () -> Unit = {},
    onOpenAccount: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val windowInfo = rememberLecturesWindow()
    val reducedMotion = rememberPrefersReducedMotion()
    val ext = LocalExtendedColors.current
    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.OVERVIEW) }
    var isAccountScreenOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isAccountScreenOpen) {
        isAccountScreenOpen = false
    }

    BackHandler(enabled = !isAccountScreenOpen && !windowInfo.isDualPane && selectedCategory != SettingsCategory.OVERVIEW) {
        selectedCategory = SettingsCategory.OVERVIEW
    }

    val handleOpenAccount = {
        if (onOpenAccount != null) {
            onOpenAccount()
        } else {
            isAccountScreenOpen = true
        }
    }

    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository
    val dataLoader = app?.dataLoader
    val alarmScheduler = remember(context) { TaskAlarmScheduler(context) }
    val classAlarmScheduler = remember(context) { AlarmScheduler(context) }

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

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val msg = uiState.errorMessage ?: uiState.successMessage
        if (msg != null) {
            delay(4000L)
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
        if (progCode.isBlank()) null
        else FacultyCatalog.faculties.find { f -> f.programmes.any { it.code.equals(progCode, ignoreCase = true) } }
    }
    val programme = remember(progCode, faculty) {
        if (progCode.isBlank()) null
        else faculty?.programmes?.find { it.code.equals(progCode, ignoreCase = true) }
    }
    val programmeDisplayName = when {
        programme != null -> programme.name
        activeProgramme.isNotBlank() -> "Academic Programme ($activeProgramme)"
        else -> "No Programme Selected"
    }
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
    val feedbackMgr = remember(app, context) {
        app.feedbackManager ?: FeedbackManager(context, app.etagStore ?: ETagStore(context))
    }
    if (showFeedbackModal) {
        AnonymousFeedbackSheet(
            feedbackManager = feedbackMgr,
            onDismiss = { showFeedbackModal = false }
        )
    }
    if (showVersionDetailsDialog) {
        VersionDetailsDialog(
            uiState = uiState,
            onDismiss = { showVersionDetailsDialog = false }
        )
    }

    val updateInfoToShow = uiState.availableUpdateInfo
    if (uiState.showAppUpdateDialog && updateInfoToShow != null) {
        AppUpdateDialog(
            updateInfo = updateInfoToShow,
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }

    val railScrollState = rememberScrollState()
    val overviewScrollState = rememberScrollState()
    val notificationsScrollState = rememberScrollState()
    val appearanceScrollState = rememberScrollState()
    val dataSyncScrollState = rememberScrollState()
    val aboutDangerScrollState = rememberScrollState()

    AnimatedContent(
        targetState = isAccountScreenOpen,
        transitionSpec = {
            MotionSpec.directionalSlideTransform(
                forward = targetState,
                reducedMotion = reducedMotion,
                useFadeOnly = windowInfo.useNavigationRail
            )
        },
        label = "settings_account_transition"
    ) { accountOpen ->
        if (accountOpen) {
            AccountScreen(onBack = { isAccountScreenOpen = false })
        } else {
            Scaffold(
                containerColor = ext.surfaceBase,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    FloatingIslandTopBar {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val showSubBack = !windowInfo.isDualPane && selectedCategory != SettingsCategory.OVERVIEW
                            if (onBack != null || showSubBack) {
                                IconButton(
                                    onClick = {
                                        if (showSubBack) {
                                            selectedCategory = SettingsCategory.OVERVIEW
                                        } else {
                                            onBack?.invoke()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .desktopHandCursor()
                                ) {
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
                            val topBarTitle = if (windowInfo.isDualPane || selectedCategory == SettingsCategory.OVERVIEW) {
                                "Settings"
                            } else {
                                selectedCategory.title
                            }
                            AnimatedContent(
                                targetState = topBarTitle,
                                transitionSpec = { MotionSpec.fadeTransform(reducedMotion = reducedMotion) },
                                label = "settings_topbar_title"
                            ) { titleText ->
                                Text(
                                    text = titleText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                var areNotificationsAllowed by remember {
                    mutableStateOf(NotificationPermissionHelper.areNotificationsEnabled(context))
                }
                var canScheduleExactAlarms by remember {
                    mutableStateOf(NotificationPermissionHelper.canScheduleExactAlarms(context))
                }
                var blockedChannels by remember {
                    mutableStateOf(
                        NotificationPermissionHelper.getBlockedReminderChannels(
                            context = context,
                            includeDailyDigest = uiState.dailyDigestEnabled
                        )
                    )
                }
                LaunchedEffect(uiState.dailyDigestEnabled, uiState.notificationsEnabled) {
                    blockedChannels = NotificationPermissionHelper.getBlockedReminderChannels(
                        context = context,
                        includeDailyDigest = uiState.dailyDigestEnabled
                    )
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    areNotificationsAllowed = NotificationPermissionHelper.areNotificationsEnabled(context)
                    canScheduleExactAlarms = NotificationPermissionHelper.canScheduleExactAlarms(context)
                    blockedChannels = NotificationPermissionHelper.getBlockedReminderChannels(
                        context = context,
                        includeDailyDigest = uiState.dailyDigestEnabled
                    )
                }
                var isPermissionBannerDismissed by rememberSaveable { mutableStateOf(false) }

                val bottomContentPadding = windowInfo.navBarBottom + 32.dp
                val openCustomLeadDialog = {
                    customLeadInput = uiState.classAlarmLeadMinutes.toString()
                    showCustomLeadDialog = true
                }
                val checkOrOpenUpdate = {
                    if (uiState.availableUpdateInfo != null) {
                        viewModel.openUpdateDialog()
                    } else {
                        viewModel.checkForAppUpdate()
                    }
                }

                if (windowInfo.isDualPane) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = windowInfo.maxReadingWidth)
                                .fillMaxSize()
                                .padding(start = 24.dp, end = 24.dp, top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            SettingsCategoryRail(
                                selectedCategory = selectedCategory,
                                isTightFont = windowInfo.isTightFont,
                                reducedMotion = reducedMotion,
                                bottomPadding = windowInfo.navBarBottom + 14.dp,
                                railScrollState = railScrollState,
                                onSelectCategory = { selectedCategory = it }
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("settings_detail_pane")
                            ) {
                                SettingsFeedbackBanners(
                                    errorMessage = uiState.errorMessage,
                                    successMessage = uiState.successMessage,
                                    horizontalPadding = 0.dp,
                                    onDismiss = { viewModel.clearFeedbackMessages() }
                                )

                                AnimatedContent(
                                    targetState = selectedCategory,
                                    transitionSpec = { MotionSpec.fadeTransform(reducedMotion = reducedMotion) },
                                    contentKey = { it },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    label = "settings_desktop_detail"
                                ) { activeCategory ->
                                    SettingsCategoryPaneContent(
                                        category = activeCategory,
                                        constrainWidth = false,
                                        horizontalPadding = 0.dp,
                                        bottomContentPadding = bottomContentPadding,
                                        context = context,
                                        uiState = uiState,
                                        appTheme = appTheme,
                                        isDark = isDark,
                                        isTightFont = windowInfo.isTightFont,
                                        programmeDisplayName = programmeDisplayName,
                                        activeProgramme = activeProgramme,
                                        facultyDisplayName = facultyDisplayName,
                                        areNotificationsAllowed = areNotificationsAllowed,
                                        canScheduleExactAlarms = canScheduleExactAlarms,
                                        blockedChannels = blockedChannels,
                                        isPermissionBannerDismissed = isPermissionBannerDismissed,
                                        overviewScrollState = overviewScrollState,
                                        notificationsScrollState = notificationsScrollState,
                                        appearanceScrollState = appearanceScrollState,
                                        dataSyncScrollState = dataSyncScrollState,
                                        aboutDangerScrollState = aboutDangerScrollState,
                                        onSelectCategory = { selectedCategory = it },
                                        onDismissPermissionBanner = { isPermissionBannerDismissed = true },
                                        onReconfigureAcademicProfile = onReconfigureAcademicProfile,
                                        onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                                        onSelectClassLeadMinutes = { viewModel.setClassAlarmLeadMinutes(it) },
                                        onOpenCustomLeadDialog = openCustomLeadDialog,
                                        onSelectTaskLeadHours = { viewModel.setTaskReminderLeadHours(it) },
                                        onToggleDailyDigest = { viewModel.setDailyDigestEnabled(it) },
                                        onSelectThemeMode = onSelectThemeMode,
                                        onSelectAccent = onSelectAccent,
                                        onSyncTimetable = { viewModel.syncTimetable() },
                                        onOpenAccount = handleOpenAccount,
                                        onSyncUserData = { viewModel.syncUserData() },
                                        onExportBackup = { filename -> exportBackupLauncher.launch(filename) },
                                        onRequestResetNotes = { showResetNotesDialog = true },
                                        onRequestResetTasks = { showResetTasksDialog = true },
                                        onCheckOrOpenUpdate = checkOrOpenUpdate,
                                        onShowVersionDetails = { showVersionDetailsDialog = true },
                                        onOpenDeveloper = { showDeveloperModal = true },
                                        onOpenFeedback = { showFeedbackModal = true },
                                        onOpenDisclaimer = { showDisclaimerDialog = true },
                                        onOpenTerms = { showTermsDialog = true },
                                        onOpenPrivacy = { showPrivacyDialog = true }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            SettingsFeedbackBanners(
                                modifier = Modifier.widthIn(max = 760.dp),
                                errorMessage = uiState.errorMessage,
                                successMessage = uiState.successMessage,
                                horizontalPadding = 20.dp,
                                onDismiss = { viewModel.clearFeedbackMessages() }
                            )
                        }

                        AnimatedContent(
                            targetState = selectedCategory,
                            transitionSpec = {
                                MotionSpec.directionalSlideTransform(
                                    forward = targetState != SettingsCategory.OVERVIEW,
                                    reducedMotion = reducedMotion,
                                    useFadeOnly = windowInfo.useNavigationRail
                                )
                            },
                            contentKey = { it },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            label = "settings_phone_subscreen"
                        ) { activeCategory ->
                            SettingsCategoryPaneContent(
                                category = activeCategory,
                                constrainWidth = true,
                                horizontalPadding = 20.dp,
                                bottomContentPadding = bottomContentPadding,
                                context = context,
                                uiState = uiState,
                                appTheme = appTheme,
                                isDark = isDark,
                                isTightFont = windowInfo.isTightFont,
                                programmeDisplayName = programmeDisplayName,
                                activeProgramme = activeProgramme,
                                facultyDisplayName = facultyDisplayName,
                                areNotificationsAllowed = areNotificationsAllowed,
                                canScheduleExactAlarms = canScheduleExactAlarms,
                                blockedChannels = blockedChannels,
                                isPermissionBannerDismissed = isPermissionBannerDismissed,
                                overviewScrollState = overviewScrollState,
                                notificationsScrollState = notificationsScrollState,
                                appearanceScrollState = appearanceScrollState,
                                dataSyncScrollState = dataSyncScrollState,
                                aboutDangerScrollState = aboutDangerScrollState,
                                onSelectCategory = { selectedCategory = it },
                                onDismissPermissionBanner = { isPermissionBannerDismissed = true },
                                onReconfigureAcademicProfile = onReconfigureAcademicProfile,
                                onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                                onSelectClassLeadMinutes = { viewModel.setClassAlarmLeadMinutes(it) },
                                onOpenCustomLeadDialog = openCustomLeadDialog,
                                onSelectTaskLeadHours = { viewModel.setTaskReminderLeadHours(it) },
                                onToggleDailyDigest = { viewModel.setDailyDigestEnabled(it) },
                                onSelectThemeMode = onSelectThemeMode,
                                onSelectAccent = onSelectAccent,
                                onSyncTimetable = { viewModel.syncTimetable() },
                                onOpenAccount = handleOpenAccount,
                                onSyncUserData = { viewModel.syncUserData() },
                                onExportBackup = { filename -> exportBackupLauncher.launch(filename) },
                                onRequestResetNotes = { showResetNotesDialog = true },
                                onRequestResetTasks = { showResetTasksDialog = true },
                                onCheckOrOpenUpdate = checkOrOpenUpdate,
                                onShowVersionDetails = { showVersionDetailsDialog = true },
                                onOpenDeveloper = { showDeveloperModal = true },
                                onOpenFeedback = { showFeedbackModal = true },
                                onOpenDisclaimer = { showDisclaimerDialog = true },
                                onOpenTerms = { showTermsDialog = true },
                                onOpenPrivacy = { showPrivacyDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryPaneContent(
    category: SettingsCategory,
    constrainWidth: Boolean,
    horizontalPadding: Dp,
    bottomContentPadding: Dp,
    context: Context,
    uiState: SettingsUiState,
    appTheme: AppThemeState,
    isDark: Boolean,
    isTightFont: Boolean,
    programmeDisplayName: String,
    activeProgramme: String,
    facultyDisplayName: String,
    areNotificationsAllowed: Boolean,
    canScheduleExactAlarms: Boolean,
    blockedChannels: List<String>,
    isPermissionBannerDismissed: Boolean,
    overviewScrollState: ScrollState,
    notificationsScrollState: ScrollState,
    appearanceScrollState: ScrollState,
    dataSyncScrollState: ScrollState,
    aboutDangerScrollState: ScrollState,
    onSelectCategory: (SettingsCategory) -> Unit,
    onDismissPermissionBanner: () -> Unit,
    onReconfigureAcademicProfile: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSelectClassLeadMinutes: (Int) -> Unit,
    onOpenCustomLeadDialog: () -> Unit,
    onSelectTaskLeadHours: (Int) -> Unit,
    onToggleDailyDigest: (Boolean) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectAccent: (Int) -> Unit,
    onSyncTimetable: () -> Unit,
    onOpenAccount: () -> Unit,
    onSyncUserData: () -> Unit,
    onExportBackup: (String) -> Unit,
    onRequestResetNotes: () -> Unit,
    onRequestResetTasks: () -> Unit,
    onCheckOrOpenUpdate: () -> Unit,
    onShowVersionDetails: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val paneModifier = if (constrainWidth) {
        Modifier.fillMaxSize().widthIn(max = 760.dp)
    } else {
        Modifier.fillMaxSize()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (category) {
            SettingsCategory.OVERVIEW -> {
                SettingsOverviewContent(
                    modifier = paneModifier,
                    scrollState = overviewScrollState,
                    context = context,
                    uiState = uiState,
                    appTheme = appTheme,
                    isDark = isDark,
                    isTightFont = isTightFont,
                    horizontalPadding = horizontalPadding,
                    bottomContentPadding = bottomContentPadding,
                    programmeDisplayName = programmeDisplayName,
                    activeProgramme = activeProgramme,
                    facultyDisplayName = facultyDisplayName,
                    areNotificationsAllowed = areNotificationsAllowed,
                    canScheduleExactAlarms = canScheduleExactAlarms,
                    blockedChannels = blockedChannels,
                    isPermissionBannerDismissed = isPermissionBannerDismissed,
                    onDismissPermissionBanner = onDismissPermissionBanner,
                    onReconfigureAcademicProfile = onReconfigureAcademicProfile,
                    onToggleNotifications = onToggleNotifications,
                    onOpenNotificationPreferences = { onSelectCategory(SettingsCategory.NOTIFICATIONS) },
                    onOpenAppearance = { onSelectCategory(SettingsCategory.APPEARANCE) },
                    onOpenDataAndBackup = { onSelectCategory(SettingsCategory.DATA_SYNC) },
                    onOpenAboutAndLegal = { onSelectCategory(SettingsCategory.ABOUT_DANGER) }
                )
            }

            SettingsCategory.NOTIFICATIONS -> {
                NotificationSettingsView(
                    modifier = paneModifier,
                    scrollState = notificationsScrollState,
                    context = context,
                    uiState = uiState,
                    isDark = isDark,
                    areNotificationsAllowed = areNotificationsAllowed,
                    canScheduleExactAlarms = canScheduleExactAlarms,
                    blockedChannels = blockedChannels,
                    isPermissionBannerDismissed = isPermissionBannerDismissed,
                    isTightFont = isTightFont,
                    horizontalPadding = horizontalPadding,
                    bottomContentPadding = bottomContentPadding,
                    onDismissPermissionBanner = onDismissPermissionBanner,
                    onToggleNotifications = onToggleNotifications,
                    onSelectClassLeadMinutes = onSelectClassLeadMinutes,
                    onOpenCustomLeadDialog = onOpenCustomLeadDialog,
                    onSelectTaskLeadHours = onSelectTaskLeadHours,
                    onToggleDailyDigest = onToggleDailyDigest
                )
            }

            SettingsCategory.APPEARANCE -> {
                AppearanceSettingsView(
                    modifier = paneModifier,
                    scrollState = appearanceScrollState,
                    appTheme = appTheme,
                    isTightFont = isTightFont,
                    horizontalPadding = horizontalPadding,
                    bottomContentPadding = bottomContentPadding,
                    onSelectThemeMode = onSelectThemeMode,
                    onSelectAccent = onSelectAccent
                )
            }

            SettingsCategory.DATA_SYNC -> {
                DataAndBackupSettingsView(
                    modifier = paneModifier,
                    scrollState = dataSyncScrollState,
                    uiState = uiState,
                    isTightFont = isTightFont,
                    horizontalPadding = horizontalPadding,
                    bottomContentPadding = bottomContentPadding,
                    onSyncTimetable = onSyncTimetable,
                    onOpenAccount = onOpenAccount,
                    onSyncUserData = onSyncUserData,
                    onExportBackup = onExportBackup,
                    onRequestResetNotes = onRequestResetNotes,
                    onRequestResetTasks = onRequestResetTasks
                )
            }

            SettingsCategory.ABOUT_DANGER -> {
                AboutAndLegalSettingsView(
                    modifier = paneModifier,
                    scrollState = aboutDangerScrollState,
                    uiState = uiState,
                    isTightFont = isTightFont,
                    horizontalPadding = horizontalPadding,
                    bottomContentPadding = bottomContentPadding,
                    onCheckOrOpenUpdate = onCheckOrOpenUpdate,
                    onShowVersionDetails = onShowVersionDetails,
                    onOpenDeveloper = onOpenDeveloper,
                    onOpenFeedback = onOpenFeedback,
                    onOpenDisclaimer = onOpenDisclaimer,
                    onOpenTerms = onOpenTerms,
                    onOpenPrivacy = onOpenPrivacy
                )
            }
        }
    }
}
