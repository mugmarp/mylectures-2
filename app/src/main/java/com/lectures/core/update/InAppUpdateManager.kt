package com.lectures.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.lectures.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Manages in-app application updates outside Google Play Store.
 * Handles checking remote version metadata, in-app background/progress downloading,
 * SHA-256 verification, pre-flight package parsing, automated cache management,
 * and seamless native 1-tap installation via FileProvider.
 */
object InAppUpdateManager {

    private const val TAG = "InAppUpdateManager"

    // Primary endpoint: raw version.json from project repo
    const val DEFAULT_VERSION_METADATA_URL =
        "https://raw.githubusercontent.com/mugmarp/mylectures/main/version.json"

    // Secondary fallback: GitHub Releases Latest API
    const val GITHUB_RELEASES_API_URL =
        "https://api.github.com/repos/mugmarp/mylectures/releases/latest"

    // In-memory cache to prevent exhausting GitHub unauthenticated 60 req/hr rate limit
    private var cachedUpdateInfo: Pair<Long, AppUpdateInfo?>? = null
    private const val CACHE_TTL_MS = 15 * 60 * 1000L // 15 minutes

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Checks if a new app version is available.
     * Compares remote version against local BuildConfig.
     */
    suspend fun checkForUpdate(
        context: Context,
        customUrl: String? = null,
        forceRefresh: Boolean = false
    ): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && customUrl == null) {
            cachedUpdateInfo?.let { (timestamp, info) ->
                if (now - timestamp < CACHE_TTL_MS) {
                    Log.d(TAG, "Using cached update check result")
                    val isNewer = info?.let {
                        isNewerRelease(it.versionName, it.versionCode, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                    } ?: false
                    return@withContext Result.success(if (isNewer) info else null)
                }
            }
        }

        try {
            val urlToFetch = customUrl ?: DEFAULT_VERSION_METADATA_URL
            val versionJsonResult = fetchFromVersionJson(urlToFetch)
            var updateInfo = versionJsonResult.getOrNull()

            // Fallback to GitHub Releases API if version.json returned nothing or failed
            if (updateInfo == null) {
                val ghResult = fetchFromGitHubReleases()
                if (ghResult.isSuccess) {
                    updateInfo = ghResult.getOrNull()
                } else if (versionJsonResult.isFailure) {
                    // Both endpoints failed due to network / IO issues. Return failure rather than false success.
                    val primaryException = versionJsonResult.exceptionOrNull() ?: ghResult.exceptionOrNull()
                    return@withContext Result.failure(
                        primaryException ?: IllegalStateException("Unable to connect to update servers")
                    )
                }
            }

            cachedUpdateInfo = now to updateInfo

            if (updateInfo == null) {
                return@withContext Result.success(null)
            }

            val isNewer = isNewerRelease(
                remoteVersionName = updateInfo.versionName,
                remoteVersionCode = updateInfo.versionCode,
                localVersionName = BuildConfig.VERSION_NAME,
                localVersionCode = BuildConfig.VERSION_CODE
            )

            Log.d(TAG, "Local build ${BuildConfig.VERSION_NAME} (code=${BuildConfig.VERSION_CODE}), " +
                    "Remote release ${updateInfo.versionName} (code=${updateInfo.versionCode}), isNewer=$isNewer")

            if (isNewer) {
                Result.success(updateInfo)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Check for update failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Purges stale, orphaned APKs and temporary download files from previous releases.
     * Prevents unbounded storage leaks in cacheDir/updates/.
     */
    fun purgeStaleUpdateFiles(context: Context, currentTargetVersionName: String? = null) {
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists() || !updatesDir.isDirectory) return
            val targetApkName = currentTargetVersionName?.let { "Lectures-v$it.apk" }

            updatesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".tmp") || (targetApkName != null && file.name.endsWith(".apk") && file.name != targetApkName)) {
                    val deleted = file.delete()
                    if (deleted) {
                        Log.d(TAG, "Purged stale update artifact: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to purge stale update files: ${e.message}")
        }
    }

    /**
     * Verifies that the downloaded APK is a valid, parseable package matching this app's package name.
     * Prevents Android's PackageInstaller from displaying "There was a problem parsing the package".
     */
    fun verifyPackageArchive(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(IllegalStateException("APK file is missing or empty"))
            }

            val archiveInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
                ?: return Result.failure(IllegalStateException("Downloaded file is not a valid Android APK package or is corrupted."))

            if (archiveInfo.packageName != context.packageName) {
                apkFile.delete()
                return Result.failure(
                    SecurityException("APK package name '${archiveInfo.packageName}' does not match expected '${context.packageName}'")
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Package archive pre-flight verification failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Checks whether an update APK has already been downloaded and verified in cache.
     */
    fun getDownloadedUpdateApk(context: Context, updateInfo: AppUpdateInfo): File? {
        val targetFile = getTargetApkFile(context, updateInfo)
        if (targetFile.exists() && targetFile.length() > 0) {
            val checksumMatches = updateInfo.sha256.isNullOrBlank() || verifyChecksum(targetFile, updateInfo.sha256)
            if (checksumMatches && verifyPackageArchive(context, targetFile).isSuccess) {
                return targetFile
            }
        }
        return null
    }

    /**
     * Downloads the APK file into app's private cache directory with progress reporting.
     */
    suspend fun downloadUpdate(
        context: Context,
        updateInfo: AppUpdateInfo,
        onProgress: (progress: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        // Automatically purge any stale downloads to save device storage
        purgeStaleUpdateFiles(context, updateInfo.versionName)

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val tempFile = File(updatesDir, "download_${updateInfo.versionCode}.tmp")
        val finalFile = getTargetApkFile(context, updateInfo)

        try {
            if (tempFile.exists()) tempFile.delete()

            val request = Request.Builder()
                .url(updateInfo.downloadUrl)
                .header("User-Agent", "Lectures-InAppUpdater/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Download failed with HTTP ${response.code}")
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                onProgress(progress)
                            }
                        }
                        output.flush()
                    }
                }
            }

            // 1. Verify SHA-256 if provided in metadata
            if (!updateInfo.sha256.isNullOrBlank()) {
                val matches = verifyChecksum(tempFile, updateInfo.sha256)
                if (!matches) {
                    tempFile.delete()
                    throw SecurityException("Downloaded APK SHA-256 checksum does not match expected release hash")
                }
            }

            // 2. Pre-flight Package Archive verification
            val packageCheck = verifyPackageArchive(context, tempFile)
            if (packageCheck.isFailure) {
                tempFile.delete()
                throw packageCheck.exceptionOrNull() ?: IllegalStateException("Package archive validation failed")
            }

            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(1f)
            Log.i(TAG, "Update successfully downloaded and verified: ${finalFile.absolutePath} (${finalFile.length()} bytes)")
            Result.success(finalFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update: ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Checks if the app has permission to request package installation.
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens Android System Settings directly to allow the user to toggle "Install unknown apps" once.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Launches the native Android package installer sheet for the downloaded APK.
     */
    fun triggerInstall(context: Context, apkFile: File): Result<Unit> {
        return try {
            // Pre-flight check
            val validation = verifyPackageArchive(context, apkFile)
            if (validation.isFailure) {
                return validation
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getTargetApkFile(context: Context, updateInfo: AppUpdateInfo): File {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        return File(updatesDir, "Lectures-v${updateInfo.versionName}.apk")
    }

    private fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val calculatedHash = digest.digest().joinToString("") { "%02x".format(it) }
            calculatedHash.equals(expectedSha256.trim(), ignoreCase = true)
        } catch (e: Exception) {
            Log.w(TAG, "Checksum verification error: ${e.message}")
            false
        }
    }

    private fun fetchFromVersionJson(url: String): Result<AppUpdateInfo?> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Lectures-InAppUpdater/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IllegalStateException("HTTP ${response.code} from version metadata"))
                }
                val body = response.body?.string() ?: return Result.failure(IllegalStateException("Empty response body"))
                val info = parseUpdateJson(body)
                Result.success(info)
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchFromVersionJson failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun fetchFromGitHubReleases(): Result<AppUpdateInfo?> {
        return try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Lectures-InAppUpdater/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val message = if (response.code == 403) {
                        "GitHub API rate limit exceeded (HTTP 403)"
                    } else {
                        "HTTP ${response.code} from GitHub Releases"
                    }
                    return Result.failure(IllegalStateException(message))
                }
                val body = response.body?.string() ?: return Result.failure(IllegalStateException("Empty response body"))
                val json = JSONObject(body)

                val tagName = json.optString("tag_name", "").removePrefix("v")
                val releaseNotes = json.optString("body", "Bug fixes and improvements")
                val publishedAt = json.optString("published_at", null)

                // Locate APK asset in release
                val assets = json.optJSONArray("assets") ?: return Result.success(null)
                var apkUrl: String? = null
                var apkSize = 0L

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }

                if (apkUrl.isNullOrBlank()) return Result.success(null)

                val parsedCode = parseVersionCodeFromTag(tagName)

                Result.success(
                    AppUpdateInfo(
                        versionCode = parsedCode,
                        versionName = tagName.ifBlank { "Latest" },
                        releaseNotes = releaseNotes,
                        downloadUrl = apkUrl,
                        fileSizeBytes = apkSize,
                        publishedAt = publishedAt
                    )
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchFromGitHubReleases failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun parseUpdateJson(jsonString: String): AppUpdateInfo? {
        return try {
            val root = JSONObject(jsonString)
            val obj = if (root.has("app_update")) root.getJSONObject("app_update") else root

            val vName = obj.optString("versionName", obj.optString("latest_version_name", ""))
            if (vName.isBlank()) return null

            var vCode = obj.optInt("versionCode", obj.optInt("latest_version_code", -1))
            if (vCode <= 0) {
                vCode = parseVersionCodeFromTag(vName)
            }

            val notes = obj.optString("releaseNotes", obj.optString("release_notes", "General improvements and fixes"))
            val url = obj.optString("downloadUrl", obj.optString("download_url", ""))
            if (url.isBlank()) return null

            val sha256 = if (obj.has("sha256")) obj.optString("sha256") else null
            val size = obj.optLong("fileSizeBytes", obj.optLong("file_size_bytes", 0L))
            val mandatory = obj.optBoolean("force_update", obj.optBoolean("isMandatory", false))
            val publishedAt = obj.optString("published_at", null)

            AppUpdateInfo(
                versionCode = vCode,
                versionName = vName,
                releaseNotes = notes,
                downloadUrl = url,
                fileSizeBytes = size,
                sha256 = sha256,
                isMandatory = mandatory,
                publishedAt = publishedAt
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseUpdateJson error: ${e.message}")
            null
        }
    }

    /**
     * Compares two semantic version strings (e.g. "1.1.2" vs "1.1.1").
     * Returns:
     *   > 0 if v1 is newer than v2
     *   < 0 if v1 is older than v2 (downgrade)
     *   0 if equal
     */
    fun compareSemVer(v1: String, v2: String): Int {
        val clean1 = v1.trim().removePrefix("v").substringBefore("-").substringBefore("+")
        val clean2 = v2.trim().removePrefix("v").substringBefore("-").substringBefore("+")

        val parts1 = clean1.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val parts2 = clean2.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }

        val maxLen = maxOf(parts1.size, parts2.size, 3)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }
        return 0
    }

    /**
     * Determines whether a remote release is strictly newer than the currently installed app.
     * Prevents any downgrades or re-installing the identical version.
     */
    fun isNewerRelease(
        remoteVersionName: String,
        remoteVersionCode: Int,
        localVersionName: String = BuildConfig.VERSION_NAME,
        localVersionCode: Int = BuildConfig.VERSION_CODE
    ): Boolean {
        val semVerCmp = compareSemVer(remoteVersionName, localVersionName)
        if (semVerCmp > 0) {
            return true // Strictly newer version name (e.g. 1.1.2 > 1.1.1)
        }
        if (semVerCmp < 0) {
            return false // Older version name (e.g. 1.1.0 < 1.1.1) -> DOWNGRADE FORBIDDEN
        }
        // Same semantic version: only allow if remote explicitly specifies a strictly higher versionCode
        return remoteVersionCode > localVersionCode
    }

    private fun parseVersionCodeFromTag(tag: String): Int {
        return try {
            val clean = tag.trim().removePrefix("v")
            val mainSemVer = clean.substringBefore("-").substringBefore("+")
            val parts = mainSemVer.split(".").mapNotNull { it.toIntOrNull() }
            val major = parts.getOrElse(0) { 0 }
            val minor = parts.getOrElse(1) { 0 }
            val patch = parts.getOrElse(2) { 0 }

            val suffixNum = if (clean.contains("-") || clean.contains("+")) {
                val suffix = clean.substringAfter("-", "").substringAfter("+", "")
                suffix.filter { it.isDigit() }.toIntOrNull() ?: 0
            } else 0

            (major * 1000000) + (minor * 10000) + (patch * 100) + suffixNum
        } catch (e: Exception) {
            0
        }
    }
}
