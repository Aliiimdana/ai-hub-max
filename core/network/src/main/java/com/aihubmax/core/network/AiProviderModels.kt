package com.aihubmax.core.network

data class ProviderSpec(
    val id: String,
    val displayName: String,
    val officialKeyUrl: String,
    val docsUrl: String,
    val keyPrefixHint: String,
    val isFreeTierAvailable: Boolean = false
)

data class DiscoveredModel(
    val id: String,
    val displayName: String,
    val capabilities: Set<String>
)

sealed interface ProviderValidationResult {
    data class Success(val models: List<DiscoveredModel>) : ProviderValidationResult
    data class Failure(val reason: String, val httpCode: Int? = null) : ProviderValidationResult
}

sealed interface ChatInferenceResult {
    data class Success(
        val text: String,
        val providerId: String,
        val modelId: String,
        val totalTokens: Int? = null,
        val latencyMs: Long = 0L
    ) : ChatInferenceResult

    data class Failure(
        val providerId: String,
        val modelId: String,
        val reason: String
    ) : ChatInferenceResult
}

interface AiProviderAdapter {
    val spec: ProviderSpec
    suspend fun validateAndDiscoverModels(apiKey: String): ProviderValidationResult
    suspend fun generateText(apiKey: String, modelId: String, prompt: String): ChatInferenceResult
}
