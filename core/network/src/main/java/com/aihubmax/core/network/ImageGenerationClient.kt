package com.aihubmax.core.network

import android.util.Base64
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

sealed interface ImageGenerationResult {
    data class Success(
        val bytes: ByteArray,
        val providerId: String,
        val modelId: String
    ) : ImageGenerationResult

    data class Failure(val reason: String) : ImageGenerationResult
}

@Singleton
class ImageGenerationClient @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun generateWithOpenAi(
        apiKey: String,
        prompt: String,
        modelId: String = "gpt-image-1"
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("model", modelId)
            .put("prompt", prompt)
            .put("size", "1024x1024")

        val request = Request.Builder()
            .url("https://api.openai.com/v1/images/generations")
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext ImageGenerationResult.Failure("HTTP ${response.code}: ${response.message}")
                }
                val b64 = JSONObject(bodyStr)
                    .optJSONArray("data")
                    ?.optJSONObject(0)
                    ?.optString("b64_json")
                    .orEmpty()
                if (b64.isBlank()) {
                    ImageGenerationResult.Failure("No image data returned by OpenAI")
                } else {
                    ImageGenerationResult.Success(
                        bytes = Base64.decode(b64, Base64.DEFAULT),
                        providerId = "openai",
                        modelId = modelId
                    )
                }
            }
        }.getOrElse { ImageGenerationResult.Failure(it.message ?: "Network error") }
    }

    suspend fun generateWithGeminiImagen(
        apiKey: String,
        prompt: String,
        modelId: String = "imagen-3.0-generate-002"
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("instances", JSONArray().put(JSONObject().put("prompt", prompt)))
            .put("parameters", JSONObject().put("sampleCount", 1))

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$modelId:predict")
            .header("x-goog-api-key", apiKey.trim())
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext ImageGenerationResult.Failure("HTTP ${response.code}: ${response.message}")
                }
                val b64 = JSONObject(bodyStr)
                    .optJSONArray("predictions")
                    ?.optJSONObject(0)
                    ?.optString("bytesBase64Encoded")
                    .orEmpty()
                if (b64.isBlank()) {
                    ImageGenerationResult.Failure("No image bytes returned by Gemini Imagen")
                } else {
                    ImageGenerationResult.Success(
                        bytes = Base64.decode(b64, Base64.DEFAULT),
                        providerId = "gemini",
                        modelId = modelId
                    )
                }
            }
        }.getOrElse { ImageGenerationResult.Failure(it.message ?: "Network error") }
    }
}
