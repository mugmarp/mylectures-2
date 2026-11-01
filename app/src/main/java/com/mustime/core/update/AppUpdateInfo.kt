package com.mustime.core.update

/**
 * Metadata model describing an available application release update.
 */
data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val fileSizeBytes: Long = 0L,
    val sha256: String? = null,
    val isMandatory: Boolean = false,
    val publishedAt: String? = null
)
