package com.lectures.features.settings

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.ThemeMode
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

enum class SettingsSubScreen {
    MAIN,
    NOTIFICATIONS,
    APPEARANCE,
    DATA_BACKUP,
    ABOUT_LEGAL
}

enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val subScreen: SettingsSubScreen
) {
    OVERVIEW("Overview & Profile", "Programme & quick status", Icons.Outlined.School, SettingsSubScreen.MAIN),
    NOTIFICATIONS("Notifications & Alarms", "Lead times, digest & delivery", Icons.Outlined.NotificationsActive, SettingsSubScreen.NOTIFICATIONS),
    APPEARANCE("Appearance & Theme", "Dark mode & accent colors", Icons.Outlined.Palette, SettingsSubScreen.APPEARANCE),
    DATA_SYNC("Data, Sync & Reset", "Cloud backup, export & reset", Icons.Outlined.CloudSync, SettingsSubScreen.DATA_BACKUP),
    ABOUT_DANGER("About, Updates & Legal", "Releases, developer & privacy", Icons.Outlined.Info, SettingsSubScreen.ABOUT_LEGAL)
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
    var currentSubScreen by rememberSaveable { mutableStateOf(SettingsSubScreen.MAIN) }
    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.OVERVIEW) }

    var isAccountScreenOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isAccountScreenOpen) {
        isAccountScreenOpen = false
    }

    BackHandler(enabled = !isAccountScreenOpen && !windowInfo.isDualPane && currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
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

    val scrollState = rememberScrollState()
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
                    val showSubBack = !windowInfo.isDualPane && currentSubScreen != SettingsSubScreen.MAIN
                    if (onBack != null || showSubBack) {
                        IconButton(
                            onClick = {
                                if (showSubBack) {
                                    currentSubScreen = SettingsSubScreen.MAIN
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
                    val topBarTitle = if (windowInfo.isDualPane) {
                        "Settings"
                    } else {
                        when (currentSubScreen) {
                            SettingsSubScreen.APPEARANCE -> "Appearance & Theme"
                            SettingsSubScreen.NOTIFICATIONS -> "Notifications & Alarms"
                            SettingsSubScreen.DATA_BACKUP -> "Data, Sync & Reset"
                            SettingsSubScreen.ABOUT_LEGAL -> "About, Updates & Legal"
                            SettingsSubScreen.MAIN -> "Settings"
                        }
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
            mutableStateOf(NotificationPermissionHelper.getBlockedReminderChannels(context))
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            areNotificationsAllowed = NotificationPermissionHelper.areNotificationsEnabled(context)
            canScheduleExactAlarms = NotificationPermissionHelper.canScheduleExactAlarms(context)
            blockedChannels = NotificationPermissionHelper.getBlockedReminderChannels(context)
        }
        var isPermissionBannerDismissed by rememberSaveable { mutableStateOf(false) }

        val bottomContentPadding = windowInfo.navBarBottom + 32.dp

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
                    // Left Category Rail
                    val categoryRailWidth = if (windowInfo.isTightFont) 316.dp else 272.dp
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ext.surfaceCard,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .width(categoryRailWidth)
                            .fillMaxHeight()
                            .padding(bottom = windowInfo.navBarBottom + 14.dp)
                            .testTag("settings_category_rail")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(railScrollState)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PREFERENCES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            SettingsCategory.entries.forEach { category ->
                                val isSelected = selectedCategory == category
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                                    label = "settings_cat_bg"
                                )
                                val fgColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                                    label = "settings_cat_fg"
                                )
                                val iconTint by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                                    label = "settings_cat_icon"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .desktopHoverable(
                                            hoverContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                        )
                                        .background(bgColor, RoundedCornerShape(14.dp))
                                        .clickable {
                                            selectedCategory = category
                                            currentSubScreen = category.subScreen
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                        .testTag("settings_category_${category.name}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = category.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = fgColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = category.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right Detail Workspace
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
                        when (activeCategory) {
                            SettingsCategory.OVERVIEW -> {
                                SettingsOverviewContent(
                                    modifier = Modifier.fillMaxSize(),
                                    scrollState = overviewScrollState,
                                    context = context,
                                    uiState = uiState,
                                    appTheme = appTheme,
                                    isDark = isDark,
                                    isTightFont = windowInfo.isTightFont,
                                    horizontalPadding = 0.dp,
                                    bottomContentPadding = bottomContentPadding,
                                    programmeDisplayName = programmeDisplayName,
                                    activeProgramme = activeProgramme,
                                    facultyDisplayName = facultyDisplayName,
                                    areNotificationsAllowed = areNotificationsAllowed,
                                    canScheduleExactAlarms = canScheduleExactAlarms,
                                    blockedChannels = blockedChannels,
                                    isPermissionBannerDismissed = isPermissionBannerDismissed,
                                    onDismissPermissionBanner = { isPermissionBannerDismissed = true },
                                    onReconfigureAcademicProfile = onReconfigureAcademicProfile,
                                    onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                                    onOpenNotificationPreferences = {
                                        selectedCategory = SettingsCategory.NOTIFICATIONS
                                        currentSubScreen = SettingsSubScreen.NOTIFICATIONS
                                    },
                                    onOpenAppearance = {
                                        selectedCategory = SettingsCategory.APPEARANCE
                                        currentSubScreen = SettingsSubScreen.APPEARANCE
                                    },
                                    onOpenDataAndBackup = {
                                        selectedCategory = SettingsCategory.DATA_SYNC
                                        currentSubScreen = SettingsSubScreen.DATA_BACKUP
                                    },
                                    onOpenAboutAndLegal = {
                                        selectedCategory = SettingsCategory.ABOUT_DANGER
                                        currentSubScreen = SettingsSubScreen.ABOUT_LEGAL
                                    }
                                )
                            }

                            SettingsCategory.NOTIFICATIONS -> {
                                NotificationSettingsView(
                                    modifier = Modifier.fillMaxSize(),
                                    scrollState = notificationsScrollState,
                                    context = context,
                                    uiState = uiState,
                                    isDark = isDark,
                                    areNotificationsAllowed = areNotificationsAllowed,
                                    canScheduleExactAlarms = canScheduleExactAlarms,
                                    blockedChannels = blockedChannels,
                                    isPermissionBannerDismissed = isPermissionBannerDismissed,
                                    isTightFont = windowInfo.isTightFont,
                                    horizontalPadding = 0.dp,
                                    bottomContentPadding = bottomContentPadding,
                                    onDismissPermissionBanner = { isPermissionBannerDismissed = true },
                                    onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                                    onSelectClassLeadMinutes = { viewModel.setClassAlarmLeadMinutes(it) },
                                    onOpenCustomLeadDialog = {
                                        customLeadInput = uiState.classAlarmLeadMinutes.toString()
                                        showCustomLeadDialog = true
                                    },
                                    onSelectTaskLeadHours = { viewModel.setTaskReminderLeadHours(it) },
                                    onToggleDailyDigest = { viewModel.setDailyDigestEnabled(it) }
                                )
                            }

                            SettingsCategory.APPEARANCE -> {
                                AppearanceSettingsView(
                                    modifier = Modifier.fillMaxSize(),
                                    scrollState = appearanceScrollState,
                                    appTheme = appTheme,
                                    isTightFont = windowInfo.isTightFont,
                                    horizontalPadding = 0.dp,
                                    bottomContentPadding = bottomContentPadding,
                                    onSelectThemeMode = onSelectThemeMode,
                                    onSelectAccent = onSelectAccent
                                )
                            }

                            SettingsCategory.DATA_SYNC -> {
                                DataAndBackupSettingsView(
                                    modifier = Modifier.fillMaxSize(),
                                    scrollState = dataSyncScrollState,
                                    uiState = uiState,
                                    isTightFont = windowInfo.isTightFont,
                                    horizontalPadding = 0.dp,
                                    bottomContentPadding = bottomContentPadding,
                                    onSyncTimetable = { viewModel.syncTimetable() },
                                    onOpenAccount = handleOpenAccount,
                                    onSyncUserData = { viewModel.syncUserData() },
                                    onExportBackup = { filename -> exportBackupLauncher.launch(filename) },
                                    onRequestResetNotes = { showResetNotesDialog = true },
                                    onRequestResetTasks = { showResetTasksDialog = true }
                                )
                            }

                            SettingsCategory.ABOUT_DANGER -> {
                                AboutAndLegalSettingsView(
                                    modifier = Modifier.fillMaxSize(),
                                    scrollState = aboutDangerScrollState,
                                    uiState = uiState,
                                    horizontalPadding = 0.dp,
                                    bottomContentPadding = bottomContentPadding,
                                    onCheckOrOpenUpdate = {
                                        if (uiState.availableUpdateInfo != null) {
                                            viewModel.openUpdateDialog()
                                        } else {
                                            viewModel.checkForAppUpdate()
                                        }
                                    },
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
                    targetState = currentSubScreen,
                    transitionSpec = {
                        MotionSpec.directionalSlideTransform(
                            forward = targetState != SettingsSubScreen.MAIN,
                            reducedMotion = reducedMotion,
                            useFadeOnly = windowInfo.useNavigationRail
                        )
                    },
                    contentKey = { it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "settings_phone_subscreen"
                ) { activeSubScreen ->
                    when (activeSubScreen) {
                        SettingsSubScreen.APPEARANCE -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                AppearanceSettingsView(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 760.dp),
                                    scrollState = appearanceScrollState,
                                    appTheme = appTheme,
                                    isTightFont = windowInfo.isTightFont,
                                    horizontalPadding = 20.dp,
                                    bottomContentPadding = bottomContentPadding,
                                    onSelectThemeMode = onSelectThemeMode,
                                    onSelectAccent = onSelectAccent
                                )
                            }
                        }
                        SettingsSubScreen.NOTIFICATIONS -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                NotificationSettingsView(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 760.dp),
                                    scrollState = notificationsScrollState,
                                    context = context,
                                    uiState = uiState,
                                    isDark = isDark,
                                    areNotificationsAllowed = areNotificationsAllowed,
                                    canScheduleExactAlarms = canScheduleExactAlarms,
                                    blockedChannels = blockedChannels,
                                    isPermissionBannerDismissed = isPermissionBannerDismissed,
                                    isTightFont = windowInfo.isTightFont,
                                    horizontalPadding = 20.dp,
                                    bottomContentPadding = bottomContentPadding,
                                    onDismissPermissionBanner = { isPermissionBannerDismissed = true },
                                    onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                                    onSelectClassLeadMinutes = { viewModel.setClassAlarmLeadMinutes(it) },
                                    onOpenCustomLeadDialog = {
                                        customLeadInput = uiState.classAlarmLeadMinutes.toString()
                                        showCustomLeadDialog = true
                                    },
                                    onSelectTaskLeadHours = { viewModel.setTaskReminderLeadHours(it) },
                                    onToggleDailyDigest = { viewModel.setDailyDigestEnabled(it) }
                                )
                            }
                        }
                        SettingsSubScreen.DATA_BACKUP -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                DataAndBackupSettingsView(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 760.dp),
                                    scrollState = dataSyncScrollState,
                                    uiState = uiState,
                                    isTightFont = windowInfo.isTightFont,
                                    horizontalPadding = 20.dp,
                                    bottomContentPadding = bottomContentPadding,
                                    onSyncTimetable = { viewModel.syncTimetable() },
                                    onOpenAccount = handleOpenAccount,
                                    onSyncUserData = { viewModel.syncUserData() },
                                    onExportBackup = { filename -> exportBackupLauncher.launch(filename) },
                                    onRequestResetNotes = { showResetNotesDialog = true },
                                    onRequestResetTasks = { showResetTasksDialog = true }
                                )
                            }
                        }
                        SettingsSubScreen.ABOUT_LEGAL -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                AboutAndLegalSettingsView(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 760.dp),
                                    scrollState = aboutDangerScrollState,
                                    uiState = uiState,
                                    horizontalPadding = 20.dp,
                                    bottomContentPadding = bottomContentPadding,
                                    onCheckOrOpenUpdate = {
                                        if (uiState.availableUpdateInfo != null) {
                                            viewModel.openUpdateDialog()
                                        } else {
                                            viewModel.checkForAppUpdate()
                                        }
                                    },
                                    onShowVersionDetails = { showVersionDetailsDialog = true },
                                    onOpenDeveloper = { showDeveloperModal = true },
                                    onOpenFeedback = { showFeedbackModal = true },
                                    onOpenDisclaimer = { showDisclaimerDialog = true },
                                    onOpenTerms = { showTermsDialog = true },
                                    onOpenPrivacy = { showPrivacyDialog = true }
                                )
                            }
                        }
                        SettingsSubScreen.MAIN -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                SettingsOverviewContent(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 760.dp),
                                    scrollState = scrollState,
                                    context = context,
                                    uiState = uiState,
                                    appTheme = appTheme,
                                    isDark = isDark,
                                    isTightFont = windowInfo.isTightFont,
                                    horizontalPadding = 20.dp,
                                    bottomContentPadding = bottomContentPadding,
                                    programmeDisplayName = programmeDisplayName,
                                    activeProgramme = activeProgramme,
                                    facultyDisplayName = facultyDisplayName,
                                    areNotificationsAllowed = areNotificationsAllowed,
                                    canScheduleExactAlarms = canScheduleExactAlarms,
                                    blockedChannels = blockedChannels,
                                    isPermissionBannerDismissed = isPermissionBannerDismissed,
                                    onDismissPermissionBanner = { isPermissionBannerDismissed = true },
                                    onReconfigureAcademicProfile = onReconfigureAcademicProfile,
                                    onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                                    onOpenNotificationPreferences = {
                                        currentSubScreen = SettingsSubScreen.NOTIFICATIONS
                                        selectedCategory = SettingsCategory.NOTIFICATIONS
                                    },
                                    onOpenAppearance = {
                                        currentSubScreen = SettingsSubScreen.APPEARANCE
                                        selectedCategory = SettingsCategory.APPEARANCE
                                    },
                                    onOpenDataAndBackup = {
                                        currentSubScreen = SettingsSubScreen.DATA_BACKUP
                                        selectedCategory = SettingsCategory.DATA_SYNC
                                    },
                                    onOpenAboutAndLegal = {
                                        currentSubScreen = SettingsSubScreen.ABOUT_LEGAL
                                        selectedCategory = SettingsCategory.ABOUT_DANGER
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
        }
    }
}

