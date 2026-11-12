package com.aihubmax.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

open class OpenAiCompatibleProviderAdapter(
    private val client: OkHttpClient,
    private val modelsEndpointUrl: String,
    private val chatCompletionsUrl: String,
    override val spec: ProviderSpec
) : AiProviderAdapter {

    override suspend fun validateAndDiscoverModels(apiKey: String): ProviderValidationResult =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(modelsEndpointUrl)
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .get()
                .build()

            runCatching {
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext ProviderValidationResult.Failure(
                            reason = "HTTP ${response.code}: ${response.message}",
                            httpCode = response.code
                        )
                    }
                    val json = JSONObject(bodyStr)
                    val array = json.optJSONArray("data")
                    val models = mutableListOf<DiscoveredModel>()
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val id = obj.optString("id")
                            if (id.isNotBlank()) {
                                models.add(
                                    DiscoveredModel(
                                        id = id,
                                        displayName = id,
                                        capabilities = inferCapabilities(id)
                                    )
                                )
                            }
                        }
                    }
                    ProviderValidationResult.Success(models)
                }
            }.getOrElse { err ->
                ProviderValidationResult.Failure(err.message ?: "Network error")
            }
        }

    override suspend fun generateText(
        apiKey: String,
        modelId: String,
        prompt: String
    ): ChatInferenceResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val payload = JSONObject()
            .put("model", modelId)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
                )
            )

        val request = Request.Builder()
            .url(chatCompletionsUrl)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val latency = System.currentTimeMillis() - start
                if (!response.isSuccessful) {
                    return@withContext ChatInferenceResult.Failure(
                        providerId = spec.id,
                        modelId = modelId,
                        reason = "HTTP ${response.code}: ${response.message}"
                    )
                }
                val json = JSONObject(bodyStr)
                val text = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                val tokens = json.optJSONObject("usage")?.optInt("total_tokens")
                if (text.isBlank()) {
                    ChatInferenceResult.Failure(spec.id, modelId, "Empty response from ${spec.displayName}")
                } else {
                    ChatInferenceResult.Success(
                        text = text,
                        providerId = spec.id,
                        modelId = modelId,
                        totalTokens = tokens,
                        latencyMs = latency
                    )
                }
            }
        }.getOrElse { err ->
            ChatInferenceResult.Failure(spec.id, modelId, err.message ?: "Network error")
        }
    }

    private fun inferCapabilities(modelId: String): Set<String> {
        val lower = modelId.lowercase()
        val caps = mutableSetOf("chat")
        if (lower.contains("vision") || lower.contains("4o") || lower.contains("vl")) caps.add("vision")
        if (lower.contains("coder") || lower.contains("code")) caps.add("coding")
        if (lower.contains("dall-e") || lower.contains("image")) caps.add("image")
        if (lower.contains("whisper") || lower.contains("tts") || lower.contains("audio")) caps.add("audio")
        return caps
    }
}

@Singleton
class ProviderRegistry @Inject constructor(
    client: OkHttpClient,
    geminiAdapter: GeminiProviderAdapter
) {
    val adapters: List<AiProviderAdapter> = listOf(
        geminiAdapter,
        OpenAiCompatibleProviderAdapter(
            client = client,
            modelsEndpointUrl = "https://api.openai.com/v1/models",
            chatCompletionsUrl = "https://api.openai.com/v1/chat/completions",
            spec = ProviderSpec(
                id = "openai",
                displayName = "OpenAI",
                officialKeyUrl = "https://platform.openai.com/api-keys",
                docsUrl = "https://platform.openai.com/docs",
                keyPrefixHint = "sk-",
                isFreeTierAvailable = false
            )
        ),
        OpenAiCompatibleProviderAdapter(
            client = client,
            modelsEndpointUrl = "https://openrouter.ai/api/v1/models",
            chatCompletionsUrl = "https://openrouter.ai/api/v1/chat/completions",
            spec = ProviderSpec(
                id = "openrouter",
                displayName = "OpenRouter",
                officialKeyUrl = "https://openrouter.ai/keys",
                docsUrl = "https://openrouter.ai/docs",
                keyPrefixHint = "sk-or-",
                isFreeTierAvailable = true
            )
        ),
        OpenAiCompatibleProviderAdapter(
            client = client,
            modelsEndpointUrl = "https://api.groq.com/openai/v1/models",
            chatCompletionsUrl = "https://api.groq.com/openai/v1/chat/completions",
            spec = ProviderSpec(
                id = "groq",
                displayName = "Groq",
                officialKeyUrl = "https://console.groq.com/keys",
                docsUrl = "https://console.groq.com/docs",
                keyPrefixHint = "gsk_",
                isFreeTierAvailable = true
            )
        ),
        OpenAiCompatibleProviderAdapter(
            client = client,
            modelsEndpointUrl = "https://api.deepseek.com/models",
            chatCompletionsUrl = "https://api.deepseek.com/chat/completions",
            spec = ProviderSpec(
                id = "deepseek",
                displayName = "DeepSeek",
                officialKeyUrl = "https://platform.deepseek.com/api_keys",
                docsUrl = "https://api-docs.deepseek.com/",
                keyPrefixHint = "sk-",
                isFreeTierAvailable = false
            )
        )
    )

    fun getAdapter(providerId: String): AiProviderAdapter? =
        adapters.firstOrNull { it.spec.id == providerId }
}
