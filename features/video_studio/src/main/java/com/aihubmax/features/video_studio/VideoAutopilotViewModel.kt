package com.aihubmax.features.video_studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihubmax.core.network.ChatInferenceResult
import com.aihubmax.core.network.ProviderRegistry
import com.aihubmax.core.security.SecureApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoAutopilotUiState(
    val topic: String = "",
    val currentStage: Int = 1,
    val briefText: String = "",
    val scriptText: String = "",
    val storyboardText: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val connectedProviderIds: Set<String> = emptySet()
)

@HiltViewModel
class VideoAutopilotViewModel @Inject constructor(
    private val keyStore: SecureApiKeyStore,
    private val registry: ProviderRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoAutopilotUiState())
    val uiState: StateFlow<VideoAutopilotUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            keyStore.connectedProviderIds.collect { ids ->
                _uiState.update { it.copy(connectedProviderIds = ids) }
            }
        }
    }

    fun onTopicChanged(value: String) {
        _uiState.update { it.copy(topic = value, errorMessage = null) }
    }

    fun onBriefChanged(value: String) {
        _uiState.update { it.copy(briefText = value) }
    }

    fun onScriptChanged(value: String) {
        _uiState.update { it.copy(scriptText = value) }
    }

    fun onStoryboardChanged(value: String) {
        _uiState.update { it.copy(storyboardText = value) }
    }

    fun generateCurrentStage() {
        val state = _uiState.value
        if (state.topic.isBlank()) return
        val providerId = listOf("gemini", "groq", "openrouter", "openai", "deepseek")
            .firstOrNull { it in state.connectedProviderIds }

        if (providerId == null) {
            _uiState.update { it.copy(errorMessage = "ابتدا یک Provider متنی در مرکز API متصل کنید.") }
            return
        }

        val prompt = when (state.currentStage) {
            1 -> "Create a concise Creative Brief (Target Audience, Goal, Visual Style, Key Message) in Persian for this 60s video topic: ${state.topic}"
            2 -> "Write a 60-second Persian Voiceover Script & Scene Narration based on this Creative Brief:\n${state.briefText.ifBlank { state.topic }}"
            else -> "Break this video script into a 6-scene Storyboard with visual prompts and duration (seconds):\n${state.scriptText.ifBlank { state.topic }}"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            val key = keyStore.getApiKey(providerId)
            val adapter = registry.getAdapter(providerId)
            if (key.isNullOrBlank() || adapter == null) {
                _uiState.update { it.copy(isGenerating = false, errorMessage = "کلید API یافت نشد.") }
                return@launch
            }
            val modelId = when (providerId) {
                "gemini" -> "gemini-2.5-flash"
                "groq" -> "llama-3.3-70b-versatile"
                "openrouter" -> "openrouter/free"
                "deepseek" -> "deepseek-chat"
                else -> "gpt-4o-mini"
            }

            when (val res = adapter.generateText(key, modelId, prompt)) {
                is ChatInferenceResult.Success -> {
                    _uiState.update { s ->
                        when (s.currentStage) {
                            1 -> s.copy(isGenerating = false, briefText = res.text)
                            2 -> s.copy(isGenerating = false, scriptText = res.text)
                            else -> s.copy(isGenerating = false, storyboardText = res.text)
                        }
                    }
                }
                is ChatInferenceResult.Failure -> {
                    _uiState.update { it.copy(isGenerating = false, errorMessage = res.reason) }
                }
            }
        }
    }

    fun approveAndNextStage() {
        _uiState.update { it.copy(currentStage = (it.currentStage + 1).coerceAtMost(4)) }
    }

    fun goToStage(stage: Int) {
        _uiState.update { it.copy(currentStage = stage.coerceIn(1, 4)) }
    }
}