@Composable
private fun SettingsOverviewContent(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    context: android.content.Context,
    uiState: SettingsUiState,
    appTheme: com.lectures.ui.AppThemeState,
    isDark: Boolean,
    isTightFont: Boolean,
    horizontalPadding: Dp,
    bottomContentPadding: Dp,
    programmeDisplayName: String,
    activeProgramme: String,
    facultyDisplayName: String,
    areNotificationsAllowed: Boolean,
    canScheduleExactAlarms: Boolean,
    blockedChannels: List<String>,
    isPermissionBannerDismissed: Boolean,
    onDismissPermissionBanner: () -> Unit,
    onReconfigureAcademicProfile: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onOpenNotificationPreferences: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenDataAndBackup: () -> Unit,
    onOpenAboutAndLegal: () -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(top = 14.dp)
    ) {
        SettingsAcademicProfileCard(
            programmeDisplayName = programmeDisplayName,
            activeProgramme = activeProgramme,
            facultyDisplayName = facultyDisplayName,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onReconfigureAcademicProfile = onReconfigureAcademicProfile
        )

        Spacer(modifier = Modifier.height(22.dp))

        SettingsNotificationsSection(
            context = context,
            uiState = uiState,
            isDark = isDark,
            areNotificationsAllowed = areNotificationsAllowed,
            canScheduleExactAlarms = canScheduleExactAlarms,
            blockedChannels = blockedChannels,
            isPermissionBannerDismissed = isPermissionBannerDismissed,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onDismissPermissionBanner = onDismissPermissionBanner,
            onToggleNotifications = onToggleNotifications,
            onOpenNotificationPreferences = onOpenNotificationPreferences
        )

        Spacer(modifier = Modifier.height(22.dp))

        SettingsAppearanceSection(
            appTheme = appTheme,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onOpenAppearance = onOpenAppearance
        )

        Spacer(modifier = Modifier.height(22.dp))

        SettingsDataSummaryCard(
            uiState = uiState,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onOpenDataAndBackup = onOpenDataAndBackup
        )

        Spacer(modifier = Modifier.height(22.dp))

        SettingsAboutSummaryCard(
            uiState = uiState,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onOpenAboutAndLegal = onOpenAboutAndLegal
        )

        Spacer(modifier = Modifier.height(bottomContentPadding))
    }
}

