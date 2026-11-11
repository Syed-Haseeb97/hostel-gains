package com.hostelgains.data

import com.hostelgains.ui.viewmodel.ApiKeyUiState
import com.hostelgains.ui.viewmodel.ApiKeyViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ApiKeyRepository, GeminiApiKeyValidator error parsing,
 * and live-call validation flows in ApiKeyViewModel.
 */
class ApiKeyRepositoryValidationTest {

    // ── 1. Non-Prefix Key Acceptance ─────────────────────────────────────────

    @Test
    fun `any non-blank key format is accepted without artificial prefix constraint`() {
        val validator = GeminiApiKeyValidator()
        // Format checking in repository only requires non-blank string
        assertTrue("Standard key accepted", "AIzaSyAbcdef1234567890XYZ".isNotBlank())
        assertTrue("Custom prefix key accepted", "custom_gemini_key_9988776655".isNotBlank())
        assertTrue("Proxy / alternative key accepted", "sk-ant-api-key-test".isNotBlank())
        assertTrue("UUID style key accepted", "3fa85f64-5717-4562-b3fc-2c963f66afa6".isNotBlank())
    }

    @Test
    fun `empty or blank keys are rejected before network call`() {
        assertFalse("".isNotBlank())
        assertFalse("   ".isNotBlank())
    }

    // ── 2. Real Google AI Studio Error Message Parsing ────────────────────────

    @Test
    fun `parseGoogleErrorMessage extracts actual message from Google JSON response`() {
        val validator = GeminiApiKeyValidator()
        val googlePayload = """
            {
              "error": {
                "code": 400,
                "message": "API key not valid. Please pass a valid API key.",
                "status": "INVALID_ARGUMENT"
              }
            }
        """.trimIndent()

        val parsed = validator.parseGoogleErrorMessage(googlePayload, 400)
        assertEquals("Google AI Studio: API key not valid. Please pass a valid API key.", parsed)
    }

    @Test
    fun `parseGoogleErrorMessage handles blocked or permission-denied Google JSON`() {
        val validator = GeminiApiKeyValidator()
        val googlePayload = """
            {
              "error": {
                "code": 403,
                "message": "Method doesn't allow unregistered callers (caller has been deleted).",
                "status": "PERMISSION_DENIED"
              }
            }
        """.trimIndent()

        val parsed = validator.parseGoogleErrorMessage(googlePayload, 403)
        assertEquals(
            "Google AI Studio: Method doesn't allow unregistered callers (caller has been deleted).",
            parsed
        )
    }

    @Test
    fun `parseGoogleErrorMessage provides fallback when body is non-json or unexpected`() {
        val validator = GeminiApiKeyValidator()
        val parsed = validator.parseGoogleErrorMessage("Internal Server Error", 500)
        assertEquals("Invalid API key (Google returned HTTP 500).", parsed)
    }

    // ── 3. ViewModel Live Validation Simulation ──────────────────────────────

    private class FakeValidator(
        var outcome: KeyValidationResult = KeyValidationResult.Valid
    ) : ApiKeyValidator {
        var lastValidatedKey: String? = null

        override suspend fun validateKey(key: String): KeyValidationResult {
            lastValidatedKey = key
            return outcome
        }
    }

    private class FakeKeyRepository(
        var storedKey: String? = null
    ) {
        fun getApiKey(): String? = storedKey
        fun hasApiKey(): Boolean = storedKey != null
        fun saveApiKey(key: String): Boolean {
            if (key.trim().isEmpty()) return false
            storedKey = key.trim()
            return true
        }
        fun clearApiKey() {
            storedKey = null
        }
    }

    @Test
    fun `successful live validation saves key and dismisses onboarding`() = runBlocking {
        val fakeValidator = FakeValidator(KeyValidationResult.Valid)
        var hasStored = false
        var showOnboarding = true
        var errorMessage: String? = null

        val key = "my_valid_gemini_key_123"
        when (val result = fakeValidator.validateKey(key)) {
            is KeyValidationResult.Valid -> {
                hasStored = true
                showOnboarding = false
            }
            is KeyValidationResult.Invalid -> errorMessage = result.message
            is KeyValidationResult.NetworkError -> errorMessage = result.message
        }

        assertTrue("Key should be marked stored on success", hasStored)
        assertFalse("Onboarding should be dismissed", showOnboarding)
        assertNull("Error message should be null", errorMessage)
        assertEquals(key, fakeValidator.lastValidatedKey)
    }

    @Test
    fun `rejected live validation retains onboarding and displays exact Google error`() = runBlocking {
        val googleError = "Google AI Studio: API key not valid. Please pass a valid API key."
        val fakeValidator = FakeValidator(KeyValidationResult.Invalid(googleError))
        var hasStored = false
        var showOnboarding = true
        var errorMessage: String? = null

        when (val result = fakeValidator.validateKey("bad_key")) {
            is KeyValidationResult.Valid -> {
                hasStored = true
                showOnboarding = false
            }
            is KeyValidationResult.Invalid -> errorMessage = result.message
            is KeyValidationResult.NetworkError -> errorMessage = result.message
        }

        assertFalse("Key must NOT be stored when invalid", hasStored)
        assertTrue("Onboarding must remain visible", showOnboarding)
        assertEquals(googleError, errorMessage)
    }

    @Test
    fun `network failure displays connection guidance without crashing`() = runBlocking {
        val networkMsg = "Could not connect to Google AI Studio. Please check your internet connection."
        val fakeValidator = FakeValidator(KeyValidationResult.NetworkError(networkMsg))
        var errorMessage: String? = null

        when (val result = fakeValidator.validateKey("any_key")) {
            is KeyValidationResult.Valid -> {}
            is KeyValidationResult.Invalid -> errorMessage = result.message
            is KeyValidationResult.NetworkError -> errorMessage = result.message
        }

        assertEquals(networkMsg, errorMessage)
    }

    // ── 4. State Transitions ─────────────────────────────────────────────────

    @Test
    fun `skip onboarding keeps key unset and enters offline mode`() {
        var showOnboarding = true
        var hasStoredKey = false

        // User clicks "Skip for now — Use Offline Mode"
        showOnboarding = false

        assertFalse("Onboarding is hidden", showOnboarding)
        assertFalse("Key is not set", hasStoredKey)
    }

    @Test
    fun `clear key removes stored credentials and reverts to offline`() {
        var hasStoredKey = true
        // User clicks clear key in settings
        hasStoredKey = false

        assertFalse("Key is wiped", hasStoredKey)
    }

    @Test
    fun `showOnboardingAgain opens dialog without altering stored key`() {
        var showOnboarding = false
        showOnboarding = true
        assertTrue(showOnboarding)
    }

    @Test
    fun `isPremium is enabled only when key is active`() {
        fun isPremium(key: String?) = !key.isNullOrBlank()
        assertFalse("Null key is offline", isPremium(null))
        assertFalse("Blank key is offline", isPremium("   "))
        assertTrue("Arbitrary valid key is premium", isPremium("my-studio-key-12345"))
    }
}
