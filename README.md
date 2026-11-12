# AI HUB MAX — Android AI Super App

Modular Native Android application built with **Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, OkHttp, and Android Keystore** (minSdk 26 / Android 8.0+, full Persian RTL & English LTR support).

## Modules
- `:app` — Application entry point & Navigation Compose graph
- `:core:common` — Local intent classification & shared models
- `:core:design` — Material 3 light/dark theme
- `:core:security` — AES-256-GCM encryption via Android Keystore + DataStore
- `:core:network` — Real provider adapters (Gemini, OpenAI, OpenRouter, Groq, DeepSeek) & Image client
- `:core:routing` — `MaxAiRouter` (MAX FREE, MAX PERSIAN, MAX SPEED, MAX QUALITY, MAX CODING, MAX BALANCED)
- `:core:database` — Room database for offline chat & token usage history
- `:core:storage` — Local media gallery storage
- `:features:home` — AI MAX Dashboard & ASK AI MAX
- `:features:api_manager` — API Center & One-Tap Clipboard Auto Connect
- `:features:free_ai` — MAX FREE AI RADAR with verified Free Tier catalog
- `:features:chat` — Universal AI Chat & Multi-AI Debate
- `:features:image_studio` — Image Studio & Local Gallery
- `:features:video_studio` — 9-stage Video Autopilot (Brief/Script/Storyboard + External Video Services)
- `:features:agents` — Agent Builder & Step-by-Step Workflow Runner
- `:features:documents` — Document AI (File picker + text/PDF inspection + LLM analysis)
- `:features:usage` — Cost Control & Token Usage Tracker

## Build Debug / Release APK
### Option 1: Android Studio
1. Open this folder in **Android Studio** (Ladybug or newer, JDK 17).
2. Let Gradle sync dependencies.
3. Run **Build > Build Bundle(s) / APK(s) > Build APK(s)** (`app-debug.apk`).
4. For Release APK, configure signing in `Build > Generate Signed Bundle / APK`.

### Option 2: GitHub Actions (Automated Debug APK)
1. Push this repository to GitHub.
2. Open the **Actions** tab and run **Build Debug APK** (`.github/workflows/build-apk.yml`).
3. Download `ai-hub-max-debug-apk` from the workflow artifacts.
