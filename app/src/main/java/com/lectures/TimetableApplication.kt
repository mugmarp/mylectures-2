package com.lectures

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.room.Room
import com.lectures.core.database.AppDatabase
import com.lectures.core.network.FirebaseFactory
import com.lectures.core.sync.SyncRepository
import com.lectures.features.onboarding.OnboardingDataStore
import com.lectures.features.timetable.data.DataLoader
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.data.TimetableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TimetableApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var etagStore: ETagStore
    lateinit var onboardingDataStore: OnboardingDataStore
    lateinit var repository: TimetableRepository
    lateinit var syncRepository: SyncRepository
    lateinit var dataLoader: DataLoader
    lateinit var feedbackManager: com.lectures.features.feedback.FeedbackManager
    lateinit var analyticsManager: com.lectures.core.analytics.AnalyticsManager

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    @Volatile
    private var startedActivitiesCount = 0

    override fun onCreate() {
        super.onCreate()

        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (_: Exception) { }

        // Install App Check: Debug provider in debug builds (via reflection so release builds
        // compile cleanly without debug-only artifact on classpath), Play Integrity in release.
        try {
            val factory = if (BuildConfig.DEBUG) {
                runCatching {
                    val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    val getInstance = clazz.getMethod("getInstance")
                    getInstance.invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
                }.getOrNull()
            } else null
            com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(
                    factory ?: com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
                )
        } catch (_: Exception) { }

        // Initialize Android NotificationChannels for academic task reminders and class alarms
        com.lectures.core.notification.NotificationHelper.createNotificationChannels(this)

        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "mustime_db"
        )
        // First install: copy the prepackaged asset (1304 rows already present)
        // instead of parsing JSON and inserting 1304 rows on the device.
        // Only used when mustime_db does not yet exist; existing installs go
        // through the migrations below as before.
        .createFromAsset(AppDatabase.PREPOPULATED_DB_NAME)
        .addMigrations(
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11,
            AppDatabase.MIGRATION_11_12
        )
        // Strictly protect all production coursework migrations (v2-v7).
        // Only allow destructive reset for pre-release prototype version 1.
        .fallbackToDestructiveMigrationFrom(true, 1)
        .build()

        etagStore = ETagStore(this)
        onboardingDataStore = OnboardingDataStore(this)
        dataLoader = DataLoader(this, database)

        syncRepository = SyncRepository(database)
        repository = TimetableRepository(
            dao = database.timetableDao(),
            eventDao = database.customEventDao(),
            assignmentDao = database.assignmentDao(),
            etagStore = etagStore,
            onboardingDataStore = onboardingDataStore,
            syncRepository = syncRepository
        )

        feedbackManager = com.lectures.features.feedback.FeedbackManager(this, etagStore)
        analyticsManager = com.lectures.core.analytics.AnalyticsManager(this)

        schedulePeriodicTimetableSync()

        // Ensure pre-packaged / asset database is hydrated locally without attaching network listeners
        applicationScope.launch {
            dataLoader.loadInitialDataIfNeeded()
        }

        syncRepository.onRemoteSettingsPulled = {
            if (repository.checkIsOnboardingCompleted()) {
                resyncLocalAlarmsFromSettings(
                    force = true,
                    preserveImminentExisting = true,
                    reason = "remote_settings"
                )
            }
        }

        syncRepository.onUserScheduleDataSynced = {
            if (repository.checkIsOnboardingCompleted()) {
                resyncLocalAlarmsFromSettings(
                    force = true,
                    preserveImminentExisting = true,
                    reason = "remote_user_data"
                )
            }
        }

        // Bind live Firestore listeners strictly while a UI Activity is in the foreground
        applicationScope.launch {
            combine(
                isAppInForeground,
                com.lectures.core.auth.AuthRepository.getInstance().currentUser
            ) { inForeground, user ->
                inForeground to user
            }
                .distinctUntilChanged()
                .collect { (inForeground, user) ->
                    if (inForeground) {
                        syncRepository.startTimetableSync(applicationScope)
                        if (user != null) {
                            syncRepository.startAllSync(user.uid, applicationScope)
                        } else {
                            syncRepository.stopUserSync()
                        }
                    } else {
                        syncRepository.stopAllSync()
                    }
                }
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                val wasBackgrounded = startedActivitiesCount == 0
                startedActivitiesCount++
                if (wasBackgrounded && !_isAppInForeground.value) {
                    _isAppInForeground.value = true
                    onForegroundEntered()
                }
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                startedActivitiesCount = (startedActivitiesCount - 1).coerceAtLeast(0)
                if (startedActivitiesCount == 0 && !activity.isChangingConfigurations) {
                    _isAppInForeground.value = false
                    onForegroundExited()
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun onForegroundEntered() {
        applicationScope.launch {
            dataLoader.loadInitialDataIfNeeded()
            feedbackManager.flushPendingSubmissions()
            val prog = etagStore.getSavedProgramme() ?: ""
            analyticsManager.recordAppLaunch(prog, this)
            if (repository.checkIsOnboardingCompleted()) {
                resyncLocalAlarmsFromSettings(
                    force = false,
                    preserveImminentExisting = true,
                    reason = "app_foreground"
                )
            }
        }
    }

    private fun onForegroundExited() {
        syncRepository.stopAllSync()
    }

    suspend fun resyncLocalAlarmsFromSettings(
        force: Boolean = false,
        preserveImminentExisting: Boolean = false,
        reason: String = "app_launch"
    ) {
        com.lectures.core.alarm.AlarmSyncCoordinator.resyncAll(
            context = this,
            repository = repository,
            force = force,
            preserveImminentExisting = preserveImminentExisting,
            reason = reason
        )
    }

    private fun schedulePeriodicTimetableSync() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.lectures.core.sync.TimetableSyncWorker>(
                12, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                com.lectures.core.sync.TimetableSyncWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            android.util.Log.d("TimetableApplication", "Periodic timetable sync worker enqueued (12h interval)")
        } catch (e: Exception) {
            android.util.Log.e("TimetableApplication", "Failed to schedule TimetableSyncWorker: ${e.message}")
        }
    }
}
