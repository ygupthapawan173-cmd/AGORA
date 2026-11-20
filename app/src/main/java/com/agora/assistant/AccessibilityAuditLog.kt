package com.agora.assistant

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stores action metadata only; never stores screen text, passwords, or typed content. */
object AccessibilityAuditLog {
    fun record(context: Context, packageName: String, action: String, result: String) {
        val prefs = context.getSharedPreferences("jarvis_accessibility_log", Context.MODE_PRIVATE)
        // Never persist a screen label or visible text supplied by a Click action.
        val safeAction = if (action.startsWith("click ", ignoreCase = true)) "Click visible control" else action.take(40)
        val safeResult = when {
            action.startsWith("click ", ignoreCase = true) && result.startsWith("Clicked", ignoreCase = true) -> "Click completed"
            action.startsWith("click ", ignoreCase = true) -> "Click not completed"
            else -> result.take(100)
        }
        val line = "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())} | ${packageName.substringAfterLast('.')} | $safeAction | $safeResult"
        val old = prefs.getString("entries", "").orEmpty()
        prefs.edit().putString("entries", (old + "\n" + line).takeLast(6000)).apply()
    }
    fun get(context: Context): String = context.getSharedPreferences("jarvis_accessibility_log", Context.MODE_PRIVATE).getString("entries", "").orEmpty().trim()
    fun clear(context: Context) { context.getSharedPreferences("jarvis_accessibility_log", Context.MODE_PRIVATE).edit().clear().apply() }
}
