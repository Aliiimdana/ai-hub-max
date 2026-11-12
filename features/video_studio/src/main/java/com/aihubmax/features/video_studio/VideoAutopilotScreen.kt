package com.aihubmax.features.video_studio

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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

@Composable
fun VideoAutopilotRoute(
    isPersian: Boolean,
    onBack: () -> Unit,
    viewModel: VideoAutopilotViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    VideoAutopilotScreen(
        state = state,
        isPersian = isPersian,
        onBack = onBack,
        onTopicChanged = viewModel::onTopicChanged,
        onBriefChanged = viewModel::onBriefChanged,
        onScriptChanged = viewModel::onScriptChanged,
        onStoryboardChanged = viewModel::onStoryboardChanged,
        onGenerateStage = viewModel::generateCurrentStage,
        onApproveNext = viewModel::approveAndNextStage,
        onSelectStage = viewModel::goToStage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoAutopilotScreen(
    state: VideoAutopilotUiState,
    isPersian: Boolean,
    onBack: () -> Unit,
    onTopicChanged: (String) -> Unit,
    onBriefChanged: (String) -> Unit,
    onScriptChanged: (String) -> Unit,
    onStoryboardChanged: (String) -> Unit,
    onGenerateStage: () -> Unit,
    onApproveNext: () -> Unit,
    onSelectStage: (Int) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAX VIDEO AUTOPILOT", fontWeight = FontWeight.Bold) },
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
            OutlinedTextField(
                value = state.topic,
                onValueChange = onTopicChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isPersian) "موضوع ویدئو" else "Video Topic") },
                placeholder = {
                    Text(
                        if (isPersian) "مثلاً: یک ویدئوی تبلیغاتی ۶۰ ثانیه‌ای درباره خدمات سازمان"
                        else "e.g., 60-second promo video"
                    )
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to "1. Brief", 2 to "2. Script", 3 to "3. Storyboard", 4 to "4. Render/Ext").forEach { (num, label) ->
                    FilterChip(
                        selected = state.currentStage == num,
                        onClick = { onSelectStage(num) },
                        label = { Text(label) }
                    )
                }
            }

            if (state.isGenerating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.errorMessage?.let { err ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (state.currentStage in 1..3) {
                val (label, value, onChange) = when (state.currentStage) {
                    1 -> Triple("Stage 1: Creative Brief", state.briefText, onBriefChanged)
                    2 -> Triple("Stage 2: Script & Narration", state.scriptText, onScriptChanged)
                    else -> Triple("Stage 3: Storyboard", state.storyboardText, onStoryboardChanged)
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    label = { Text(label) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onGenerateStage,
                        enabled = state.topic.isNotBlank() && !state.isGenerating,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Text(if (isPersian) " تولید با AI" else " Generate")
                    }
                    OutlinedButton(
                        onClick = onApproveNext,
                        enabled = value.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                        Text(if (isPersian) " تأیید و مرحله بعد" else " Approve & Next")
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isPersian) "مراحل ۴ تا ۹ (سرویس‌های خارجی و رندر)"
                            else "Stages 4–9 (External Video Services & Render)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPersian) {
                                "برای تولید واقعی ویدئو از استوری‌بورد بالا، هیچ خروجی جعلی ساخته نمی‌شود. می‌توانید از سرویس‌های دارای API رسمی زیر استفاده کنید:"
                            } else {
                                "No fake video output is generated. Use one of the official External Services below with your storyboard:"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        listOf(
                            "Runway API (External Service)" to "https://docs.dev.runwayml.com/",
                            "Fal.ai Video API (External Service)" to "https://fal.ai/models",
                            "Replicate Video API (External Service)" to "https://replicate.com/collections/text-to-video"
                        ).forEach { (title, url) ->
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.OpenInBrowser, contentDescription = null)
                                Text(" $title")
                            }
                        }
                    }
                }
            }
        }
    }
}
