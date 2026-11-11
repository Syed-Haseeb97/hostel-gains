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
    val showOnboarding: Boolean = false
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
            showOnboarding = !repository.hasApiKey()
        )
    )
    val uiState: StateFlow<ApiKeyUiState> = _uiState.asStateFlow()

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
                        showOnboarding = false,
                        errorMessage = null,
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
            showOnboarding = false,
            apiKey = "",
            errorMessage = null
        )
    }

    fun getStoredKey(): String? = repository.getApiKey()

    fun showOnboardingAgain() {
        _uiState.value = _uiState.value.copy(showOnboarding = true, apiKey = "", errorMessage = null)
    }
}
