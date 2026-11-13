package com.hostelgains.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelgains.data.update.AppReleaseInfo
import com.hostelgains.data.update.AppUpdateService
import com.hostelgains.data.update.DownloadStatus
import com.hostelgains.data.update.UpdateCheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateUiState(
    val isChecking: Boolean = false,
    val releaseInfo: AppReleaseInfo? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.Idle,
    val showInstallDialog: Boolean = false,
    val bannerMessage: String? = null
)

class UpdateViewModel(
    private val updateService: AppUpdateService = AppUpdateService(),
    private val currentVersion: String = DEFAULT_VERSION_NAME,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    companion object {
        const val DEFAULT_VERSION_NAME = "1.3.0"
    }

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /**
     * Checks in real time for updates on launch using the installed package's real versionName
     * (falling back to [currentVersion] in unit test environments).
     */
    fun checkForUpdatesOnLaunch(context: Context? = null) {
        if (_uiState.value.isChecking) return

        val resolvedVersion = context?.let { resolveInstalledVersionName(it) } ?: currentVersion
        _uiState.value = _uiState.value.copy(isChecking = true)

        scope.launch {
            when (val result = updateService.checkForUpdate(resolvedVersion)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    // Double check to prevent infinite install loops
                    val isReallyNewer = AppUpdateService.isRemoteVersionNewer(resolvedVersion, result.releaseInfo.tagName)
                    if (!isReallyNewer) {
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            releaseInfo = null,
                            bannerMessage = null
                        )
                        return@launch
                    }

                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        releaseInfo = result.releaseInfo,
                        bannerMessage = "Updating to ${result.releaseInfo.tagName} in background..."
                    )
                    if (context != null) {
                        startSilentBackgroundDownload(context, result.releaseInfo)
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        releaseInfo = null,
                        bannerMessage = null
                    )
                }
                is UpdateCheckResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isChecking = false
                    )
                }
            }
        }
    }

    private fun resolveInstalledVersionName(context: Context): String? {
        return try {
            val pkgInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager?.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager?.getPackageInfo(context.packageName, 0)
            }
            pkgInfo?.versionName?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun startSilentBackgroundDownload(context: Context, releaseInfo: AppReleaseInfo) {
        _uiState.value = _uiState.value.copy(
            downloadStatus = DownloadStatus.Downloading(0)
        )

        scope.launch {
            val downloadResult = updateService.downloadApk(context, releaseInfo) { progress ->
                _uiState.value = _uiState.value.copy(
                    downloadStatus = DownloadStatus.Downloading(progress),
                    bannerMessage = "Downloading ${releaseInfo.tagName} ($progress%)..."
                )
            }

            downloadResult.onSuccess { apkFile ->
                _uiState.value = _uiState.value.copy(
                    downloadStatus = DownloadStatus.ReadyToInstall(apkFile, releaseInfo),
                    showInstallDialog = true,
                    bannerMessage = "Update ${releaseInfo.tagName} ready!"
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    downloadStatus = DownloadStatus.Failed(err.message ?: "Download failed"),
                    bannerMessage = null
                )
            }
        }
    }

    fun installDownloadedUpdate(context: Context) {
        val ready = _uiState.value.downloadStatus as? DownloadStatus.ReadyToInstall ?: return
        runCatching {
            updateService.triggerInstall(context, ready.apkFile)
        }
        _uiState.value = _uiState.value.copy(showInstallDialog = false)
    }

    fun dismissInstallDialog() {
        _uiState.value = _uiState.value.copy(showInstallDialog = false)
    }
}
