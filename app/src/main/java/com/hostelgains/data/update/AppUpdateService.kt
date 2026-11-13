package com.hostelgains.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import com.hostelgains.data.network.SharedHttpClient
import com.hostelgains.domain.util.SimpleJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class AppReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val isNewer: Boolean,
    val expectedSha256: String? = null
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
 * Hardened update service querying GitHub Releases API for Syed-Haseeb97/hostel-gains.
 *
 * Security hardening:
 * - Structured JSON parsing via [SimpleJsonParser] instead of fragile regular expressions.
 * - Strict HTTPS & GitHub host allowlist validation on [AppReleaseInfo.apkDownloadUrl].
 * - Filename sanitization and canonical path verification to prevent directory traversal.
 * - Optional SHA-256 digest verification during APK streaming download.
 * - Package archive validation prior to launching the Android package installer.
 */
open class AppUpdateService(
    private val client: OkHttpClient = defaultClient,
    private val owner: String = "Syed-Haseeb97",
    private val repo: String = "hostel-gains"
) {

    companion object {
        private val ALLOWED_DOWNLOAD_HOSTS = setOf(
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        )

        private val SHA256_REGEX = Regex("(?:sha256|SHA-256)[:=\\s]+([a-fA-F0-9]{64})")

        val defaultClient: OkHttpClient by lazy {
            SharedHttpClient.instance.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        /**
         * Compares semantic version strings like "1.0.1" vs "1.0.0" or "v1.0.2".
         * Returns true if remote is strictly greater than local.
         */
        fun isRemoteVersionNewer(localVersion: String, remoteTag: String): Boolean {
            val cleanLocal = localVersion.trim().removePrefix("v").removePrefix("V").substringBefore("-")
            val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V").substringBefore("-")

            val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }
            val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
            if (remoteParts.isEmpty()) return false

            val maxLen = maxOf(localParts.size, remoteParts.size)
            for (i in 0 until maxLen) {
                val l = localParts.getOrElse(i) { 0 }
                val r = remoteParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        }

        /**
         * Validates that the APK download URL uses HTTPS and points to an official GitHub release host.
         */
        fun isTrustedDownloadUrl(url: String): Boolean {
            val parsed = url.toHttpUrlOrNull() ?: return false
            return parsed.isHttps &&
                parsed.host.lowercase() in ALLOWED_DOWNLOAD_HOSTS &&
                parsed.encodedPath.lowercase().endsWith(".apk")
        }

        /**
         * Sanitizes a version tag for safe use in a local filename, stripping path separators.
         */
        fun sanitizeTagForFilename(tag: String): String {
            val cleaned = tag.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .replace("..", "_")
                .trim('_')
                .take(40)
            return cleaned.ifEmpty { "latest" }
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
                    return@withContext UpdateCheckResult.UpToDate
                }
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("Update check failed (HTTP ${response.code})")
                }

                val body = response.body.string()
                val parsed = parseReleaseJson(body)
                    ?: return@withContext UpdateCheckResult.Error("Could not parse release information")

                val isNewer = isRemoteVersionNewer(currentVersionName, parsed.tagName)
                if (isNewer && parsed.apkDownloadUrl.isNotBlank() && isTrustedDownloadUrl(parsed.apkDownloadUrl)) {
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

    suspend fun downloadApk(
        context: Context,
        releaseInfo: AppReleaseInfo,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!isTrustedDownloadUrl(releaseInfo.apkDownloadUrl)) {
            return@withContext Result.failure(
                SecurityException("Refusing to download APK from untrusted URL: ${releaseInfo.apkDownloadUrl}")
            )
        }

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
                val safeTag = sanitizeTagForFilename(releaseInfo.tagName)
                val apkFile = File(updateDir, "hostel-gains-$safeTag.apk")

                // Canonical path check against directory traversal
                val canonicalDir = updateDir.canonicalFile
                val canonicalTarget = apkFile.canonicalFile
                if (!canonicalTarget.path.startsWith(canonicalDir.path + File.separator)) {
                    return@withContext Result.failure(
                        SecurityException("Invalid target file path outside update cache directory")
                    )
                }

                val totalBytes = responseBody.contentLength()
                var downloadedBytes = 0L
                val digest = MessageDigest.getInstance("SHA-256")

                responseBody.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0) {
                                val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                onProgress(percent)
                            }
                        }
                        output.flush()
                    }
                }

                val expectedHash = releaseInfo.expectedSha256?.trim()?.lowercase()
                if (!expectedHash.isNullOrEmpty()) {
                    val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
                    if (actualHash != expectedHash) {
                        apkFile.delete()
                        return@withContext Result.failure(
                            SecurityException("APK SHA-256 mismatch (expected $expectedHash, got $actualHash)")
                        )
                    }
                }

                Result.success(apkFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifies that the downloaded file is a valid APK for this application package,
     * then prompts the Android OS package installer using FileProvider.
     */
    fun triggerInstall(context: Context, apkFile: File) {
        if (!apkFile.exists() || apkFile.length() == 0L) return

        val pm = context.packageManager
        val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_META_DATA)
        if (archiveInfo != null && archiveInfo.packageName != context.packageName) {
            apkFile.delete()
            throw SecurityException("Downloaded APK package name (${archiveInfo.packageName}) does not match installed app (${context.packageName})")
        }

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
        val root = SimpleJsonParser.parse(json) as? SimpleJsonParser.Node.ObjectNode ?: return null
        val tag = root.optString("tag_name").trim()
        if (tag.isEmpty()) return null

        val name = root.optString("name").ifBlank { tag }
        val body = root.optString("body")

        var apkUrl = ""
        var assetDigest: String? = null
        val assets = (root["assets"] as? SimpleJsonParser.Node.ArrayNode)?.list
        if (assets != null) {
            for (item in assets) {
                val assetObj = item as? SimpleJsonParser.Node.ObjectNode ?: continue
                val downloadUrl = assetObj.optString("browser_download_url")
                if (downloadUrl.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = downloadUrl
                    val rawDigest = assetObj.optString("digest")
                    if (rawDigest.startsWith("sha256:", ignoreCase = true)) {
                        assetDigest = rawDigest.substringAfter(":").trim()
                    }
                    break
                }
            }
        }

        val expectedSha256 = assetDigest ?: SHA256_REGEX.find(body)?.groupValues?.get(1)

        return AppReleaseInfo(
            tagName = tag,
            releaseName = name,
            releaseNotes = body,
            apkDownloadUrl = apkUrl,
            isNewer = false,
            expectedSha256 = expectedSha256
        )
    }
}
