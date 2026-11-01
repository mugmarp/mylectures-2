package com.mustime.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.mustime.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Manages in-app application updates outside Google Play Store.
 * Handles checking remote version metadata, in-app background/progress downloading,
 * SHA-256 verification, and seamless native 1-tap installation via FileProvider.
 */
object InAppUpdateManager {

    private const val TAG = "InAppUpdateManager"

    // Primary endpoint: raw version.json from project repo
    const val DEFAULT_VERSION_METADATA_URL =
        "https://raw.githubusercontent.com/mugmarp/mylectures/main/version.json"

    // Secondary fallback: GitHub Releases Latest API
    const val GITHUB_RELEASES_API_URL =
        "https://api.github.com/repos/mugmarp/mylectures/releases/latest"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Checks if a new app version is available.
     * Compares remote versionCode against local BuildConfig.VERSION_CODE.
     */
    suspend fun checkForUpdate(
        context: Context,
        customUrl: String? = null
    ): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val urlToFetch = customUrl ?: DEFAULT_VERSION_METADATA_URL
            var updateInfo = fetchFromVersionJson(urlToFetch)

            // Fallback to GitHub Releases API if version.json returned nothing
            if (updateInfo == null) {
                updateInfo = fetchFromGitHubReleases()
            }

            if (updateInfo == null) {
                return@withContext Result.success(null)
            }

            val currentVersionCode = BuildConfig.VERSION_CODE
            Log.d(TAG, "Local build versionCode=$currentVersionCode, Remote versionCode=${updateInfo.versionCode}")

            if (updateInfo.versionCode > currentVersionCode) {
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
     * Checks whether an update APK has already been downloaded and verified in cache.
     */
    fun getDownloadedUpdateApk(context: Context, updateInfo: AppUpdateInfo): File? {
        val targetFile = getTargetApkFile(context, updateInfo)
        if (targetFile.exists() && targetFile.length() > 0) {
            if (updateInfo.sha256.isNullOrBlank() || verifyChecksum(targetFile, updateInfo.sha256)) {
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

            // Verify SHA-256 if provided in metadata
            if (!updateInfo.sha256.isNullOrBlank()) {
                val matches = verifyChecksum(tempFile, updateInfo.sha256)
                if (!matches) {
                    tempFile.delete()
                    throw SecurityException("Downloaded APK SHA-256 checksum does not match expected release hash")
                }
            }

            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(1f)
            Log.i(TAG, "Update successfully downloaded: ${finalFile.absolutePath} (${finalFile.length()} bytes)")
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
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(IllegalStateException("APK file is missing or empty"))
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

    private fun fetchFromVersionJson(url: String): AppUpdateInfo? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Lectures-InAppUpdater/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                parseUpdateJson(body)
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchFromVersionJson skipped: ${e.message}")
            null
        }
    }

    private fun fetchFromGitHubReleases(): AppUpdateInfo? {
        return try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Lectures-InAppUpdater/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)

                val tagName = json.optString("tag_name", "").removePrefix("v")
                val releaseNotes = json.optString("body", "Bug fixes and improvements")
                val publishedAt = json.optString("published_at", null)

                // Locate APK asset in release
                val assets = json.optJSONArray("assets") ?: return null
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

                if (apkUrl.isNullOrBlank()) return null

                // Compute an integer version code from semantic version (e.g. 1.1.2 -> 10102) or parse
                val parsedCode = parseVersionCodeFromTag(tagName)

                AppUpdateInfo(
                    versionCode = parsedCode,
                    versionName = tagName.ifBlank { "Latest" },
                    releaseNotes = releaseNotes,
                    downloadUrl = apkUrl,
                    fileSizeBytes = apkSize,
                    publishedAt = publishedAt
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchFromGitHubReleases skipped: ${e.message}")
            null
        }
    }

    fun parseUpdateJson(jsonString: String): AppUpdateInfo? {
        return try {
            val root = JSONObject(jsonString)
            val obj = if (root.has("app_update")) root.getJSONObject("app_update") else root

            val vCode = obj.optInt("versionCode", obj.optInt("latest_version_code", -1))
            if (vCode <= 0) return null

            val vName = obj.optString("versionName", obj.optString("latest_version_name", "v$vCode"))
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

    private fun parseVersionCodeFromTag(tag: String): Int {
        val parts = tag.split(".").mapNotNull { it.filter { char -> char.isDigit() }.toIntOrNull() }
        return when (parts.size) {
            3 -> (parts[0] * 10000) + (parts[1] * 100) + parts[2]
            2 -> (parts[0] * 10000) + (parts[1] * 100)
            1 -> parts[0]
            else -> 1
        }
    }
}
