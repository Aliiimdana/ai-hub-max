package com.aihubmax.features.image_studio

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ImageStudioRoute(
    isPersian: Boolean,
    onBack: () -> Unit,
    viewModel: ImageStudioViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    ImageStudioScreen(
        state = state,
        isPersian = isPersian,
        onBack = onBack,
        onPromptChanged = viewModel::onPromptChanged,
        onProviderSelected = viewModel::onProviderSelected,
        onEnhancePrompt = viewModel::enhancePrompt,
        onRequestGenerate = viewModel::requestGenerate,
        onConfirmGenerate = viewModel::confirmAndGenerate,
        onDismissCost = viewModel::dismissCostConfirmation,
        onDeleteImage = viewModel::deleteImage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageStudioScreen(
    state: ImageStudioUiState,
    isPersian: Boolean,
    onBack: () -> Unit,
    onPromptChanged: (String) -> Unit,
    onProviderSelected: (String) -> Unit,
    onEnhancePrompt: () -> Unit,
    onRequestGenerate: () -> Unit,
    onConfirmGenerate: () -> Unit,
    onDismissCost: () -> Unit,
    onDeleteImage: (String) -> Unit
) {
    if (state.showCostConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissCost,
            title = { Text(if (isPersian) "تأیید تولید تصویر" else "Confirm Image Generation") },
            text = {
                Text(
                    if (isPersian) {
                        "Provider: ${state.selectedProviderId}\nتولید تصویر ممکن است شامل هزینه API باشد. ادامه می‌دهید؟"
                    } else {
                        "Provider: ${state.selectedProviderId}\nImage generation may incur API charges. Proceed?"
                    }
                )
            },
            confirmButton = {
                Button(onClick = onConfirmGenerate) {
                    Text(if (isPersian) "تأیید و ساخت تصویر" else "Confirm & Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCost) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAX IMAGE STUDIO", fontWeight = FontWeight.Bold) },
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.selectedProviderId == "gemini",
                    onClick = { onProviderSelected("gemini") },
                    label = { Text("Gemini Imagen") }
                )
                FilterChip(
                    selected = state.selectedProviderId == "openai",
                    onClick = { onProviderSelected("openai") },
                    label = { Text("OpenAI gpt-image-1") }
                )
            }

            OutlinedTextField(
                value = state.prompt,
                onValueChange = onPromptChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = {
                    Text(
                        if (isPersian) "توصیف تصویر مورد نظر را بنویسید..."
                        else "Describe the image to generate..."
                    )
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEnhancePrompt,
                    enabled = state.prompt.isNotBlank() && !state.isEnhancingPrompt,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null)
                    Text(if (isPersian) " بهبود پرامپت" else " Enhance")
                }
                Button(
                    onClick = onRequestGenerate,
                    enabled = state.prompt.isNotBlank() && !state.isGenerating,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Text(if (isPersian) " ساخت تصویر" else " Generate")
                }
            }

            if (state.isGenerating || state.isEnhancingPrompt) {
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

            Text(
                text = if (isPersian) "گالری تصاویر ساخته‌شده (${state.galleryItems.size})"
                else "Generated Image Gallery (${state.galleryItems.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            state.galleryItems.forEach { item ->
                val bitmap = remember(item.filePath) {
                    BitmapFactory.decodeFile(item.filePath)?.asImageBitmap()
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = item.prompt,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        }
                        Text(text = item.prompt, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.providerId} • ${item.modelId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { onDeleteImage(item.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}
