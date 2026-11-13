package com.hostelgains.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AppReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val isNewer: Boolean
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val releaseInfo: AppReleaseInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data class Downloading(val progressPercent: Int) : DownloadStatus
    data class ReadyToInstall(val apkFile: File, val releaseInfo: AppReleaseInfo) : DownloadStatus
    data class Failed(val error: String) : DownloadStatus
}

/**
 * Real-time update service querying GitHub Releases API for Syed-Haseeb97/hostel-gains.
 */
open class AppUpdateService(
    private val client: OkHttpClient = defaultClient,
    private val owner: String = "Syed-Haseeb97",
    private val repo: String = "hostel-gains"
) {

    companion object {
        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        /**
         * Compares semantic version strings like "1.0.1" vs "1.0.0" or "v1.0.2".
         * Returns true if remote is strictly greater than local.
         */
        fun isRemoteVersionNewer(localVersion: String, remoteTag: String): Boolean {
            val cleanLocal = localVersion.trim().removePrefix("v").removePrefix("V")
            val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")

            val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }
            val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(localParts.size, remoteParts.size)
            for (i in 0 until maxLen) {
                val l = localParts.getOrElse(i) { 0 }
                val r = remoteParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        }
    }

    open suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "HostelGains-Android-App")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    // No release published yet
                    return@withContext UpdateCheckResult.UpToDate
                }
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("Update check failed (HTTP ${response.code})")
                }

                val body = response.body.string()
                val parsed = parseReleaseJson(body)
                    ?: return@withContext UpdateCheckResult.Error("Could not parse release information")

                val isNewer = isRemoteVersionNewer(currentVersionName, parsed.tagName)
                if (isNewer && parsed.apkDownloadUrl.isNotBlank()) {
                    UpdateCheckResult.UpdateAvailable(parsed.copy(isNewer = true))
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        } catch (e: IOException) {
            UpdateCheckResult.Error("Network error while checking updates: ${e.message ?: "No connection"}")
        } catch (e: Exception) {
            UpdateCheckResult.Error("Error checking updates: ${e.message}")
        }
    }

    open suspend fun downloadApk(
        context: Context,
        releaseInfo: AppReleaseInfo,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(releaseInfo.apkDownloadUrl)
            .addHeader("User-Agent", "HostelGains-Android-App")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to download APK (HTTP ${response.code})"))
                }

                val responseBody = response.body
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updateDir, "hostel-gains-${releaseInfo.tagName}.apk")

                val totalBytes = responseBody.contentLength()
                var downloadedBytes = 0L

                responseBody.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0) {
                                val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                                onProgress(percent)
                            }
                        }
                        output.flush()
                    }
                }
                Result.success(apkFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Prompts the Android OS package installer using FileProvider.
     */
    fun triggerInstall(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    internal fun parseReleaseJson(json: String): AppReleaseInfo? {
        val tagRegex = "\"tag_name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val nameRegex = "\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val bodyRegex = "\"body\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        val browserDownloadRegex = "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"".toRegex()

        val tag = tagRegex.find(json)?.groupValues?.get(1) ?: return null
        val name = nameRegex.find(json)?.groupValues?.get(1) ?: tag
        val body = bodyRegex.find(json)?.groupValues?.get(1) ?: ""
        val apkUrl = browserDownloadRegex.find(json)?.groupValues?.get(1) ?: ""

        return AppReleaseInfo(
            tagName = tag,
            releaseName = name,
            releaseNotes = body.replace("\\n", "\n"),
            apkDownloadUrl = apkUrl,
            isNewer = false
        )
    }
}
