package com.aihubmax.features.api_manager

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ApiManagerRoute(
    isPersian: Boolean,
    onBack: () -> Unit,
    viewModel: ApiManagerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    ApiManagerScreen(
        state = state,
        isPersian = isPersian,
        onBack = onBack,
        onSelectProvider = viewModel::selectProvider,
        onApiKeyChanged = viewModel::onApiKeyChanged,
        onPasteFromClipboard = viewModel::onPasteFromClipboard,
        onValidateAndConnect = viewModel::validateAndConnect,
        onDisconnectProvider = viewModel::disconnectProvider
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApiManagerScreen(
    state: ApiManagerUiState,
    isPersian: Boolean,
    onBack: () -> Unit,
    onSelectProvider: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onPasteFromClipboard: (String?) -> Unit,
    onValidateAndConnect: () -> Unit,
    onDisconnectProvider: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val selected = state.providers.firstOrNull { it.spec.id == state.selectedProviderId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isPersian) "مرکز API و اتصال امن" else "API Center & Auto Connect",
                        fontWeight = FontWeight.Bold
                    )
                },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isPersian) "۱. انتخاب Provider" else "1. Select Provider",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.providers.forEach { item ->
                    FilterChip(
                        selected = item.spec.id == state.selectedProviderId,
                        onClick = { onSelectProvider(item.spec.id) },
                        label = {
                            val badge = if (item.isConnected) " ✓" else ""
                            Text("${item.spec.displayName}$badge")
                        }
                    )
                }
            }

            selected?.let { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = item.spec.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.spec.officialKeyUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.OpenInBrowser, contentDescription = null)
                            Text(
                                text = if (isPersian) " ۲. دریافت API Key از سایت رسمی" else " 2. Open Official API Key Page",
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }

                        OutlinedTextField(
                            value = state.apiKeyDraft,
                            onValueChange = onApiKeyChanged,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        onPasteFromClipboard(clipboard.getText()?.text)
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.ContentPaste,
                                        contentDescription = if (isPersian) "جایگذاری از کلیپ‌بورد" else "Paste from Clipboard"
                                    )
                                }
                            },
                            label = {
                                Text(
                                    if (isPersian) "کلید API (رمزنگاری با Android Keystore)"
                                    else "API Key (Encrypted via Android Keystore)"
                                )
                            },
                            placeholder = { Text("${item.spec.keyPrefixHint}...") }
                        )

                        Button(
                            onClick = onValidateAndConnect,
                            enabled = state.apiKeyDraft.isNotBlank() && !item.isTesting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (item.isTesting) {
                                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            }
                            Text(
                                if (isPersian) "۳. اعتبارسنجی، دریافت مدل‌ها و اتصال امن"
                                else "3. Validate, Fetch Models & Connect"
                            )
                        }

                        if (item.isConnected) {
                            OutlinedButton(
                                onClick = { onDisconnectProvider(item.spec.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                                Text(
                                    text = if (isPersian) " قطع اتصال و حذف کلید" else " Disconnect & Remove Key",
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }

                        state.statusMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item.lastError != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        }

                        if (item.discoveredModels.isNotEmpty()) {
                            Text(
                                text = if (isPersian) "مدل‌های شناسایی‌شده (${item.discoveredModels.size}):"
                                else "Discovered Models (${item.discoveredModels.size}):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            item.discoveredModels.take(8).forEach { model ->
                                Text(
                                    text = "• ${model.displayName} (${model.id})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
