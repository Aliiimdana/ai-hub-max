package com.aihubmax.core.routing

import com.aihubmax.core.common.AiTaskCategory
import javax.inject.Inject
import javax.inject.Singleton

enum class RouterMode(val labelFa: String, val labelEn: String) {
    MAX_BALANCED("متعادل", "MAX BALANCED"),
    MAX_FREE("رایگان", "MAX FREE"),
    MAX_QUALITY("کیفیت بالا", "MAX QUALITY"),
    MAX_SPEED("سرعت بالا", "MAX SPEED"),
    MAX_PERSIAN("بهینه برای فارسی", "MAX PERSIAN"),
    MAX_CODING("کدنویسی", "MAX CODING"),
    MAX_CREATIVE("خلاقانه", "MAX CREATIVE"),
    MAX_LOW_COST("کم‌هزینه", "MAX LOW COST"),
    MANUAL("انتخاب دستی", "MANUAL")
}

data class RouteCandidate(
    val providerId: String,
    val providerName: String,
    val modelId: String,
    val isFreeTierAvailable: Boolean,
    val speedScore: Int,
    val qualityScore: Int,
    val persianScore: Int,
    val codingScore: Int,
    val estimatedCostNoteFa: String,
    val estimatedCostNoteEn: String
)

sealed interface RoutingDecision {
    data class Selected(
        val candidate: RouteCandidate,
        val reasonFa: String,
        val reasonEn: String
    ) : RoutingDecision

    data class NoConnectedProvider(
        val recommendedProviderId: String,
        val recommendedProviderName: String,
        val reasonFa: String,
        val reasonEn: String
    ) : RoutingDecision
}

@Singleton
class MaxAiRouter @Inject constructor() {

    private val candidates = listOf(
        RouteCandidate(
            providerId = "gemini",
            providerName = "Google Gemini",
            modelId = "gemini-2.5-flash",
            isFreeTierAvailable = true,
            speedScore = 9,
            qualityScore = 9,
            persianScore = 10,
            codingScore = 8,
            estimatedCostNoteFa = "دارای Free Tier در Google AI Studio (با محدودیت RPM/RPD)",
            estimatedCostNoteEn = "Free Tier available via Google AI Studio (rate-limited)"
        ),
        RouteCandidate(
            providerId = "groq",
            providerName = "Groq",
            modelId = "llama-3.3-70b-versatile",
            isFreeTierAvailable = true,
            speedScore = 10,
            qualityScore = 8,
            persianScore = 7,
            codingScore = 8,
            estimatedCostNoteFa = "دارای Free Plan (محدودیت روزانه هر مدل)",
            estimatedCostNoteEn = "Free Plan available (per-model daily cap)"
        ),
        RouteCandidate(
            providerId = "openrouter",
            providerName = "OpenRouter",
            modelId = "openrouter/free",
            isFreeTierAvailable = true,
            speedScore = 7,
            qualityScore = 8,
            persianScore = 8,
            codingScore = 8,
            estimatedCostNoteFa = "مسیر رایگان openrouter/free ($0 هر توکن، محدودیت 20 RPM)",
            estimatedCostNoteEn = "openrouter/free ($0/token, 20 RPM rate limit)"
        ),
        RouteCandidate(
            providerId = "deepseek",
            providerName = "DeepSeek",
            modelId = "deepseek-chat",
            isFreeTierAvailable = false,
            speedScore = 8,
            qualityScore = 9,
            persianScore = 8,
            codingScore = 10,
            estimatedCostNoteFa = "پولی (هزینه بر اساس مصرف توکن)",
            estimatedCostNoteEn = "Paid API (per-token billing)"
        ),
        RouteCandidate(
            providerId = "openai",
            providerName = "OpenAI",
            modelId = "gpt-4o-mini",
            isFreeTierAvailable = false,
            speedScore = 8,
            qualityScore = 9,
            persianScore = 9,
            codingScore = 9,
            estimatedCostNoteFa = "پولی (هزینه بر اساس مصرف توکن)",
            estimatedCostNoteEn = "Paid API (per-token billing)"
        )
    )

    fun route(
        category: AiTaskCategory,
        mode: RouterMode,
        connectedProviderIds: Set<String>
    ): RoutingDecision {
        val available = candidates.filter { it.providerId in connectedProviderIds }
        val pool = if (mode == RouterMode.MAX_FREE) {
            available.filter { it.isFreeTierAvailable }
        } else {
            available
        }

        if (pool.isEmpty()) {
            val recommended = if (mode == RouterMode.MAX_CODING) {
                candidates.first { it.providerId == "gemini" }
            } else {
                candidates.first { it.isFreeTierAvailable }
            }
            return RoutingDecision.NoConnectedProvider(
                recommendedProviderId = recommended.providerId,
                recommendedProviderName = recommended.providerName,
                reasonFa = "هیچ Provider متصلی برای حالت ${mode.labelFa} یافت نشد. پیشنهاد: اتصال ${recommended.providerName}",
                reasonEn = "No connected provider matches ${mode.labelEn}. Recommended: connect ${recommended.providerName}"
            )
        }

        val best = when (mode) {
            RouterMode.MAX_SPEED -> pool.maxBy { it.speedScore }
            RouterMode.MAX_QUALITY -> pool.maxBy { it.qualityScore }
            RouterMode.MAX_PERSIAN -> pool.maxBy { it.persianScore }
            RouterMode.MAX_CODING -> pool.maxBy { it.codingScore }
            RouterMode.MAX_FREE, RouterMode.MAX_LOW_COST ->
                pool.sortedByDescending { it.isFreeTierAvailable }
                    .maxBy { it.qualityScore + it.speedScore }
            RouterMode.MAX_CREATIVE, RouterMode.MAX_BALANCED, RouterMode.MANUAL ->
                pool.maxBy {
                    when (category) {
                        AiTaskCategory.CODING -> it.codingScore * 2 + it.qualityScore
                        else -> it.qualityScore + it.persianScore + it.speedScore
                    }
                }
        }

        return RoutingDecision.Selected(
            candidate = best,
            reasonFa = "انتخاب خودکار بر اساس حالت ${mode.labelFa} برای ${category.titleFa}",
            reasonEn = "Auto-selected via ${mode.labelEn} for ${category.titleEn}"
        )
    }
}
