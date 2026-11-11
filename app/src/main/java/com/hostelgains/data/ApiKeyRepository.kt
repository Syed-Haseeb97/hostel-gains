package com.hostelgains.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure repository for the user's Google AI Studio API key.
 *
 * Uses Android EncryptedSharedPreferences (AES-256 GCM) so the key is encrypted
 * at rest and never written to disk in plaintext.
 *
 * Fallback: on devices where EncryptedSharedPreferences cannot be initialised
 * (e.g. emulators without hardware keystore), falls back to private SharedPreferences.
 */
class ApiKeyRepository(context: Context) {

    companion object {
        private const val PREFS_FILE = "hostel_gains_secure_prefs"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    var isHardwareEncrypted: Boolean = true
        private set

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        isHardwareEncrypted = false
        context.getSharedPreferences(PREFS_FILE + "_fallback", Context.MODE_PRIVATE)
    }

    /** Returns the saved API key, or null if not set. */
    fun getApiKey(): String? {
        val key = prefs.getString(KEY_GEMINI_API_KEY, null)
        return if (key.isNullOrBlank()) null else key
    }

    /** Returns true if an API key is currently stored. */
    fun hasApiKey(): Boolean = getApiKey() != null

    /**
     * Saves the user's API key.
     * @return true if the key is non-blank and saved successfully.
     */
    fun saveApiKey(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return false
        prefs.edit().putString(KEY_GEMINI_API_KEY, trimmed).apply()
        return true
    }

    /** Removes the saved API key, reverting to Offline Mode. */
    fun clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    /**
     * Basic non-empty check. The true validity is determined by live network call.
     */
    fun isValidKeyFormat(key: String): Boolean {
        return key.trim().isNotEmpty()
    }
}
