package com.aihubmax.features.api_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihubmax.core.network.DiscoveredModel
import com.aihubmax.core.network.ProviderRegistry
import com.aihubmax.core.network.ProviderSpec
import com.aihubmax.core.network.ProviderValidationResult
import com.aihubmax.core.security.SecureApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderUiItem(
    val spec: ProviderSpec,
    val isConnected: Boolean = false,
    val discoveredModels: List<DiscoveredModel> = emptyList(),
    val lastError: String? = null,
    val isTesting: Boolean = false
)

data class ApiManagerUiState(
    val providers: List<ProviderUiItem> = emptyList(),
    val selectedProviderId: String = "gemini",
    val apiKeyDraft: String = "",
    val statusMessage: String? = null
)

@HiltViewModel
class ApiManagerViewModel @Inject constructor(
    private val registry: ProviderRegistry,
    private val keyStore: SecureApiKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ApiManagerUiState(
            providers = registry.adapters.map { ProviderUiItem(spec = it.spec) }
        )
    )
    val uiState: StateFlow<ApiManagerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            keyStore.connectedProviderIds.collect { connectedIds ->
                _uiState.update { state ->
                    state.copy(
                        providers = state.providers.map { item ->
                            item.copy(isConnected = item.spec.id in connectedIds)
                        }
                    )
                }
            }
        }
    }

    fun selectProvider(providerId: String) {
        _uiState.update {
            it.copy(selectedProviderId = providerId, apiKeyDraft = "", statusMessage = null)
        }
    }

    fun onApiKeyChanged(value: String) {
        _uiState.update { it.copy(apiKeyDraft = value, statusMessage = null) }
    }

    fun onPasteFromClipboard(clipboardText: String?) {
        val cleaned = clipboardText?.trim().orEmpty()
        if (cleaned.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Clipboard is empty") }
            return
        }
        _uiState.update { it.copy(apiKeyDraft = cleaned, statusMessage = "Pasted from clipboard") }
    }

    fun validateAndConnect() {
        val current = _uiState.value
        val providerId = current.selectedProviderId
        val rawKey = current.apiKeyDraft.trim()
        if (rawKey.isBlank()) return

        val adapter = registry.getAdapter(providerId) ?: return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    providers = state.providers.map {
                        if (it.spec.id == providerId) it.copy(isTesting = true, lastError = null) else it
                    },
                    statusMessage = null
                )
            }

            when (val result = adapter.validateAndDiscoverModels(rawKey)) {
                is ProviderValidationResult.Success -> {
                    keyStore.saveApiKey(providerId, rawKey)
                    _uiState.update { state ->
                        state.copy(
                            apiKeyDraft = "",
                            statusMessage = "Connected ${adapter.spec.displayName} (${result.models.size} models)",
                            providers = state.providers.map {
                                if (it.spec.id == providerId) {
                                    it.copy(
                                        isConnected = true,
                                        isTesting = false,
                                        discoveredModels = result.models,
                                        lastError = null
                                    )
                                } else it
                            }
                        )
                    }
                }
                is ProviderValidationResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            statusMessage = result.reason,
                            providers = state.providers.map {
                                if (it.spec.id == providerId) {
                                    it.copy(isTesting = false, lastError = result.reason)
                                } else it
                            }
                        )
                    }
                }
            }
        }
    }

    fun disconnectProvider(providerId: String) {
        viewModelScope.launch {
            keyStore.removeApiKey(providerId)
            _uiState.update { state ->
                state.copy(
                    providers = state.providers.map {
                        if (it.spec.id == providerId) {
                            it.copy(isConnected = false, discoveredModels = emptyList(), lastError = null)
                        } else it
                    }
                )
            }
        }
    }
}
