package com.aihubmax.core.common

enum class AiTaskCategory(
    val titleFa: String,
    val titleEn: String,
    val suggestedWorkflowFa: String,
    val suggestedWorkflowEn: String,
    val requiredCapability: String
) {
    IMAGE_GENERATION(
        titleFa = "تولید تصویر",
        titleEn = "Image Generation",
        suggestedWorkflowFa = "تشخیص سبک ← بهبود پرامپت ← انتخاب مدل تصویر ← ذخیره در گالری",
        suggestedWorkflowEn = "Style Detection -> Prompt Enhancement -> Image Model -> Gallery",
        requiredCapability = "Image API (Gemini Image / FLUX / OpenAI Image)"
    ),
    VIDEO_GENERATION(
        titleFa = "تولید ویدئو",
        titleEn = "Video Generation",
        suggestedWorkflowFa = "خلاصه ایده ← سناریو ← استوری‌بورد ← تولید صحنه ← رندر نهایی",
        suggestedWorkflowEn = "Creative Brief -> Script -> Storyboard -> Scene Gen -> Render",
        requiredCapability = "Video API (Veo / Runway / Fal.ai / Replicate)"
    ),
    WEB_RESEARCH(
        titleFa = "تحقیق وب",
        titleEn = "Web Research",
        suggestedWorkflowFa = "جستجوی زنده ← بررسی منابع معتبر ← تحلیل مقایسه‌ای ← گزارش",
        suggestedWorkflowEn = "Live Search -> Source Verification -> Comparison -> Report",
        requiredCapability = "Web Search + Reasoning LLM (Perplexity / Gemini)"
    ),
    DOCUMENT_AI(
        titleFa = "تحلیل سند",
        titleEn = "Document AI",
        suggestedWorkflowFa = "استخراج متن/OCR ← تحلیل ساختار ← خلاصه‌سازی ← خروجی",
        suggestedWorkflowEn = "Text/OCR Extraction -> Structure Analysis -> Summary -> Export",
        requiredCapability = "Long-Context / Vision LLM"
    ),
    CODING(
        titleFa = "کدنویسی و طراحی وب",
        titleEn = "Coding & Web Design",
        suggestedWorkflowFa = "تحلیل نیازمندی ← معماری ← تولید کد ← بررسی خطا",
        suggestedWorkflowEn = "Requirements -> Architecture -> Code Generation -> Review",
        requiredCapability = "Coding LLM (Claude / DeepSeek / Gemini / OpenAI)"
    ),
    AGENT_WORKFLOW(
        titleFa = "ساخت Agent و اتوماسیون",
        titleEn = "Agent & Workflow",
        suggestedWorkflowFa = "تعریف هدف ← انتخاب ابزارهای مجاز ← تنظیم حافظه ← اجرای مرحله‌ای",
        suggestedWorkflowEn = "Goal -> Allowed Tools -> Memory -> Step Execution",
        requiredCapability = "Tool-Calling LLM + Local Workflow Engine"
    ),
    GENERAL_CHAT(
        titleFa = "گفتگوی هوشمند",
        titleEn = "General AI Chat",
        suggestedWorkflowFa = "تحلیل درخواست ← مسیریابی هوشمند ← پاسخ متنی",
        suggestedWorkflowEn = "Prompt Analysis -> Smart Routing -> Text Response",
        requiredCapability = "Any Connected Chat LLM"
    )
}

object LocalIntentClassifier {
    fun classify(prompt: String): AiTaskCategory {
        val text = prompt.trim().lowercase()
        if (text.isBlank()) return AiTaskCategory.GENERAL_CHAT

        return when {
            containsAny(text, "تصویر", "عکس", "پوستر", "لوگو", "image", "draw", "photo", "illustration") &&
                !containsAny(text, "به ویدئو", "to video") -> AiTaskCategory.IMAGE_GENERATION

            containsAny(text, "ویدئو", "ویدیو", "کلیپ", "تیزر", "به ویدئو", "video", "clip", "storyboard") ->
                AiTaskCategory.VIDEO_GENERATION

            containsAny(text, "تحقیق", "جستجو", "مقایسه", "اخبار", "منبع", "research", "search", "latest") ->
                AiTaskCategory.WEB_RESEARCH

            containsAny(text, "pdf", "فایل", "سند", "خلاصه کن", "اکسل", "docx", "xlsx", "document", "summarize") ->
                AiTaskCategory.DOCUMENT_AI

            containsAny(text, "کد", "وبسایت", "سایت", "برنامه", "اپ", "code", "website", "kotlin", "html") ->
                AiTaskCategory.CODING

            containsAny(text, "agent", "ایجنت", "اتوماسیون", "workflow", "مدیریت پروژه", "فرآیند") ->
                AiTaskCategory.AGENT_WORKFLOW

            else -> AiTaskCategory.GENERAL_CHAT
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
