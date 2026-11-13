package com.hostelgains.ui.viewmodel

import com.hostelgains.data.update.AppReleaseInfo
import com.hostelgains.data.update.AppUpdateService
import com.hostelgains.data.update.UpdateCheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateViewModelTest {

    @Test
    fun testUpdateViewModel_whenAlreadyOnLatestVersion_doesNotTriggerDownloadOrDialog() {
        val fakeService = object : AppUpdateService() {
            override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
                val isNewer = isRemoteVersionNewer(currentVersionName, "v1.3.0")
                return if (isNewer) {
                    UpdateCheckResult.UpdateAvailable(
                        AppReleaseInfo("v1.3.0", "1.3.0", "", "https://example.com/app.apk", true)
                    )
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        }

        // Simulating current installed version is already 1.3.0
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = UpdateViewModel(
            updateService = fakeService,
            currentVersion = "1.3.0",
            externalScope = testScope
        )

        viewModel.checkForUpdatesOnLaunch()

        val state = viewModel.uiState.value
        assertFalse("Must not show install dialog if already up to date", state.showInstallDialog)
        assertNull("Release info must be null", state.releaseInfo)
        assertNull("Banner message must be null", state.bannerMessage)
    }

    @Test
    fun testUpdateViewModel_whenOlderVersion_detectsUpdate() {
        val fakeService = object : AppUpdateService() {
            override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
                val isNewer = isRemoteVersionNewer(currentVersionName, "v1.3.0")
                return if (isNewer) {
                    UpdateCheckResult.UpdateAvailable(
                        AppReleaseInfo("v1.3.0", "1.3.0", "", "https://example.com/app.apk", true)
                    )
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        }

        // Simulating older installed version 1.1.0
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = UpdateViewModel(
            updateService = fakeService,
            currentVersion = "1.1.0",
            externalScope = testScope
        )

        viewModel.checkForUpdatesOnLaunch()

        val state = viewModel.uiState.value
        assertEquals("v1.3.0", state.releaseInfo?.tagName)
    }

    @Test
    fun testUpdateViewModel_doubleCheckPreventsInfiniteLoopWhenVersionMatchesTag() {
        // Simulates server returning UpdateAvailable even though version is same
        val fakeService = object : AppUpdateService() {
            override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
                return UpdateCheckResult.UpdateAvailable(
                    AppReleaseInfo("v1.3.0", "1.3.0", "", "https://example.com/app.apk", true)
                )
            }
        }

        // App is already on 1.3.0
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = UpdateViewModel(
            updateService = fakeService,
            currentVersion = "1.3.0",
            externalScope = testScope
        )

        viewModel.checkForUpdatesOnLaunch()

        val state = viewModel.uiState.value
        // Even if server said UpdateAvailable, double check prevents it!
        assertFalse(state.showInstallDialog)
        assertNull(state.releaseInfo)
        assertNull(state.bannerMessage)
    }
}
