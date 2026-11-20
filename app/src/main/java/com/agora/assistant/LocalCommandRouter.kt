package com.agora.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import java.util.Locale

/** API-key-free deterministic commands. No arbitrary screen tapping or silent sending. */
class LocalCommandRouter(private val context: Context, private val tts: TextToSpeech?) {
    fun run(raw: String): String {
        val q = raw.trim()
        val lower = q.lowercase(Locale.ROOT)
        return try {
            when {
                lower.contains("time") || lower.contains("సమయం") || lower.contains("టైమ్") ->
                    "The time is ${java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date())}"
                lower.startsWith("search ") || lower.startsWith("వెతుకు ") || lower.startsWith("search for ") -> {
                    val query = when { lower.startsWith("search for ") -> q.substringAfter("search for ", "").trim(); lower.startsWith("వెతుకు ") -> q.substringAfter("వెతుకు ", "").trim(); else -> q.substringAfter(' ').trim() }
                    val i = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(i); "Opening web search for $query"
                }
                lower.contains("settings") || lower.contains("సెట్టింగ్స్") -> {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "Opening settings"
                }
                lower.startsWith("call ") || lower.startsWith("డయల్ ") -> {
                    val number = q.substringAfter(' ').filter { it.isDigit() || it == '+' }
                    if (number.isBlank()) "Please say a phone number" else {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        "Opening dialer. Please confirm the call on screen."
                    }
                }
                lower.startsWith("message ") || lower.startsWith("sms ") -> {
                    val body = q.substringAfter(' ').trim()
                    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", body).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(i); "Opening message composer. Review before sending."
                }
                lower.startsWith("open ") || lower.endsWith(" open chey") || lower.endsWith(" ఓపెన్ చేయి") -> {
                    val appName = if (lower.startsWith("open ")) q.substringAfter(' ').trim() else q.substringBeforeLast(' ').trim()
                    openApp(appName)
                }
                lower.contains("youtube") || lower.contains("యూట్యూబ్") -> openApp("YouTube")
                lower.contains("chrome") -> openApp("Chrome")
                lower.contains("camera") || lower.contains("కెమెరా") -> {
                    context.startActivity(Intent("android.media.action.IMAGE_CAPTURE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "Opening camera"
                }
                lower == "hello" || lower.contains("హలో") || lower.contains("నమస్తే") -> "Hello! I am JARVIS AI. How can I help?"
                else -> "__LOCAL_LLM__:$q"
            }
        } catch (e: Exception) { "I could not complete that action: ${e.message ?: "unknown error"}" }
    }

    private fun openApp(name: String): String {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(launcher, 0).firstOrNull {
            it.loadLabel(pm).toString().contains(name, ignoreCase = true)
        } ?: return "I could not find the app $name"
        val launch = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
            ?: return "Could not open $name"
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        return "Opening ${match.loadLabel(pm)}"
    }
}
