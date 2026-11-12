package com.aihubmax.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihubmax.core.common.AiTaskCategory
import com.aihubmax.core.common.LocalIntentClassifier
import com.aihubmax.core.routing.MaxAiRouter
import com.aihubmax.core.routing.RouterMode
import com.aihubmax.core.routing.RoutingDecision
import com.aihubmax.core.security.SecureApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val promptInput: String = "",
    val selectedRouterMode: RouterMode = RouterMode.MAX_BALANCED,
    val detectedCategory: AiTaskCategory? = null,
    val routingDecision: RoutingDecision? = null,
    val connectedProviderIds: Set<String> = emptySet(),
    val freeCatalogModelsCount: Int = 3
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val keyStore: SecureApiKeyStore,
    private val router: MaxAiRouter
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            keyStore.connectedProviderIds.collect { ids ->
                _uiState.update { state ->
                    val updated = state.copy(connectedProviderIds = ids)
                    recomputeRouting(updated)
                }
            }
        }
    }

    fun onPromptChanged(newPrompt: String) {
        _uiState.update { state ->
            val category = if (newPrompt.isBlank()) null else LocalIntentClassifier.classify(newPrompt)
            recomputeRouting(state.copy(promptInput = newPrompt, detectedCategory = category))
        }
    }

    fun onRouterModeSelected(mode: RouterMode) {
        _uiState.update { state ->
            recomputeRouting(state.copy(selectedRouterMode = mode))
        }
    }

    fun onQuickPromptSelected(prompt: String) {
        onPromptChanged(prompt)
    }

    fun onAnalyzePrompt() {
        _uiState.update { state ->
            val category = if (state.promptInput.isBlank()) null else LocalIntentClassifier.classify(state.promptInput)
            recomputeRouting(state.copy(detectedCategory = category))
        }
    }

    private fun recomputeRouting(state: HomeUiState): HomeUiState {
        val cat = state.detectedCategory ?: return state.copy(routingDecision = null)
        val decision = router.route(cat, state.selectedRouterMode, state.connectedProviderIds)
        return state.copy(routingDecision = decision)
    }
}
