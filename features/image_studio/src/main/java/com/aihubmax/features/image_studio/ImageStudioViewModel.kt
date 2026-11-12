package com.aihubmax.features.image_studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihubmax.core.network.ChatInferenceResult
import com.aihubmax.core.network.ImageGenerationClient
import com.aihubmax.core.network.ImageGenerationResult
import com.aihubmax.core.network.ProviderRegistry
import com.aihubmax.core.security.SecureApiKeyStore
import com.aihubmax.core.storage.LocalMediaGalleryStore
import com.aihubmax.core.storage.SavedImageItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImageStudioUiState(
    val prompt: String = "",
    val selectedProviderId: String = "gemini",
    val isGenerating: Boolean = false,
    val isEnhancingPrompt: Boolean = false,
    val showCostConfirmation: Boolean = false,
    val errorMessage: String? = null,
    val galleryItems: List<SavedImageItem> = emptyList(),
    val connectedProviderIds: Set<String> = emptySet()
)

@HiltViewModel
class ImageStudioViewModel @Inject constructor(
    private val imageClient: ImageGenerationClient,
    private val galleryStore: LocalMediaGalleryStore,
    private val keyStore: SecureApiKeyStore,
    private val registry: ProviderRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageStudioUiState())
    val uiState: StateFlow<ImageStudioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            galleryStore.images.collect { items ->
                _uiState.update { it.copy(galleryItems = items) }
            }
        }
        viewModelScope.launch {
            keyStore.connectedProviderIds.collect { ids ->
                _uiState.update { it.copy(connectedProviderIds = ids) }
            }
        }
    }

    fun onPromptChanged(value: String) {
        _uiState.update { it.copy(prompt = value, errorMessage = null) }
    }

    fun onProviderSelected(providerId: String) {
        _uiState.update { it.copy(selectedProviderId = providerId, errorMessage = null) }
    }

    fun enhancePrompt() {
        val current = _uiState.value
        if (current.prompt.isBlank() || current.isEnhancingPrompt) return
        val llmProviderId = listOf("gemini", "groq", "openrouter", "openai", "deepseek")
            .firstOrNull { it in current.connectedProviderIds }

        if (llmProviderId == null) {
            _uiState.update { it.copy(errorMessage = "برای بهبود پرامپت ابتدا یک مدل متنی متصل کنید.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isEnhancingPrompt = true, errorMessage = null) }
            val key = keyStore.getApiKey(llmProviderId)
            val adapter = registry.getAdapter(llmProviderId)
            if (key.isNullOrBlank() || adapter == null) {
                _uiState.update { it.copy(isEnhancingPrompt = false, errorMessage = "کلید API یافت نشد.") }
                return@launch
            }
            val modelId = when (llmProviderId) {
                "gemini" -> "gemini-2.5-flash"
                "groq" -> "llama-3.3-70b-versatile"
                "openrouter" -> "openrouter/free"
                "deepseek" -> "deepseek-chat"
                else -> "gpt-4o-mini"
            }
            val sysPrompt = "Rewrite this image generation prompt in vivid English cinematic detail (return ONLY the improved prompt): ${current.prompt}"
            when (val res = adapter.generateText(key, modelId, sysPrompt)) {
                is ChatInferenceResult.Success -> {
                    _uiState.update { it.copy(isEnhancingPrompt = false, prompt = res.text.trim()) }
                }
                is ChatInferenceResult.Failure -> {
                    _uiState.update { it.copy(isEnhancingPrompt = false, errorMessage = res.reason) }
                }
            }
        }
    }

    fun requestGenerate() {
        val state = _uiState.value
        if (state.prompt.isBlank()) return
        if (state.selectedProviderId !in state.connectedProviderIds) {
            _uiState.update {
                it.copy(errorMessage = "ابتدا Provider (${state.selectedProviderId}) را در مرکز API متصل کنید.")
            }
            return
        }
        _uiState.update { it.copy(showCostConfirmation = true) }
    }

    fun dismissCostConfirmation() {
        _uiState.update { it.copy(showCostConfirmation = false) }
    }

    fun confirmAndGenerate() {
        val state = _uiState.value
        _uiState.update { it.copy(showCostConfirmation = false, isGenerating = true, errorMessage = null) }

        viewModelScope.launch {
            val key = keyStore.getApiKey(state.selectedProviderId)
            if (key.isNullOrBlank()) {
                _uiState.update { it.copy(isGenerating = false, errorMessage = "کلید API یافت نشد.") }
                return@launch
            }

            val result = when (state.selectedProviderId) {
                "openai" -> imageClient.generateWithOpenAi(key, state.prompt)
                else -> imageClient.generateWithGeminiImagen(key, state.prompt)
            }

            when (result) {
                is ImageGenerationResult.Success -> {
                    galleryStore.saveGeneratedImage(
                        bytes = result.bytes,
                        prompt = state.prompt,
                        providerId = result.providerId,
                        modelId = result.modelId
                    )
                    _uiState.update { it.copy(isGenerating = false) }
                }
                is ImageGenerationResult.Failure -> {
                    _uiState.update { it.copy(isGenerating = false, errorMessage = result.reason) }
                }
            }
        }
    }

    fun deleteImage(id: String) {
        viewModelScope.launch { galleryStore.deleteImage(id) }
    }
}
