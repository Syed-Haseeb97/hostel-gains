package com.hostelgains.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateServiceTest {

    private val service = AppUpdateService()

    @Test
    fun testIsRemoteVersionNewer_majorBump() {
        assertTrue(AppUpdateService.isRemoteVersionNewer("1.0.0", "v2.0.0"))
        assertTrue(AppUpdateService.isRemoteVersionNewer("1.0.0", "2.0.0"))
    }

    @Test
    fun testIsRemoteVersionNewer_minorBump() {
        assertTrue(AppUpdateService.isRemoteVersionNewer("1.0.0", "v1.1.0"))
        assertTrue(AppUpdateService.isRemoteVersionNewer("1.0.2", "1.1.0"))
    }

    @Test
    fun testIsRemoteVersionNewer_patchBump() {
        assertTrue(AppUpdateService.isRemoteVersionNewer("1.0.0", "v1.0.1"))
        assertTrue(AppUpdateService.isRemoteVersionNewer("1.0.9", "1.0.10"))
    }

    @Test
    fun testIsRemoteVersionNewer_sameOrOlder_returnsFalse() {
        assertFalse(AppUpdateService.isRemoteVersionNewer("1.0.0", "v1.0.0"))
        assertFalse(AppUpdateService.isRemoteVersionNewer("1.1.0", "v1.0.5"))
        assertFalse(AppUpdateService.isRemoteVersionNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun testParseReleaseJson_validGitHubRelease() {
        val sampleJson = """
            {
              "tag_name": "v1.0.1",
              "name": "Hostel Gains v1.0.1 - Performance Polish",
              "body": "Minor background fixes and updated food database.",
              "assets": [
                {
                  "name": "app-debug.apk",
                  "browser_download_url": "https://github.com/Syed-Haseeb97/hostel-gains/releases/download/v1.0.1/app-debug.apk"
                }
              ]
            }
        """.trimIndent()

        val release = service.parseReleaseJson(sampleJson)
        assertNotNull(release)
        assertEquals("v1.0.1", release?.tagName)
        assertEquals("Hostel Gains v1.0.1 - Performance Polish", release?.releaseName)
        assertEquals("Minor background fixes and updated food database.", release?.releaseNotes)
        assertEquals(
            "https://github.com/Syed-Haseeb97/hostel-gains/releases/download/v1.0.1/app-debug.apk",
            release?.apkDownloadUrl
        )
    }

    @Test
    fun testParseReleaseJson_missingTag_returnsNull() {
        val malformedJson = """{ "name": "No Tag Here" }"""
        val release = service.parseReleaseJson(malformedJson)
        assertNull(release)
    }

    @Test
    fun testIsTrustedDownloadUrl_enforcesHttpsAndAllowedGitHubHosts() {
        assertTrue(
            AppUpdateService.isTrustedDownloadUrl(
                "https://github.com/Syed-Haseeb97/hostel-gains/releases/download/v1.3.0/app-release.apk"
            )
        )
        assertTrue(
            AppUpdateService.isTrustedDownloadUrl(
                "https://objects.githubusercontent.com/github-production-release-asset/app.apk"
            )
        )
        assertFalse(
            "Cleartext HTTP must be rejected",
            AppUpdateService.isTrustedDownloadUrl(
                "http://github.com/Syed-Haseeb97/hostel-gains/releases/download/v1.3.0/app.apk"
            )
        )
        assertFalse(
            "Untrusted third-party host must be rejected",
            AppUpdateService.isTrustedDownloadUrl("https://evil.example.com/app-debug.apk")
        )
    }

    @Test
    fun testSanitizeTagForFilename_stripsPathTraversalSequences() {
        val sanitized = AppUpdateService.sanitizeTagForFilename("../../etc/passwd")
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains(".."))
    }
}
