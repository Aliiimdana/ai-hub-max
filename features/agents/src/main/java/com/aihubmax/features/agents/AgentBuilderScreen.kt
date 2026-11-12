package com.aihubmax.features.agents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

data class AgentTemplate(
    val name: String,
    val instructions: String,
    val allowedTools: List<String>
)

data class AgentUiState(
    val agentName: String = "Persian Assistant",
    val instructions: String = "You are a helpful Persian AI assistant. Respond clearly and accurately.",
    val goalInput: String = "",
    val isRunning: Boolean = false,
    val workflowLog: String = "",
    val connectedProviderIds: Set<String> = emptySet()
)

@HiltViewModel
class AgentBuilderViewModel @Inject constructor(
    private val keyStore: SecureApiKeyStore,
    private val registry: ProviderRegistry
) : ViewModel() {

    val templates = listOf(
        AgentTemplate("Persian Assistant", "Respond in clear, structured Persian.", listOf("Chat")),
        AgentTemplate("Coding Agent", "Write clean Kotlin/Compose code with edge-case checks.", listOf("Chat", "Code Review")),
        AgentTemplate("Research Agent", "Analyze the topic with clear pros, cons, and next steps.", listOf("Chat", "Analysis")),
        AgentTemplate("Marketing Agent", "Create concise Persian marketing copy and hooks.", listOf("Chat", "Copywriting"))
    )

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            keyStore.connectedProviderIds.collect { ids ->
                _uiState.update { it.copy(connectedProviderIds = ids) }
            }
        }
    }

    fun selectTemplate(t: AgentTemplate) {
        _uiState.update { it.copy(agentName = t.name, instructions = t.instructions) }
    }

    fun onNameChanged(v: String) = _uiState.update { it.copy(agentName = v) }
    fun onInstructionsChanged(v: String) = _uiState.update { it.copy(instructions = v) }
    fun onGoalChanged(v: String) = _uiState.update { it.copy(goalInput = v) }

    fun runAgentWorkflow() {
        val state = _uiState.value
        if (state.goalInput.isBlank() || state.isRunning) return
        val providerId = listOf("gemini", "groq", "openrouter", "openai", "deepseek")
            .firstOrNull { it in state.connectedProviderIds }

        if (providerId == null) {
            _uiState.update { it.copy(workflowLog = "Error: No connected provider in API Center.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, workflowLog = "[1/2] Initializing ${state.agentName} via $providerId...") }
            val key = keyStore.getApiKey(providerId)
            val adapter = registry.getAdapter(providerId)
            if (key.isNullOrBlank() || adapter == null) {
                _uiState.update { it.copy(isRunning = false, workflowLog = "Error: Missing API key.") }
                return@launch
            }
            val modelId = when (providerId) {
                "gemini" -> "gemini-2.5-flash"
                "groq" -> "llama-3.3-70b-versatile"
                "openrouter" -> "openrouter/free"
                "deepseek" -> "deepseek-chat"
                else -> "gpt-4o-mini"
            }
            val fullPrompt = "Agent Role: ${state.agentName}
Instructions: ${state.instructions}
User Goal: ${state.goalInput}"
            when (val res = adapter.generateText(key, modelId, fullPrompt)) {
                is ChatInferenceResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            workflowLog = "[2/2] Completed ($providerId / $modelId):

${res.text}"
                        )
                    }
                }
                is ChatInferenceResult.Failure -> {
                    _uiState.update { it.copy(isRunning = false, workflowLog = "Failed: ${res.reason}") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgentBuilderRoute(
    isPersian: Boolean,
    onBack: () -> Unit,
    viewModel: AgentBuilderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAX AGENT BUILDER", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.templates.forEach { t ->
                    FilterChip(
                        selected = state.agentName == t.name,
                        onClick = { viewModel.selectTemplate(t) },
                        label = { Text(t.name) }
                    )
                }
            }

            OutlinedTextField(
                value = state.agentName,
                onValueChange = viewModel::onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isPersian) "نام Agent" else "Agent Name") }
            )
            OutlinedTextField(
                value = state.instructions,
                onValueChange = viewModel::onInstructionsChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text(if (isPersian) "دستورالعمل Agent" else "Agent Instructions") }
            )
            OutlinedTextField(
                value = state.goalInput,
                onValueChange = viewModel::onGoalChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text(if (isPersian) "هدف / تسک برای اجرا" else "Goal / Task to Execute") }
            )

            Button(
                onClick = viewModel::runAgentWorkflow,
                enabled = state.goalInput.isNotBlank() && !state.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text(if (isPersian) " اجرای Workflow ایجنت" else " Run Agent Workflow")
            }

            if (state.isRunning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (state.workflowLog.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.workflowLog,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}
