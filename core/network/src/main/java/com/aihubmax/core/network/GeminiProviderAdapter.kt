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

@Singleton
class GeminiProviderAdapter @Inject constructor(
    private val client: OkHttpClient
) : AiProviderAdapter {

    override val spec = ProviderSpec(
        id = "gemini",
        displayName = "Google Gemini",
        officialKeyUrl = "https://aistudio.google.com/app/apikey",
        docsUrl = "https://ai.google.dev/gemini-api/docs",
        keyPrefixHint = "AIza",
        isFreeTierAvailable = true
    )

    override suspend fun validateAndDiscoverModels(apiKey: String): ProviderValidationResult =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models")
                .header("x-goog-api-key", apiKey.trim())
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
                    val array = json.optJSONArray("models")
                    val models = mutableListOf<DiscoveredModel>()
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val rawName = obj.optString("name")
                            val id = rawName.removePrefix("models/")
                            val displayName = obj.optString("displayName", id)
                            val methods = obj.optJSONArray("supportedGenerationMethods")
                            val caps = mutableSetOf<String>()
                            if (methods != null) {
                                for (m in 0 until methods.length()) {
                                    caps.add(methods.getString(m))
                                }
                            }
                            if (id.isNotBlank()) {
                                models.add(DiscoveredModel(id = id, displayName = displayName, capabilities = caps))
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
        val payload = JSONObject().put(
            "contents",
            JSONArray().put(
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", prompt))
                )
            )
        )
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent")
            .header("x-goog-api-key", apiKey.trim())
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
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    .orEmpty()
                val tokens = json.optJSONObject("usageMetadata")?.optInt("totalTokenCount")
                if (text.isBlank()) {
                    ChatInferenceResult.Failure(spec.id, modelId, "Empty response from Gemini")
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
}
