package com.lectures

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase6BuildHardeningAndPlayPolicyTest {

    private val app: TimetableApplication
        get() = ApplicationProvider.getApplicationContext()

    private fun resolveAppFile(relativeFromApp: String): File {
        val candidates = listOf(
            File(relativeFromApp),
            File("app/$relativeFromApp"),
            File("../app/$relativeFromApp")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate $relativeFromApp from ${File(".").absolutePath}")
    }

    @Test
    fun playPolicyPermissionsExcludeBroadMediaStorageAndInstallPackages() {
        val packageInfo = app.packageManager.getPackageInfo(
            app.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = packageInfo.requestedPermissions?.toSet().orEmpty()

        val prohibitedPermissions = setOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
            "android.permission.REQUEST_INSTALL_PACKAGES"
        )

        val violations = requested.intersect(prohibitedPermissions)
        assertTrue("Prohibited Play Policy permissions present: $violations", violations.isEmpty())
    }

    @Test
    fun playPolicyAppMetadataCompliesWithBrandingAndTitleConstraints() {
        val appName = app.getString(R.string.app_name)
        assertTrue("App name should be non-blank", appName.isNotBlank())
        assertTrue("App name must be <= 30 chars (was ${appName.length})", appName.length <= 30)
        assertFalse("App name must not be ALL CAPS", appName.length > 1 && appName == appName.uppercase())

        val promoBuzzwords = listOf("free", "best", "#1", "top app")
        val lowerName = appName.lowercase()
        assertTrue(
            "App name must not include promotional buzzwords",
            promoBuzzwords.none { lowerName.contains(it) }
        )

        assertEquals("com.lectures.must", BuildConfig.APPLICATION_ID)
        assertTrue("targetSdk should remain >= 35", app.applicationInfo.targetSdkVersion >= 35)
    }

    @Test
    fun manifestComponentsExplicitlyDeclareExportedAndFileProviderAuthority() {
        val pm = app.packageManager
        val flags = PackageManager.GET_ACTIVITIES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS
        val pkg: PackageInfo = pm.getPackageInfo(app.packageName, flags)

        val providers = pkg.providers.orEmpty()
        val fileProvider = providers.firstOrNull { it.name.contains("FileProvider") }
        assertTrue("FileProvider must be registered", fileProvider != null)
        assertFalse("FileProvider must not be exported", fileProvider!!.exported)
        assertEquals("${app.packageName}.fileprovider", fileProvider.authority)

        val mainActivity = pkg.activities.orEmpty().firstOrNull { it.name.endsWith("MainActivity") }
        assertTrue("MainActivity must be registered", mainActivity != null)
        assertTrue("MainActivity launcher must be exported", mainActivity!!.exported)

        val manifestText = resolveAppFile("src/main/AndroidManifest.xml").readText()
        assertFalse(
            "Manifest should declare allowBackup explicitly without broad unguarded exports",
            manifestText.contains("android:requestLegacyExternalStorage=\"true\"")
        )
    }

    @Test
    fun backupRulesExcludeVolatileSqliteJournalsAndDeviceSyncState() {
        val backupXml = resolveAppFile("src/main/res/xml/backup_rules.xml").readText()
        val extractionXml = resolveAppFile("src/main/res/xml/data_extraction_rules.xml").readText()

        val expectedExclusions = listOf(
            "mustime_db-wal",
            "mustime_db-shm",
            "androidx.work.workdb",
            "mustime_sync_prefs.xml"
        )
        expectedExclusions.forEach { entry ->
            assertTrue("backup_rules.xml missing exclusion for $entry", backupXml.contains(entry))
            assertTrue("data_extraction_rules.xml missing exclusion for $entry", extractionXml.contains(entry))
        }
    }

    @Test
    fun proguardRulesPreserveRoomWorkManagerFirestoreAndCredentialReflectiveClasses() {
        val proguard = resolveAppFile("proguard-rules.pro").readText()

        val requiredKeepRules = listOf(
            "androidx.room.RoomDatabase",
            "androidx.work.ListenableWorker",
            "android.content.BroadcastReceiver",
            "com.google.firebase.firestore.PropertyName",
            "androidx.credentials.**",
            "com.lectures.core.sync.**"
        )
        requiredKeepRules.forEach { rule ->
            assertTrue("proguard-rules.pro missing keep rule: $rule", proguard.contains(rule))
        }
    }
}
