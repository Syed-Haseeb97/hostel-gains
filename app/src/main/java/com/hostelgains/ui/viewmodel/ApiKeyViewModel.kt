package com.hostelgains.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelgains.data.ApiKeyRepository
import com.hostelgains.data.ApiKeyValidator
import com.hostelgains.data.GeminiApiKeyValidator
import com.hostelgains.data.KeyValidationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApiKeyUiState(
    val apiKey: String = "",
    val isKeyVisible: Boolean = false,
    val hasStoredKey: Boolean = false,
    val isValidating: Boolean = false,
    val errorMessage: String? = null,
    val showOnboarding: Boolean = false,
    val isKeyValid: Boolean = false,
    val showApiKeyReminder: Boolean = false,
    val statusMessage: String? = null
)

class ApiKeyViewModel(
    private val repository: ApiKeyRepository,
    private val validator: ApiKeyValidator = GeminiApiKeyValidator(),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        ApiKeyUiState(
            hasStoredKey = repository.hasApiKey(),
            showOnboarding = !repository.hasApiKey(),
            showApiKeyReminder = !repository.hasApiKey(),
            statusMessage = if (!repository.hasApiKey()) "AI Key Needed" else "Checking AI Key..."
        )
    )
    val uiState: StateFlow<ApiKeyUiState> = _uiState.asStateFlow()

    init {
        checkApiKeyOnLaunch()
    }

    /**
     * Checks on every app launch whether the stored Gemini API key is valid.
     * If absent or invalid, prompts and reminds the user prominently.
     */
    fun checkApiKeyOnLaunch() {
        val key = repository.getApiKey()?.trim()
        if (key.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(
                hasStoredKey = false,
                isKeyValid = false,
                showApiKeyReminder = true,
                statusMessage = "AI Key Needed"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isValidating = true,
            statusMessage = "Verifying Gemini AI..."
        )

        scope.launch {
            when (val result = validator.validateKey(key)) {
                is KeyValidationResult.Valid -> {
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        hasStoredKey = true,
                        isKeyValid = true,
                        showApiKeyReminder = false,
                        errorMessage = null,
                        statusMessage = "🟢 Gemini AI Connected"
                    )
                }
                is KeyValidationResult.Invalid -> {
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        hasStoredKey = true,
                        isKeyValid = false,
                        showApiKeyReminder = true,
                        errorMessage = result.message,
                        statusMessage = "⚠️ AI Key Invalid"
                    )
                }
                is KeyValidationResult.NetworkError -> {
                    // Stored key is intact; offline network
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        hasStoredKey = true,
                        isKeyValid = true,
                        showApiKeyReminder = false,
                        statusMessage = "🟢 Gemini AI (Stored Key)"
                    )
                }
            }
        }
    }

    fun dismissApiKeyReminder() {
        _uiState.value = _uiState.value.copy(showApiKeyReminder = false)
    }

    fun onKeyInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value, errorMessage = null)
    }

    fun toggleKeyVisibility() {
        _uiState.value = _uiState.value.copy(isKeyVisible = !_uiState.value.isKeyVisible)
    }

    /**
     * Performs a real live validation call to Google AI Studio.
     * If Google responds with HTTP 200, the key is saved and premium features are unlocked.
     * If Google responds with an error, the exact error from Google is surfaced in the UI.
     */
    fun validateAndSaveKey(onSuccess: () -> Unit = {}) {
        val key = _uiState.value.apiKey.trim()
        if (key.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter your Google AI Studio API key."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isValidating = true, errorMessage = null)

        scope.launch {
            when (val result = validator.validateKey(key)) {
                is KeyValidationResult.Valid -> {
                    repository.saveApiKey(key)
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        hasStoredKey = true,
                        isKeyValid = true,
                        showApiKeyReminder = false,
                        showOnboarding = false,
                        errorMessage = null,
                        statusMessage = "🟢 Gemini AI Connected",
                        apiKey = ""
                    )
                    onSuccess()
                }
                is KeyValidationResult.Invalid -> {
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        errorMessage = result.message
                    )
                }
                is KeyValidationResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun skipOnboarding() {
        _uiState.value = _uiState.value.copy(showOnboarding = false)
    }

    fun clearKey() {
        repository.clearApiKey()
        _uiState.value = _uiState.value.copy(
            hasStoredKey = false,
            isKeyValid = false,
            showApiKeyReminder = true,
            showOnboarding = false,
            apiKey = "",
            errorMessage = null,
            statusMessage = "AI Key Needed"
        )
    }

    fun getStoredKey(): String? = repository.getApiKey()

    fun showOnboardingAgain() {
        _uiState.value = _uiState.value.copy(showOnboarding = true, apiKey = "", errorMessage = null)
    }
}
