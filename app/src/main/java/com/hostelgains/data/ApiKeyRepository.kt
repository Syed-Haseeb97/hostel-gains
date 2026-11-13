package com.hostelgains.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure repository for the user's Google AI Studio API key.
 *
 * Uses Android EncryptedSharedPreferences (AES-256 GCM) backed by the Android Keystore
 * so the key is encrypted at rest and never written to disk in plaintext.
 *
 * Security hardening:
 * - Attempts self-healing recovery if the Keystore keyset becomes corrupted after an OS upgrade.
 * - Never falls back to unencrypted disk SharedPreferences; if hardware encryption is unavailable,
 *   holds the key strictly in volatile memory for the active session and purges any legacy
 *   plaintext fallback file.
 */
class ApiKeyRepository(context: Context) {

    companion object {
        private const val PREFS_FILE = "hostel_gains_secure_prefs"
        private const val LEGACY_FALLBACK_FILE = "hostel_gains_secure_prefs_fallback"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val MAX_KEY_LENGTH = 256
    }

    var isHardwareEncrypted: Boolean = true
        private set

    @Volatile
    private var volatileSessionKey: String? = null

    private val encryptedPrefs: SharedPreferences? = initEncryptedPrefs(context.applicationContext)

    private fun initEncryptedPrefs(appContext: Context): SharedPreferences? {
        // Purge any legacy plaintext fallback file created by older versions
        runCatching { appContext.deleteSharedPreferences(LEGACY_FALLBACK_FILE) }

        return try {
            createEncryptedPrefs(appContext)
        } catch (firstError: Exception) {
            // Attempt one-time recovery by clearing corrupted keyset preferences
            try {
                appContext.deleteSharedPreferences(PREFS_FILE)
                createEncryptedPrefs(appContext)
            } catch (secondError: Exception) {
                isHardwareEncrypted = false
                null
            }
        }
    }

    private fun createEncryptedPrefs(appContext: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Returns the saved API key, or null if not set. */
    fun getApiKey(): String? {
        val raw = if (encryptedPrefs != null) {
            encryptedPrefs.getString(KEY_GEMINI_API_KEY, null)
        } else {
            volatileSessionKey
        }
        return raw?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Returns true if an API key is currently stored. */
    fun hasApiKey(): Boolean = getApiKey() != null

    /**
     * Saves the user's API key into encrypted storage (or session-only memory if Keystore is unavailable).
     * @return true if the key is non-blank, within safe length bounds, and saved successfully.
     */
    fun saveApiKey(key: String): Boolean {
        val trimmed = key.trim()
        if (!isValidKeyFormat(trimmed)) return false

        if (encryptedPrefs != null) {
            encryptedPrefs.edit().putString(KEY_GEMINI_API_KEY, trimmed).apply()
        } else {
            volatileSessionKey = trimmed
        }
        return true
    }

    /** Removes the saved API key, reverting to Offline Mode. */
    fun clearApiKey() {
        volatileSessionKey = null
        encryptedPrefs?.edit()?.remove(KEY_GEMINI_API_KEY)?.apply()
    }

    /**
     * Validates that the key is non-empty, free of control/whitespace characters,
     * and within reasonable length bounds. Live validity is verified by [GeminiApiKeyValidator].
     */
    fun isValidKeyFormat(key: String): Boolean {
        val trimmed = key.trim()
        return trimmed.isNotEmpty() &&
            trimmed.length <= MAX_KEY_LENGTH &&
            trimmed.none { it.isWhitespace() || it.isISOControl() }
    }
}
