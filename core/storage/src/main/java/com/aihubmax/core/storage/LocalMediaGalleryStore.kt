package com.aihubmax.core.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SavedImageItem(
    val id: String,
    val prompt: String,
    val providerId: String,
    val modelId: String,
    val filePath: String,
    val createdAtMs: Long
)

@Singleton
class LocalMediaGalleryStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val galleryDir: File by lazy {
        File(context.filesDir, "ai_hub_gallery").apply { mkdirs() }
    }
    private val indexFile: File by lazy { File(galleryDir, "index.json") }

    private val _images = MutableStateFlow<List<SavedImageItem>>(loadIndex())
    val images: StateFlow<List<SavedImageItem>> = _images.asStateFlow()

    suspend fun saveGeneratedImage(
        bytes: ByteArray,
        prompt: String,
        providerId: String,
        modelId: String
    ): SavedImageItem = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val imageFile = File(galleryDir, "$id.png")
        imageFile.writeBytes(bytes)

        val item = SavedImageItem(
            id = id,
            prompt = prompt,
            providerId = providerId,
            modelId = modelId,
            filePath = imageFile.absolutePath,
            createdAtMs = System.currentTimeMillis()
        )
        val updated = listOf(item) + _images.value
        persistIndex(updated)
        _images.value = updated
        item
    }

    suspend fun deleteImage(id: String) = withContext(Dispatchers.IO) {
        val current = _images.value
        val target = current.firstOrNull { it.id == id }
        if (target != null) {
            File(target.filePath).delete()
        }
        val updated = current.filterNot { it.id == id }
        persistIndex(updated)
        _images.value = updated
    }

    private fun loadIndex(): List<SavedImageItem> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(indexFile.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val path = obj.optString("filePath")
                    if (File(path).exists()) {
                        add(
                            SavedImageItem(
                                id = obj.optString("id"),
                                prompt = obj.optString("prompt"),
                                providerId = obj.optString("providerId"),
                                modelId = obj.optString("modelId"),
                                filePath = path,
                                createdAtMs = obj.optLong("createdAtMs")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistIndex(items: List<SavedImageItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("prompt", item.prompt)
                    .put("providerId", item.providerId)
                    .put("modelId", item.modelId)
                    .put("filePath", item.filePath)
                    .put("createdAtMs", item.createdAtMs)
            )
        }
        indexFile.writeText(arr.toString())
    }
}