@Composable
private fun SettingsFeedbackBanners(
    modifier: Modifier = Modifier,
    errorMessage: String?,
    successMessage: String?,
    horizontalPadding: Dp = 20.dp,
    onDismiss: () -> Unit
) {
    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 8.dp),
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
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).desktopHandCursor()) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = successMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            successMessage?.let { success ->
                val ext = LocalExtendedColors.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ext.successBg),
                    border = BorderStroke(1.dp, ext.successBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ext.successIcon)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(success, color = ext.successText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).desktopHandCursor()) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = ext.successIcon, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsAcademicProfileCard(
    programmeDisplayName: String,
    activeProgramme: String,
    facultyDisplayName: String,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    onReconfigureAcademicProfile: () -> Unit
) {
    val ext = LocalExtendedColors.current
    SectionHeader("ACADEMIC PROFILE", horizontalPadding = horizontalPadding + 4.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .clip(RoundedCornerShape(16.dp))
            .desktopHoverable()
            .clickable(onClick = onReconfigureAcademicProfile),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
        border = BorderStroke(1.dp, ext.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            val stackSwitch = isTightFont && maxWidth < 300.dp
            val profileTextBlock: @Composable (Modifier) -> Unit = { colMod ->
                Column(modifier = colMod) {
                    Text(
                        text = programmeDisplayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isTightFont) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$activeProgramme • $facultyDisplayName",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isTightFont) 3 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val switchPill: @Composable () -> Unit = {
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

            if (stackSwitch) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    profileTextBlock(Modifier.fillMaxWidth())
                    switchPill()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    profileTextBlock(Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(12.dp))
                    switchPill()
                }
            }
        }
    }
}
