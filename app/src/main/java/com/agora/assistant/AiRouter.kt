package com.agora.assistant

import android.content.Context

object AiProviderPreferences {
    const val AUTO = "auto"
    const val GEMINI = "gemini"
    const val OPENAI = "openai"
    const val LOCAL = "local"
    private const val PREFS = "jarvis_ai_preferences"
    fun provider(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("provider", AUTO) ?: AUTO
    fun save(context: Context, provider: String) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("provider", provider).apply() }
    fun label(provider: String): String = when (provider) { GEMINI -> "Gemini"; OPENAI -> "OpenAI"; LOCAL -> "Local AI"; else -> "Auto" }
}

/** One routing surface for chat, voice and agent planning. Auto uses configured providers only. */
object AiRouter {
    fun activeLabel(context: Context): String {
        val selected = AiProviderPreferences.provider(context)
        return when (selected) {
            AiProviderPreferences.GEMINI -> "Gemini"
            AiProviderPreferences.OPENAI -> "OpenAI"
            AiProviderPreferences.LOCAL -> "Local AI"
            else -> if (GeminiApiClient.hasKey(context)) "Gemini · Auto" else "Local AI · Auto"
        }
    }

    fun ask(context: Context, prompt: String): String {
        return when (AiProviderPreferences.provider(context)) {
            AiProviderPreferences.GEMINI -> if (GeminiApiClient.hasKey(context)) GeminiApiClient.ask(context, prompt) else "Gemini key is missing. Add it in Settings."
            AiProviderPreferences.OPENAI -> OpenAIApiClient.ask(context, prompt)
            AiProviderPreferences.LOCAL -> LocalModelClient.ask(prompt) ?: "Local AI is unavailable. Start the llama.cpp server in Termux at localhost:8080."
            // Auto never silently spends on OpenAI. Select OpenAI explicitly to use its separately billed API.
            else -> if (GeminiApiClient.hasKey(context)) GeminiApiClient.ask(context, prompt)
                else LocalModelClient.ask(prompt) ?: "No free/local provider is ready. Add a Gemini key or start the local model server. Select OpenAI explicitly to use its paid API."
        }
    }
}
