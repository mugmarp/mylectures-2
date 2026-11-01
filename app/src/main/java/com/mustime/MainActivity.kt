package com.mustime

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
import com.mustime.ui.AppTheme
import com.mustime.ui.MainScaffold
import com.mustime.ui.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingUpdateInfo by androidx.compose.runtime.mutableStateOf<com.mustime.core.update.AppUpdateInfo?>(null)

    // Notification taps emit (navTab, taskId) pairs that MainScaffold consumes.
    // replay = 1 ensures cold-start intent events are retained until MainScaffold subscribes.
    private val notificationIntentFlow = MutableSharedFlow<Pair<Int, Long?>>(
        replay = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Process notification intent extras on cold start.
        handleNotificationIntent(intent)

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
                    com.mustime.core.update.AppUpdateDialog(
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
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
                pendingUpdateInfo = com.mustime.core.update.AppUpdateInfo(
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

        val navTab = intent.getIntExtra("EXTRA_NAV_TAB", -1)
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L).takeIf { it != -1L }
        if (navTab != -1) {
            notificationIntentFlow.tryEmit(navTab to taskId)
        }
    }
}
