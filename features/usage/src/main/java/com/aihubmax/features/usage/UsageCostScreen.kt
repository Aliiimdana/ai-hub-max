package com.aihubmax.features.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.aihubmax.core.database.ChatDao
import com.aihubmax.core.security.SecureApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class UsageSummaryUiState(
    val totalRequests: Int = 0,
    val totalTokens: Int = 0,
    val connectedCount: Int = 0
)

@HiltViewModel
class UsageViewModel @Inject constructor(
    chatDao: ChatDao,
    keyStore: SecureApiKeyStore
) : ViewModel() {
    val uiState = combine(
        chatDao.observeMessages(),
        keyStore.connectedProviderIds
    ) { messages, connected ->
        val assistantMsgs = messages.filter { it.role == "assistant" }
        UsageSummaryUiState(
            totalRequests = assistantMsgs.size,
            totalTokens = assistantMsgs.sumOf { it.totalTokens ?: 0 },
            connectedCount = connected.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UsageSummaryUiState())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageCostRoute(
    isPersian: Boolean,
    onBack: () -> Unit,
    viewModel: UsageViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAX COST CONTROL", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isPersian) "گزارش واقعی مصرف" else "Real Usage Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(if (isPersian) "تعداد پاسخ‌های ثبت‌شده: ${state.totalRequests}" else "Recorded Responses: ${state.totalRequests}")
                    Text(if (isPersian) "مجموع توکن‌های ثبت‌شده: ${state.totalTokens}" else "Total Recorded Tokens: ${state.totalTokens}")
                    Text(if (isPersian) "Providerهای متصل: ${state.connectedCount}" else "Connected Providers: ${state.connectedCount}")
                }
            }
        }
    }
}
