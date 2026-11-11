package com.hostelgains.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface KeyValidationResult {
    data object Valid : KeyValidationResult
    data class Invalid(val message: String) : KeyValidationResult
    data class NetworkError(val message: String) : KeyValidationResult
}

interface ApiKeyValidator {
    suspend fun validateKey(key: String): KeyValidationResult
}

/**
 * Real live validator that pings Google AI Studio's Gemini REST API
 * using a lightweight GET to `v1beta/models?key={key}`.
 *
 * - Returns [KeyValidationResult.Valid] if Google responds with HTTP 200.
 * - Extracts and returns Google's exact error message on HTTP 400/401/403.
 * - Gracefully reports network / DNS connectivity failures.
 */
class GeminiApiKeyValidator(
    private val client: OkHttpClient = defaultClient,
    private val baseUrl: String = DEFAULT_BASE_URL
) : ApiKeyValidator {

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }

    override suspend fun validateKey(key: String): KeyValidationResult = withContext(Dispatchers.IO) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            return@withContext KeyValidationResult.Invalid("API key cannot be empty.")
        }

        val url = if (baseUrl.contains("?")) "$baseUrl&key=$trimmed" else "$baseUrl?key=$trimmed"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", trimmed)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    KeyValidationResult.Valid
                } else {
                    val body = response.body.string()
                    val parsedError = parseGoogleErrorMessage(body, response.code)
                    KeyValidationResult.Invalid(parsedError)
                }
            }
        } catch (e: IOException) {
            KeyValidationResult.NetworkError(
                "Could not connect to Google AI Studio. Please check your internet connection."
            )
        } catch (e: Exception) {
            KeyValidationResult.NetworkError(
                "Verification failed: ${e.message ?: "Unexpected error"}"
            )
        }
    }

    internal fun parseGoogleErrorMessage(responseBody: String, httpCode: Int): String {
        // Example Google error payload:
        // { "error": { "code": 400, "message": "API key not valid. Please pass a valid API key.", "status": "INVALID_ARGUMENT" } }
        val messageRegex = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val match = messageRegex.find(responseBody)
        if (match != null) {
            val rawMsg = match.groupValues[1]
            return "Google AI Studio: $rawMsg"
        }
        return "Invalid API key (Google returned HTTP $httpCode)."
    }
}
