package com.aihubmax.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihubmax.core.common.LocalIntentClassifier
import com.aihubmax.core.database.ChatDao
import com.aihubmax.core.database.ChatMessageEntity
import com.aihubmax.core.network.ChatInferenceResult
import com.aihubmax.core.network.ProviderRegistry
import com.aihubmax.core.routing.MaxAiRouter
import com.aihubmax.core.routing.RouterMode
import com.aihubmax.core.routing.RoutingDecision
import com.aihubmax.core.security.SecureApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebateEntry(
    val providerName: String,
    val modelId: String,
    val text: String?,
    val error: String?,
    val latencyMs: Long,
    val tokens: Int?
)

data class PendingPaidConfirmation(
    val prompt: String,
    val providerId: String,
    val providerName: String,
    val modelId: String,
    val costNote: String
)

data class ChatUiState(
    val input: String = "",
    val isDebateMode: Boolean = false,
    val routerMode: RouterMode = RouterMode.MAX_BALANCED,
    val isSending: Boolean = false,
    val messages: List<ChatMessageEntity> = emptyList(),
    val debateResults: List<DebateEntry> = emptyList(),
    val connectedProviderIds: Set<String> = emptySet(),
    val errorBanner: String? = null,
    val pendingPaidConfirmation: PendingPaidConfirmation? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val keyStore: SecureApiKeyStore,
    private val registry: ProviderRegistry,
    private val router: MaxAiRouter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val defaultModels = mapOf(
        "gemini" to "gemini-2.5-flash",
        "groq" to "llama-3.3-70b-versatile",
        "openrouter" to "openrouter/free",
        "deepseek" to "deepseek-chat",
        "openai" to "gpt-4o-mini"
    )

    init {
        viewModelScope.launch {
            chatDao.observeMessages().collect { list ->
                _uiState.update { it.copy(messages = list) }
            }
        }
        viewModelScope.launch {
            keyStore.connectedProviderIds.collect { ids ->
                _uiState.update { it.copy(connectedProviderIds = ids) }
            }
        }
    }

    fun setInitialPrompt(prompt: String) {
        if (prompt.isNotBlank() && _uiState.value.input.isBlank()) {
            _uiState.update { it.copy(input = prompt) }
        }
    }

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(input = value, errorBanner = null) }
    }

    fun toggleDebateMode(enabled: Boolean) {
        _uiState.update { it.copy(isDebateMode = enabled, errorBanner = null) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val prompt = state.input.trim()
        if (prompt.isBlank() || state.isSending) return
        if (state.connectedProviderIds.isEmpty()) {
            _uiState.update {
                it.copy(errorBanner = "ابتدا حداقل یک Provider را در مرکز API متصل کنید.")
            }
            return
        }

        if (state.isDebateMode) {
            runMultiAiDebate(prompt, state.connectedProviderIds)
        } else {
            val category = LocalIntentClassifier.classify(prompt)
            when (val decision = router.route(category, state.routerMode, state.connectedProviderIds)) {
                is RoutingDecision.Selected -> {
                    val candidate = decision.candidate
                    if (!candidate.isFreeTierAvailable) {
                        _uiState.update {
                            it.copy(
                                pendingPaidConfirmation = PendingPaidConfirmation(
                                    prompt = prompt,
                                    providerId = candidate.providerId,
                                    providerName = candidate.providerName,
                                    modelId = candidate.modelId,
                                    costNote = candidate.estimatedCostNoteFa
                                )
                            )
                        }
                    } else {
                        executeSingleChat(prompt, candidate.providerId, candidate.modelId)
                    }
                }
                is RoutingDecision.NoConnectedProvider -> {
                    _uiState.update { it.copy(errorBanner = decision.reasonFa) }
                }
            }
        }
    }

    fun confirmPaidRequest() {
        val pending = _uiState.value.pendingPaidConfirmation ?: return
        _uiState.update { it.copy(pendingPaidConfirmation = null) }
        executeSingleChat(pending.prompt, pending.providerId, pending.modelId)
    }

    fun dismissPaidRequest() {
        _uiState.update { it.copy(pendingPaidConfirmation = null) }
    }

    private fun executeSingleChat(prompt: String, providerId: String, modelId: String) {
        val adapter = registry.getAdapter(providerId) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, input = "", errorBanner = null) }
            chatDao.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    content = prompt,
                    providerId = "user",
                    modelId = "local",
                    totalTokens = null
                )
            )
            val apiKey = keyStore.getApiKey(providerId)
            if (apiKey.isNullOrBlank()) {
                _uiState.update { it.copy(isSending = false, errorBanner = "کلید API یافت نشد.") }
                return@launch
            }

            when (val result = adapter.generateText(apiKey, modelId, prompt)) {
                is ChatInferenceResult.Success -> {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            role = "assistant",
                            content = result.text,
                            providerId = result.providerId,
                            modelId = result.modelId,
                            totalTokens = result.totalTokens
                        )
                    )
                    _uiState.update { it.copy(isSending = false) }
                }
                is ChatInferenceResult.Failure -> {
                    _uiState.update { it.copy(isSending = false, errorBanner = result.reason) }
                }
            }
        }
    }

    private fun runMultiAiDebate(prompt: String, connectedIds: Set<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, input = "", debateResults = emptyList(), errorBanner = null) }
            val results = coroutineScope {
                connectedIds.mapNotNull { pid ->
                    val adapter = registry.getAdapter(pid) ?: return@mapNotNull null
                    val modelId = defaultModels[pid] ?: return@mapNotNull null
                    async {
                        val key = keyStore.getApiKey(pid)
                        if (key.isNullOrBlank()) {
                            DebateEntry(adapter.spec.displayName, modelId, null, "Missing API Key", 0L, null)
                        } else {
                            when (val res = adapter.generateText(key, modelId, prompt)) {
                                is ChatInferenceResult.Success -> DebateEntry(
                                    providerName = adapter.spec.displayName,
                                    modelId = res.modelId,
                                    text = res.text,
                                    error = null,
                                    latencyMs = res.latencyMs,
                                    tokens = res.totalTokens
                                )
                                is ChatInferenceResult.Failure -> DebateEntry(
                                    providerName = adapter.spec.displayName,
                                    modelId = res.modelId,
                                    text = null,
                                    error = res.reason,
                                    latencyMs = 0L,
                                    tokens = null
                                )
                            }
                        }
                    }
                }.awaitAll()
            }
            _uiState.update { it.copy(isSending = false, debateResults = results) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatDao.clearHistory()
            _uiState.update { it.copy(debateResults = emptyList()) }
        }
    }
}
