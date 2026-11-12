package com.aihubmax.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aihubmax.core.routing.RouterMode
import com.aihubmax.core.routing.RoutingDecision

@Composable
fun HomeDashboardRoute(
    isPersian: Boolean,
    onToggleLanguage: () -> Unit,
    onOpenApiCenter: () -> Unit,
    onOpenFreeAiRadar: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenImageStudio: () -> Unit,
    onOpenVideoStudio: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenUsage: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    HomeDashboardScreen(
        state = state,
        isPersian = isPersian,
        onToggleLanguage = onToggleLanguage,
        onOpenApiCenter = onOpenApiCenter,
        onOpenFreeAiRadar = onOpenFreeAiRadar,
        onOpenChat = onOpenChat,
        onOpenImageStudio = onOpenImageStudio,
        onOpenVideoStudio = onOpenVideoStudio,
        onOpenAgents = onOpenAgents,
        onOpenDocuments = onOpenDocuments,
        onOpenUsage = onOpenUsage,
        onPromptChanged = viewModel::onPromptChanged,
        onRouterModeSelected = viewModel::onRouterModeSelected,
        onQuickPromptSelected = viewModel::onQuickPromptSelected,
        onAnalyzePrompt = viewModel::onAnalyzePrompt
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
    state: HomeUiState,
    isPersian: Boolean,
    onToggleLanguage: () -> Unit,
    onOpenApiCenter: () -> Unit,
    onOpenFreeAiRadar: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenImageStudio: () -> Unit,
    onOpenVideoStudio: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenUsage: () -> Unit,
    onPromptChanged: (String) -> Unit,
    onRouterModeSelected: (RouterMode) -> Unit,
    onQuickPromptSelected: (String) -> Unit,
    onAnalyzePrompt: () -> Unit
) {
    val samplePrompts = if (isPersian) {
        listOf(
            "یک تصویر سینمایی از همدان در آینده بساز",
            "این PDF را خلاصه کن",
            "درباره جدیدترین فناوری‌های AI تحقیق کن",
            "یک ویدئوی تبلیغاتی ۶۰ ثانیه‌ای بساز",
            "یک وبسایت طراحی کن",
            "یک Agent برای مدیریت پروژه بساز"
        )
    } else {
        listOf(
            "Generate a futuristic cinematic image",
            "Summarize this PDF document",
            "Research the latest AI technologies",
            "Create a 60-second promo video",
            "Design a modern website",
            "Build a project management Agent"
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "AI HUB MAX", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleLanguage) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = if (isPersian) "تغییر زبان" else "Switch Language"
                        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isPersian) "داشبورد جامع AI HUB MAX" else "AI HUB MAX Control Center",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isPersian) {
                            "Providerهای متصل: ${state.connectedProviderIds.size} • سرویس‌های رایگان تأییدشده: ${state.freeCatalogModelsCount}"
                        } else {
                            "Connected Providers: ${state.connectedProviderIds.size} • Verified Free Tiers: ${state.freeCatalogModelsCount}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenApiCenter, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Key, contentDescription = null)
                            Text(if (isPersian) " مرکز API" else " API Center")
                        }
                        Button(onClick = onOpenFreeAiRadar, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Radar, contentDescription = null)
                            Text(if (isPersian) " رادار رایگان" else " Free AI")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenImageStudio, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Image, contentDescription = null)
                            Text(if (isPersian) " تصویر" else " Image")
                        }
                        OutlinedButton(onClick = onOpenVideoStudio, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Movie, contentDescription = null)
                            Text(if (isPersian) " ویدئو" else " Video")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenAgents, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.SmartToy, contentDescription = null)
                            Text(if (isPersian) " ایجنت‌ها" else " Agents")
                        }
                        OutlinedButton(onClick = onOpenDocuments, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Description, contentDescription = null)
                            Text(if (isPersian) " اسناد" else " Docs AI")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenChat(state.promptInput) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Chat, contentDescription = null)
                            Text(if (isPersian) " چت و مناظره" else " Chat & Debate")
                        }
                        OutlinedButton(onClick = onOpenUsage, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Analytics, contentDescription = null)
                            Text(if (isPersian) " مصرف و هزینه" else " Cost Control")
                        }
                    }
                }
            }

            Text(text = "MAX AI ROUTER", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    RouterMode.MAX_BALANCED,
                    RouterMode.MAX_FREE,
                    RouterMode.MAX_PERSIAN,
                    RouterMode.MAX_SPEED,
                    RouterMode.MAX_QUALITY,
                    RouterMode.MAX_CODING
                ).forEach { mode ->
                    FilterChip(
                        selected = state.selectedRouterMode == mode,
                        onClick = { onRouterModeSelected(mode) },
                        label = { Text(if (isPersian) mode.labelFa else mode.labelEn) }
                    )
                }
            }

            Text(text = "ASK AI MAX", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = state.promptInput,
                onValueChange = onPromptChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = {
                    Text(
                        if (isPersian) "هر درخواستی بنویسید تا Workflow و بهترین مدل انتخاب شود..."
                        else "Type any request to detect workflow & optimal model..."
                    )
                }
            )

            Button(
                onClick = {
                    onAnalyzePrompt()
                    onOpenChat(state.promptInput)
                },
                enabled = state.promptInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Rounded.Send, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(if (isPersian) "ارسال به AI Chat با مسیریابی هوشمند" else "Send to AI Chat via Smart Router")
            }

            state.detectedCategory?.let { category ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isPersian) "نوع تشخیص‌داده‌شده: ${category.titleFa}" else "Detected Type: ${category.titleEn}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isPersian) "مسیر پیشنهادی: ${category.suggestedWorkflowFa}" else "Suggested Workflow: ${category.suggestedWorkflowEn}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        when (val decision = state.routingDecision) {
                            is RoutingDecision.Selected -> {
                                Text(
                                    text = if (isPersian) "مدل منتخب Router: ${decision.candidate.providerName} (${decision.candidate.modelId})"
                                    else "Router Pick: ${decision.candidate.providerName} (${decision.candidate.modelId})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            is RoutingDecision.NoConnectedProvider -> {
                                Text(
                                    text = if (isPersian) decision.reasonFa else decision.reasonEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            null -> Unit
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPersian) "نمونه دستورهای سریع" else "Quick Sample Prompts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                samplePrompts.forEach { sample ->
                    AssistChip(onClick = { onQuickPromptSelected(sample) }, label = { Text(sample) })
                }
            }
        }
    }
}
