package com.aihubmax.features.documents

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

data class DocumentUiState(
    val fileName: String = "",
    val extractedText: String = "",
    val question: String = "این سند را خلاصه کن و نکات کلیدی آن را استخراج کن.",
    val analysisOutput: String = "",
    val isAnalyzing: Boolean = false,
    val connectedProviderIds: Set<String> = emptySet()
)

@HiltViewModel
class DocumentAiViewModel @Inject constructor(
    private val keyStore: SecureApiKeyStore,
    private val registry: ProviderRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            keyStore.connectedProviderIds.collect { ids ->
                _uiState.update { it.copy(connectedProviderIds = ids) }
            }
        }
    }

    fun loadFileContent(context: Context, uri: Uri) {
        val name = uri.lastPathSegment ?: "document"
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        }.getOrDefault("")
        _uiState.update {
            it.copy(
                fileName = name,
                extractedText = text.take(15000),
                analysisOutput = if (text.isBlank()) "فایل متنی قابل خواندن نیست یا خالی است." else ""
            )
        }
    }

    fun onTextChanged(v: String) = _uiState.update { it.copy(extractedText = v) }
    fun onQuestionChanged(v: String) = _uiState.update { it.copy(question = v) }

    fun analyzeDocument() {
        val state = _uiState.value
        if (state.extractedText.isBlank() || state.isAnalyzing) return
        val providerId = listOf("gemini", "groq", "openrouter", "openai", "deepseek")
            .firstOrNull { it in state.connectedProviderIds }
        if (providerId == null) {
            _uiState.update { it.copy(analysisOutput = "ابتدا یک Provider در مرکز API متصل کنید.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            val key = keyStore.getApiKey(providerId)
            val adapter = registry.getAdapter(providerId)
            if (key.isNullOrBlank() || adapter == null) {
                _uiState.update { it.copy(isAnalyzing = false, analysisOutput = "کلید API یافت نشد.") }
                return@launch
            }
            val modelId = when (providerId) {
                "gemini" -> "gemini-2.5-flash"
                "groq" -> "llama-3.3-70b-versatile"
                "openrouter" -> "openrouter/free"
                "deepseek" -> "deepseek-chat"
                else -> "gpt-4o-mini"
            }
            val prompt = "${state.question}

Document Content:
${state.extractedText}"
            when (val res = adapter.generateText(key, modelId, prompt)) {
                is ChatInferenceResult.Success -> _uiState.update { it.copy(isAnalyzing = false, analysisOutput = res.text) }
                is ChatInferenceResult.Failure -> _uiState.update { it.copy(isAnalyzing = false, analysisOutput = res.reason) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentAiRoute(
    isPersian: Boolean,
    onBack: () -> Unit,
    viewModel: DocumentAiViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.loadFileContent(context, uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAX DOCUMENT AI", fontWeight = FontWeight.Bold) },
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
            OutlinedButton(
                onClick = { picker.launch(arrayOf("text/*", "application/json")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null)
                Text(if (isPersian) " انتخاب فایل (TXT / MD / CSV / JSON)" else " Pick Document (TXT/MD/CSV/JSON)")
            }

            OutlinedTextField(
                value = state.extractedText,
                onValueChange = viewModel::onTextChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                label = { Text(if (isPersian) "متن سند (یا جایگذاری متن)" else "Document Text") }
            )

            OutlinedTextField(
                value = state.question,
                onValueChange = viewModel::onQuestionChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isPersian) "دستور تحلیل" else "Analysis Instruction") }
            )

            Button(
                onClick = viewModel::analyzeDocument,
                enabled = state.extractedText.isNotBlank() && !state.isAnalyzing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Summarize, contentDescription = null)
                Text(if (isPersian) " تحلیل هوشمند سند" else " Analyze Document")
            }

            if (state.isAnalyzing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (state.analysisOutput.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.analysisOutput,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}
