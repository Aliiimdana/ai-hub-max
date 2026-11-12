package com.aihubmax.features.free_ai

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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FreeAiRadarRoute(
    isPersian: Boolean,
    onBack: () -> Unit,
    onConnectProvider: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showBestOnly by remember { mutableStateOf(false) }

    val filters = listOf("All", "Chat", "Coding", "Vision", "Audio", "STT (Whisper)")

    val filtered = remember(query, selectedFilter, showBestOnly) {
        VerifiedFreeAiCatalog.entries.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.modelName.contains(query, ignoreCase = true) ||
                item.providerName.contains(query, ignoreCase = true) ||
                item.capabilities.any { it.contains(query, ignoreCase = true) }

            val matchesFilter = selectedFilter == "All" ||
                item.capabilities.any { it.equals(selectedFilter, ignoreCase = true) }

            val matchesBest = !showBestOnly ||
                (item.status == FreeAvailabilityStatus.FREE || item.status == FreeAvailabilityStatus.FREE_TIER)

            matchesQuery && matchesFilter && matchesBest
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAX FREE AI RADAR", fontWeight = FontWeight.Bold) },
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
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = {
                    Text(
                        if (isPersian) "جستجوی مدل یا قابلیت رایگان (مثلاً Chat, Coding, Whisper)..."
                        else "Search free AI models or capabilities..."
                    )
                }
            )

            Button(
                onClick = { showBestOnly = !showBestOnly },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Text(
                    text = if (showBestOnly) {
                        if (isPersian) " نمایش همه موارد کاتالوگ" else " Show All Catalog Items"
                    } else {
                        " Find Best Free AI"
                    },
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            filtered.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.modelName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = if (isPersian) item.status.labelFa else item.status.labelEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "${item.providerName} • ${item.bestForTag}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = if (isPersian) item.freeLimitSummaryFa else item.freeLimitSummaryEn,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = if (isPersian) {
                                "ثبت‌نام: ${item.signupRequirementsFa} | کارت بانکی: ${if (item.requiresBankCard) "بله" else "خیر"}"
                            } else {
                                "Signup: ${item.signupRequirementsEn} | Card required: ${if (item.requiresBankCard) "Yes" else "No"}"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Rate: ${item.rateLimit} | Cap: ${item.usageCap}",
                            style = MaterialTheme.typography.labelSmall
                        )

                        Text(
                            text = if (isPersian) "آخرین بررسی: ${item.lastCheckedDate}" else "Last checked: ${item.lastCheckedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.officialApiUrl)))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.OpenInBrowser, contentDescription = null)
                                Text(if (isPersian) " منبع رسمی" else " Official Docs")
                            }
                            Button(
                                onClick = onConnectProvider,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Key, contentDescription = null)
                                Text(if (isPersian) " اتصال API" else " Connect API")
                            }
                        }
                    }
                }
            }
        }
    }
}
