package com.aihubmax.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalIntentClassifierTest {
    @Test
    fun detectsImageGenerationInPersian() {
        val result = LocalIntentClassifier.classify("یک تصویر سینمایی از همدان در آینده بساز")
        assertEquals(AiTaskCategory.IMAGE_GENERATION, result)
    }

    @Test
    fun detectsVideoWhenConvertingImageToVideo() {
        val result = LocalIntentClassifier.classify("این تصویر را به ویدئو تبدیل کن")
        assertEquals(AiTaskCategory.VIDEO_GENERATION, result)
    }

    @Test
    fun detectsDocumentSummary() {
        val result = LocalIntentClassifier.classify("این PDF را خلاصه کن")
        assertEquals(AiTaskCategory.DOCUMENT_AI, result)
    }
}
