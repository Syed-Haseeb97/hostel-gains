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
import java.io.File

data class UpdateUiState(
    val isChecking: Boolean = false,
    val releaseInfo: AppReleaseInfo? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.Idle,
    val showInstallDialog: Boolean = false,
    val bannerMessage: String? = null
)

class UpdateViewModel(
    private val updateService: AppUpdateService = AppUpdateService(),
    private val currentVersion: String = "1.0.0",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /**
     * Called on app launch: checks in real time for updates.
     * If an update is available, automatically downloads the APK in background!
     */
    fun checkForUpdatesOnLaunch(context: Context) {
        if (_uiState.value.isChecking) return

        _uiState.value = _uiState.value.copy(isChecking = true)

        scope.launch {
            when (val result = updateService.checkForUpdate(currentVersion)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        releaseInfo = result.releaseInfo,
                        bannerMessage = "Updating to ${result.releaseInfo.tagName} in background..."
                    )
                    // Automatically trigger background download
                    startSilentBackgroundDownload(context, result.releaseInfo)
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
        _uiState.value = _uiState.value.copy(showInstallDialog = false)
    }

    fun dismissInstallDialog() {
        _uiState.value = _uiState.value.copy(showInstallDialog = false)
    }
}
