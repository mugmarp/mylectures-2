package com.lectures

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.lectures.ui.AppTheme
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setBackgroundDrawableResource(android.R.color.transparent)

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
                    ThemeMode.valueOf(themeModeStr.uppercase())
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
