package com.lectures.core.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lectures.TimetableApplication
import com.lectures.core.database.AppDatabase
import com.lectures.core.notification.NotificationHelper
import com.lectures.features.timetable.domain.TimetableMetadata
import com.lectures.features.timetable.domain.TimetableVersionResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that periodically polls remote timetable metadata,
 * detects published updates or revisions, updates local Room cache,
 * and posts a system notification to inform students.
 */
class TimetableSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = performSync(applicationContext, runAttemptCount)

    companion object {
        const val TAG = "TimetableSyncWorker"
        const val WORK_NAME = "PeriodicTimetableSyncWork"
        const val PREF_REMOTE_METADATA_URL = "remote_timetable_metadata_url"
        const val PREF_REMOTE_TIMETABLE_URL = "remote_timetable_export_url"
        const val DEFAULT_METADATA_URL = "https://raw.githubusercontent.com/mugmarp/mylectures-2/main/timetable_metadata.json"
        const val DEFAULT_TIMETABLE_EXPORT_URL = "https://raw.githubusercontent.com/mugmarp/mylectures-2/main/timetable_export.json"

        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }

        internal suspend fun performSync(
            appContext: Context,
            runAttemptCount: Int = 0,
            checkAppUpdate: Boolean = true
        ): Result = withContext(Dispatchers.IO) {
            val app = appContext.applicationContext as? TimetableApplication
            val db: AppDatabase? = app?.database
            if (db == null) {
                Log.w(TAG, "Application or AppDatabase not initialized; retrying later")
                return@withContext Result.retry()
            }

            try {
                val localMeta: TimetableMetadata? = db.timetableDao().getTimetableMetadataSync()
                Log.d(TAG, "Current local timetable metadata: ${localMeta?.versionLabel} (updated: ${localMeta?.lastUpdated})")

                // 1. Try remote HTTP metadata check first, then fallback to Firestore metadata
                var remoteMeta: TimetableMetadata? = fetchFromHttp(appContext)
                val fromHttp = remoteMeta != null
                if (remoteMeta == null) {
                    remoteMeta = app.syncRepository.fetchRemoteTimetableMetadata()
                }

                if (remoteMeta == null) {
                    Log.d(TAG, "No remote timetable metadata could be fetched at this time")
                    return@withContext Result.success()
                }

                // 2. Compare semantic release attributes rather than synthetic System.currentTimeMillis()
                val isNewer = isNewerVersion(localMeta, remoteMeta)

                if (isNewer) {
                    Log.i(TAG, "New timetable version detected! Local: '${localMeta?.versionLabel}', Remote: '${remoteMeta.versionLabel}'")

                    // 3. Download and atomically apply actual timetable entries BEFORE claiming schedules updated
                    var syncedCount = 0
                    if (fromHttp) {
                        syncedCount = syncEntriesFromHttp(appContext, app)
                    }
                    if (syncedCount == 0) {
                        syncedCount = app.syncRepository.syncRemoteTimetableEntries()
                    }

                    if (syncedCount > 0) {
                        val syncedMeta = remoteMeta.copy(
                            lastUpdated = System.currentTimeMillis(),
                            totalSessions = syncedCount
                        )
                        db.timetableDao().upsertTimetableMetadata(syncedMeta)

                        val nowStr = java.text.SimpleDateFormat("h:mm a, MMM d", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        app.etagStore.setLastSyncTime("Synced at $nowStr")

                        // Re-arm enrolled class alarms and daily digest for updated schedule
                        com.lectures.core.alarm.AlarmSyncCoordinator.resyncAll(
                            context = appContext,
                            repository = app.repository,
                            force = true,
                            reason = "timetable_sync_worker"
                        )

                        if (app.etagStore.isNotificationsEnabled()) {
                            NotificationHelper.showTimetableUpdatedNotification(
                                context = appContext,
                                versionLabel = syncedMeta.versionLabel,
                                releaseNotes = syncedMeta.releaseNotes
                            )
                        }
                    } else {
                        Log.w(
                            TAG,
                            "Remote version '${remoteMeta.versionLabel}' detected, but timetable entries could not be fetched yet; skipping false update notification."
                        )
                    }
                } else {
                    Log.d(TAG, "Timetable is up to date (${localMeta?.versionLabel})")
                }

                // 4. Also check for new App Releases (sideload update check)
                if (checkAppUpdate) {
                    try {
                        val updateResult = com.lectures.core.update.InAppUpdateManager.checkForUpdate(appContext)
                        updateResult.getOrNull()?.let { updateInfo ->
                            val updatePrefs = appContext.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
                            val lastNotifiedCode = updatePrefs.getInt("last_notified_version_code", -1)
                            val lastNotifiedTime = updatePrefs.getLong("last_notified_timestamp", 0L)
                            val now = System.currentTimeMillis()
                            val throttleWindowMs = 36 * 60 * 60 * 1000L // 36 hours throttle window

                            val shouldNotify = app.etagStore.isNotificationsEnabled() && (
                                updateInfo.isMandatory ||
                                updateInfo.versionCode > lastNotifiedCode ||
                                (now - lastNotifiedTime > throttleWindowMs)
                            )

                            if (shouldNotify) {
                                NotificationHelper.showAppUpdateNotification(appContext, updateInfo)
                                updatePrefs.edit()
                                    .putInt("last_notified_version_code", updateInfo.versionCode)
                                    .putLong("last_notified_timestamp", now)
                                    .apply()
                                Log.i(TAG, "Posted app update notification for v${updateInfo.versionName} (code=${updateInfo.versionCode})")
                            } else {
                                Log.d(TAG, "Suppressed app update notification for v${updateInfo.versionName}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Periodic app update check skipped: ${e.message}")
                    }
                }

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error executing TimetableSyncWorker: ${e.message}", e)
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }

        private fun fetchFromHttp(appContext: Context): TimetableMetadata? {
            val prefs = appContext.getSharedPreferences("mustime_sync_prefs", Context.MODE_PRIVATE)
            val url = prefs.getString(PREF_REMOTE_METADATA_URL, DEFAULT_METADATA_URL) ?: return null

            return try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", "Lectures-Android-SyncWorker")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.d(TAG, "Remote HTTP check returned code ${response.code}")
                        return null
                    }
                    val bodyString = response.body?.string() ?: return null
                    parseMetadataJson(bodyString)
                }
            } catch (e: Exception) {
                Log.d(TAG, "fetchFromHttp skipped or network unavailable: ${e.message}")
                null
            }
        }

        private suspend fun syncEntriesFromHttp(appContext: Context, app: TimetableApplication): Int {
            val prefs = appContext.getSharedPreferences("mustime_sync_prefs", Context.MODE_PRIVATE)
            val metadataUrl = prefs.getString(PREF_REMOTE_METADATA_URL, DEFAULT_METADATA_URL) ?: DEFAULT_METADATA_URL
            val exportUrl = prefs.getString(PREF_REMOTE_TIMETABLE_URL, null)?.takeIf { it.isNotBlank() }
                ?: deriveExportUrl(metadataUrl)

            return try {
                val request = Request.Builder()
                    .url(exportUrl)
                    .header("Accept", "application/json")
                    .header("User-Agent", "Lectures-Android-SyncWorker")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.d(TAG, "Remote timetable entries fetch returned code ${response.code}")
                        return 0
                    }
                    val bodyStream = response.body?.byteStream() ?: return 0
                    val entries = app.dataLoader.parseEntriesFromStream(bodyStream)
                    if (entries.isNotEmpty()) {
                        app.dataLoader.replaceTimetableEntries(entries)
                    } else {
                        0
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "syncEntriesFromHttp failed: ${e.message}")
                0
            }
        }

        internal fun deriveExportUrl(metadataUrl: String): String {
            return if (metadataUrl.endsWith("timetable_metadata.json")) {
                metadataUrl.removeSuffix("timetable_metadata.json") + "timetable_export.json"
            } else {
                DEFAULT_TIMETABLE_EXPORT_URL
            }
        }

        internal fun parseMetadataJson(bodyString: String): TimetableMetadata? {
            return try {
                val obj = JSONObject(bodyString)
                val rawVersion = obj.optString("version_label", obj.optString("version", "Final Timetable"))
                val isExplicitFinal = if (obj.has("is_final")) obj.optBoolean("is_final") else null
                val acadYear = obj.optString("academic_year", "2026/2027")
                val semester = obj.optString("semester", "Semester 1")
                val releaseNotes = if (obj.has("release_notes") && !obj.isNull("release_notes")) {
                    obj.optString("release_notes").ifBlank { null }
                } else null
                val totalSessions = obj.optInt("total_sessions", 1304)
                val serverTimestamp = when {
                    obj.has("last_updated") -> obj.optLong("last_updated", 0L)
                    obj.has("updated_at") -> obj.optLong("updated_at", 0L)
                    else -> 0L
                }

                TimetableVersionResolver.resolve(
                    rawVersion = rawVersion,
                    isExplicitFinal = isExplicitFinal,
                    academicYear = acadYear,
                    semester = semester,
                    releaseNotes = releaseNotes,
                    totalEntries = totalSessions,
                    lastUpdatedMillis = serverTimestamp
                )
            } catch (_: Exception) {
                null
            }
        }

        internal fun isNewerVersion(local: TimetableMetadata?, remote: TimetableMetadata): Boolean {
            if (local == null) return true
            if (remote.versionLabel != local.versionLabel) return true
            if (remote.draftNumber != null && local.draftNumber != null && remote.draftNumber > local.draftNumber) return true
            if (remote.isFinal && !local.isFinal) return true
            if (remote.academicYear != local.academicYear || remote.semester != local.semester) return true
            if (remote.totalSessions > 0 && remote.totalSessions != local.totalSessions) return true
            if (!remote.releaseNotes.isNullOrBlank() && remote.releaseNotes != local.releaseNotes) return true
            // Only compare timestamps when remote carries an explicit server-side timestamp (> 0)
            if (remote.lastUpdated > 0L && remote.lastUpdated > local.lastUpdated) return true
            return false
        }
    }
}
