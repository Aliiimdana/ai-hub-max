package com.aihubmax.features.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ChatRoute(
    isPersian: Boolean,
    initialPrompt: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(initialPrompt) {
        viewModel.setInitialPrompt(initialPrompt)
    }
    ChatScreen(
        state = state,
        isPersian = isPersian,
        onBack = onBack,
        onInputChanged = viewModel::onInputChanged,
        onToggleDebate = viewModel::toggleDebateMode,
        onSend = viewModel::sendMessage,
        onClearHistory = viewModel::clearHistory,
        onConfirmPaid = viewModel::confirmPaidRequest,
        onDismissPaid = viewModel::dismissPaidRequest
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    isPersian: Boolean,
    onBack: () -> Unit,
    onInputChanged: (String) -> Unit,
    onToggleDebate: (Boolean) -> Unit,
    onSend: () -> Unit,
    onClearHistory: () -> Unit,
    onConfirmPaid: () -> Unit,
    onDismissPaid: () -> Unit
) {
    state.pendingPaidConfirmation?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismissPaid,
            title = {
                Text(if (isPersian) "تأیید مصرف سرویس پولی" else "Confirm Paid API Usage")
            },
            text = {
                Text(
                    if (isPersian) {
                        "Provider: ${pending.providerName}
مدل: ${pending.modelId}
وضعیت: ${pending.costNote}
آیا درخواست ارسال شود؟"
                    } else {
                        "Provider: ${pending.providerName}
Model: ${pending.modelId}
Note: ${pending.costNote}
Proceed with request?"
                    }
                )
            },
            confirmButton = {
                Button(onClick = onConfirmPaid) {
                    Text(if (isPersian) "تأیید و ارسال" else "Confirm & Send")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPaid) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isDebateMode) "MAX MULTI-AI DEBATE" else "UNIVERSAL AI CHAT",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onClearHistory) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.isDebateMode,
                    onClick = { onToggleDebate(false) },
                    label = { Text(if (isPersian) "گفتگوی هوشمند" else "Universal Chat") }
                )
                FilterChip(
                    selected = state.isDebateMode,
                    onClick = { onToggleDebate(true) },
                    leadingIcon = { Icon(Icons.Rounded.Forum, contentDescription = null) },
                    label = { Text(if (isPersian) "مناظره چندهوش (${state.connectedProviderIds.size})" else "Multi-AI Debate (${state.connectedProviderIds.size})") }
                )
            }

            if (state.isSending) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.errorBanner?.let { err ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.isDebateMode) {
                    items(state.debateResults) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${entry.providerName} • ${entry.modelId} (${entry.latencyMs}ms)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = entry.text ?: (entry.error ?: "Error"),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    items(state.messages) { msg ->
                        val isUser = msg.role == "user"
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!isUser) {
                                    Text(
                                        text = "${msg.providerId.uppercase()} • ${msg.modelId}" +
                                            (msg.totalTokens?.let { " • $it tokens" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(text = msg.content, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInputChanged,
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    placeholder = {
                        Text(if (isPersian) "پیام خود را بنویسید..." else "Type your message...")
                    }
                )
                IconButton(onClick = onSend, enabled = state.input.isNotBlank() && !state.isSending) {
                    Icon(Icons.Rounded.Send, contentDescription = null)
                }
            }
        }
    }
}
