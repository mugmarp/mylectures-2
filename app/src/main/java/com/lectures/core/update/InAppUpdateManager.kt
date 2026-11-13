package com.lectures.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.lectures.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manages checking for remote application releases and directing users to
 * the official HTTPS release or store page via [Intent.ACTION_VIEW].
 */
object InAppUpdateManager {

    private const val TAG = "InAppUpdateManager"

    // Primary endpoint: raw version.json from project repo
    const val DEFAULT_VERSION_METADATA_URL =
        "https://raw.githubusercontent.com/mugmarp/mylectures/main/version.json"

    // Secondary fallback: GitHub Releases Latest API
    const val GITHUB_RELEASES_API_URL =
        "https://api.github.com/repos/mugmarp/mylectures/releases/latest"

    const val FALLBACK_RELEASES_WEB_URL =
        "https://github.com/mugmarp/mylectures/releases/latest"

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
     * Validates that a remote update or metadata URL strictly uses HTTPS with a non-blank host.
     */
    fun isSecureHttpsUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        return try {
            val parsed = Uri.parse(rawUrl.trim())
            parsed.scheme.equals("https", ignoreCase = true) && !parsed.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
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
        purgeStaleUpdateFiles(context)

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

            Log.d(
                TAG,
                "Local build ${BuildConfig.VERSION_NAME} (code=${BuildConfig.VERSION_CODE}), " +
                    "Remote release ${updateInfo.versionName} (code=${updateInfo.versionCode}), isNewer=$isNewer"
            )

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
     * Cleans up any legacy cached update files from previous builds.
     */
    fun purgeStaleUpdateFiles(context: Context) {
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (updatesDir.exists() && updatesDir.isDirectory) {
                updatesDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to purge legacy update cache: ${e.message}")
        }
    }

    /**
     * Opens the verified HTTPS update/release page in the user's browser or store app.
     */
    fun openUpdatePage(context: Context, updateInfo: AppUpdateInfo): Result<Unit> {
        val targetUrl = updateInfo.downloadUrl.takeIf { isSecureHttpsUrl(it) }
            ?: return Result.failure(SecurityException("Insecure or invalid update URL rejected"))

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open release URL: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun fetchFromVersionJson(url: String): Result<AppUpdateInfo?> {
        if (!isSecureHttpsUrl(url)) {
            return Result.failure(SecurityException("Metadata URL must use HTTPS"))
        }
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Lectures-Updater/${BuildConfig.VERSION_NAME}")
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
                .header("User-Agent", "Lectures-Updater/${BuildConfig.VERSION_NAME}")
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
                val publishedAt = json.optString("published_at", "").ifBlank { null }
                val releasePageUrl = json.optString("html_url", "").ifBlank { FALLBACK_RELEASES_WEB_URL }
                if (!isSecureHttpsUrl(releasePageUrl)) {
                    return Result.failure(SecurityException("Release URL must use HTTPS"))
                }

                val parsedCode = parseVersionCodeFromTag(tagName)

                Result.success(
                    AppUpdateInfo(
                        versionCode = parsedCode,
                        versionName = tagName.ifBlank { "Latest" },
                        releaseNotes = releaseNotes,
                        downloadUrl = releasePageUrl,
                        fileSizeBytes = 0L,
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
            if (!isSecureHttpsUrl(url)) return null

            val sha256 = if (obj.has("sha256")) obj.optString("sha256").ifBlank { null } else null
            val size = obj.optLong("fileSizeBytes", obj.optLong("file_size_bytes", 0L))
            val mandatory = obj.optBoolean("force_update", obj.optBoolean("isMandatory", false))
            val publishedAt = obj.optString("published_at", "").ifBlank { null }

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
     * Returns > 0 if v1 is newer than v2, < 0 if older, 0 if equal.
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
        if (semVerCmp > 0) return true
        if (semVerCmp < 0) return false
        return remoteVersionCode > localVersionCode
    }

    fun parseVersionCodeFromTag(tag: String): Int {
        return try {
            val clean = tag.trim().removePrefix("v")
            val mainSemVer = clean.substringBefore("-").substringBefore("+")
            val parts = mainSemVer.split(".").mapNotNull { it.filter { c -> c.isDigit() }.take(4).toIntOrNull() }
            val major = parts.getOrElse(0) { 0 }.coerceIn(0, 999)
            val minor = parts.getOrElse(1) { 0 }.coerceIn(0, 99)
            val patch = parts.getOrElse(2) { 0 }.coerceIn(0, 99)

            val suffixNum = if (clean.contains("-") || clean.contains("+")) {
                val suffix = if (clean.contains("-")) clean.substringAfter("-") else clean.substringAfter("+")
                (suffix.filter { it.isDigit() }.take(2).toIntOrNull() ?: 0).coerceIn(0, 99)
            } else 0

            (major * 1_000_000) + (minor * 10_000) + (patch * 100) + suffixNum
        } catch (_: Exception) {
            0
        }
    }
}
