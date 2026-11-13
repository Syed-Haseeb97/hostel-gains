package com.hostelgains.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelgains.app.BuildConfig
import com.hostelgains.data.update.AppReleaseInfo
import com.hostelgains.data.update.AppUpdateService
import com.hostelgains.data.update.DownloadStatus
import com.hostelgains.data.update.UpdateCheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class UpdateUiState(
    val isChecking: Boolean = false,
    val currentInstalledVersion: String = "",
    val releaseInfo: AppReleaseInfo? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.Idle,
    val showInstallDialog: Boolean = false,
    val bannerMessage: String? = null
)

class UpdateViewModel(
    private val updateService: AppUpdateService = AppUpdateService(),
    private val versionOverride: String? = null,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    fun getInstalledVersion(context: Context? = null): String {
        if (!versionOverride.isNullOrBlank()) {
            return versionOverride
        }
        if (context == null) {
            return BuildConfig.VERSION_NAME
        }
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    /**
     * Called on app launch: checks in real time for updates using the dynamically
     * detected installed version name.
     * If a newer release is found, downloads the APK in the background.
     */
    fun checkForUpdatesOnLaunch(context: Context? = null) {
        if (_uiState.value.isChecking) return

        val currentVersion = getInstalledVersion(context)
        _uiState.value = _uiState.value.copy(
            isChecking = true,
            currentInstalledVersion = currentVersion
        )

        scope.launch {
            when (val result = updateService.checkForUpdate(currentVersion)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    // Double-check semantic version comparison to prevent infinite prompt loops
                    val isReallyNewer = AppUpdateService.isRemoteVersionNewer(currentVersion, result.releaseInfo.tagName)
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
        updateService.triggerInstall(context, ready.apkFile)
        _uiState.value = _uiState.value.copy(
            showInstallDialog = false,
            bannerMessage = null
        )
    }

    fun dismissInstallDialog() {
        _uiState.value = _uiState.value.copy(
            showInstallDialog = false,
            bannerMessage = null
        )
    }
}
