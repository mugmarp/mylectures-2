package com.lectures

import android.app.UiModeManager
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.lectures.ui.AppTheme
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.MainScaffold
import com.lectures.ui.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingUpdateInfo by androidx.compose.runtime.mutableStateOf<com.lectures.core.update.AppUpdateInfo?>(null)

    // Notification taps emit (navTab, taskId) pairs that MainScaffold consumes.
    // replay = 1 ensures cold-start intent events are retained until MainScaffold subscribes.
    private val notificationIntentFlow = MutableSharedFlow<Pair<Int, Long?>>(
        replay = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    companion object {
        private const val STATE_UPDATE_VERSION_CODE = "state_update_version_code"
        private const val STATE_UPDATE_VERSION_NAME = "state_update_version_name"
        private const val STATE_UPDATE_RELEASE_NOTES = "state_update_release_notes"
        private const val STATE_UPDATE_DOWNLOAD_URL = "state_update_download_url"
        private const val STATE_UPDATE_FILE_SIZE = "state_update_file_size"
        private const val STATE_UPDATE_SHA256 = "state_update_sha256"
        private const val STATE_UPDATE_IS_MANDATORY = "state_update_is_mandatory"
        private const val STATE_UPDATE_PUBLISHED_AT = "state_update_published_at"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = splashScreenView.iconView
            splashScreenView.view.animate()
                .alpha(0f)
                .setDuration(220L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction { splashScreenView.remove() }
                .start()
            iconView?.animate()
                ?.scaleX(1.08f)
                ?.scaleY(1.08f)
                ?.alpha(0f)
                ?.setDuration(200L)
                ?.start()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            // Process notification intent extras on cold start only (never replay across rotation).
            handleNotificationIntent(intent)
        } else {
            val savedVersionCode = savedInstanceState.getInt(STATE_UPDATE_VERSION_CODE, 0)
            val savedUrl = savedInstanceState.getString(STATE_UPDATE_DOWNLOAD_URL, "")
            if (savedVersionCode > 0 && savedUrl.isNotBlank()) {
                pendingUpdateInfo = com.lectures.core.update.AppUpdateInfo(
                    versionCode = savedVersionCode,
                    versionName = savedInstanceState.getString(STATE_UPDATE_VERSION_NAME, ""),
                    releaseNotes = savedInstanceState.getString(STATE_UPDATE_RELEASE_NOTES, ""),
                    downloadUrl = savedUrl,
                    fileSizeBytes = savedInstanceState.getLong(STATE_UPDATE_FILE_SIZE, 0L),
                    sha256 = savedInstanceState.getString(STATE_UPDATE_SHA256),
                    isMandatory = savedInstanceState.getBoolean(STATE_UPDATE_IS_MANDATORY, false),
                    publishedAt = savedInstanceState.getString(STATE_UPDATE_PUBLISHED_AT)
                )
            }
        }

        setContent {
            val app = application as? TimetableApplication
            val repo = app?.repository
            val initialThemeModeStr = remember { repo?.getThemeMode() ?: "LIGHT" }
            val themeModeStr by (repo?.themeModePref ?: flowOf(initialThemeModeStr)).collectAsState(initial = initialThemeModeStr)
            val accentIndex by (repo?.themeAccentPref ?: flowOf(0)).collectAsState(initial = 0)

            val themeMode = remember(themeModeStr) {
                try {
                    ThemeMode.valueOf(themeModeStr.uppercase(java.util.Locale.ROOT))
                } catch (e: Exception) {
                    ThemeMode.LIGHT
                }
            }

            AppTheme(
                themeMode = themeMode,
                accentIndex = accentIndex,
                onThemeModeChange = { newMode ->
                    lifecycleScope.launch {
                        repo?.setThemeMode(newMode.name)
                    }
                },
                onAccentChange = { newAccent ->
                    lifecycleScope.launch {
                        repo?.setThemeAccent(newAccent)
                    }
                }
            ) {
                val isDarkEffective = LocalAppTheme.current.isDark
                LaunchedEffect(themeMode) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val uiModeManager = getSystemService(UiModeManager::class.java)
                        val targetNightMode = when (themeMode) {
                            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                        }
                        runCatching { uiModeManager?.setApplicationNightMode(targetNightMode) }
                    }
                }
                DisposableEffect(isDarkEffective) {
                    window.setBackgroundDrawable(
                        ColorDrawable(
                            if (isDarkEffective) 0xFF101216.toInt() else 0xFFF8FAFC.toInt()
                        )
                    )
                    enableEdgeToEdge(
                        statusBarStyle = if (isDarkEffective) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT
                            )
                        },
                        navigationBarStyle = if (isDarkEffective) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT
                            )
                        }
                    )
                    onDispose {}
                }

                MainScaffold(
                    initialTab = 0,
                    notificationIntentFlow = notificationIntentFlow
                )

                pendingUpdateInfo?.let { updateInfo ->
                    com.lectures.core.update.AppUpdateDialog(
                        updateInfo = updateInfo,
                        onDismiss = {
                            if (!updateInfo.isMandatory) {
                                pendingUpdateInfo = null
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingUpdateInfo?.let { info ->
            outState.putInt(STATE_UPDATE_VERSION_CODE, info.versionCode)
            outState.putString(STATE_UPDATE_VERSION_NAME, info.versionName)
            outState.putString(STATE_UPDATE_RELEASE_NOTES, info.releaseNotes)
            outState.putString(STATE_UPDATE_DOWNLOAD_URL, info.downloadUrl)
            outState.putLong(STATE_UPDATE_FILE_SIZE, info.fileSizeBytes)
            outState.putString(STATE_UPDATE_SHA256, info.sha256)
            outState.putBoolean(STATE_UPDATE_IS_MANDATORY, info.isMandatory)
            outState.putString(STATE_UPDATE_PUBLISHED_AT, info.publishedAt)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra("EXTRA_SHOW_UPDATE_DIALOG", false)) {
            val versionCode = intent.getIntExtra("EXTRA_UPDATE_VERSION_CODE", 0)
            val versionName = intent.getStringExtra("EXTRA_UPDATE_VERSION_NAME") ?: ""
            val releaseNotes = intent.getStringExtra("EXTRA_UPDATE_RELEASE_NOTES") ?: ""
            val downloadUrl = intent.getStringExtra("EXTRA_UPDATE_DOWNLOAD_URL") ?: ""
            val fileSizeBytes = intent.getLongExtra("EXTRA_UPDATE_FILE_SIZE", 0L)
            val sha256 = intent.getStringExtra("EXTRA_UPDATE_SHA256")
            val isMandatory = intent.getBooleanExtra("EXTRA_UPDATE_IS_MANDATORY", false)

            if (versionCode > 0 && downloadUrl.isNotBlank()) {
                val isNewer = com.lectures.core.update.InAppUpdateManager.isNewerRelease(
                    remoteVersionName = versionName,
                    remoteVersionCode = versionCode,
                    localVersionName = BuildConfig.VERSION_NAME,
                    localVersionCode = BuildConfig.VERSION_CODE
                )
                if (isNewer) {
                    pendingUpdateInfo = com.lectures.core.update.AppUpdateInfo(
                        versionCode = versionCode,
                        versionName = versionName,
                        releaseNotes = releaseNotes,
                        downloadUrl = downloadUrl,
                        fileSizeBytes = fileSizeBytes,
                        sha256 = sha256,
                        isMandatory = isMandatory
                    )
                }
            }
        }

        val navTab = intent.getIntExtra("EXTRA_NAV_TAB", -1)
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L).takeIf { it != -1L }
        if (navTab != -1) {
            notificationIntentFlow.tryEmit(navTab to taskId)
        }
    }
}
