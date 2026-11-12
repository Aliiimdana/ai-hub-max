package com.aihubmax.core.routing

import com.aihubmax.core.common.AiTaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaxAiRouterTest {
    private val router = MaxAiRouter()

    @Test
    fun selectsGeminiForPersianModeWhenConnected() {
        val decision = router.route(
            category = AiTaskCategory.GENERAL_CHAT,
            mode = RouterMode.MAX_PERSIAN,
            connectedProviderIds = setOf("gemini", "groq")
        )
        assertTrue(decision is RoutingDecision.Selected)
        assertEquals("gemini", (decision as RoutingDecision.Selected).candidate.providerId)
    }

    @Test
    fun returnsNoConnectedProviderWhenOnlyPaidConnectedInFreeMode() {
        val decision = router.route(
            category = AiTaskCategory.CODING,
            mode = RouterMode.MAX_FREE,
            connectedProviderIds = setOf("openai")
        )
        assertTrue(decision is RoutingDecision.NoConnectedProvider)
    }
}
