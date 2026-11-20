package com.agora.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Gemini REST client. The key is encrypted with an Android Keystore AES key before local storage. */
object GeminiApiClient {
    private const val PREFS = "jarvis_secure"
    private const val KEY_ALIAS = "jarvis_gemini_aes"
    private const val MODEL = "gemini-3.5-flash-lite"

    fun hasKey(context: Context): Boolean = readKey(context).isNotBlank()

    fun saveKey(context: Context, raw: String) {
        val key = raw.trim()
        require(key.length >= 20) { "Please enter a valid Gemini API key." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(key.toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("gemini_key", Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun clearKey(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("gemini_key").apply()
    }

    private fun readKey(context: Context): String {
        val packed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("gemini_key", null) ?: return ""
        return try {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, 12)
            val encrypted = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    fun usage(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences("jarvis_limits", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        if (prefs.getString("day", "") != today) prefs.edit().putString("day", today).putInt("requests", 0).apply()
        return prefs.getInt("requests", 0) to 20
    }

    fun ask(context: Context, prompt: String): String {
        val key = readKey(context)
        if (key.isBlank()) return "API key లేదు. Home లో API Settings తెరిచి Gemini key add చేయండి."
        val prefs = context.getSharedPreferences("jarvis_limits", Context.MODE_PRIVATE)
        val (used, dailyLimit) = usage(context)
        if (used >= dailyLimit) return "ఈ రోజు app request limit (20) పూర్తయింది. రేపు మళ్లీ ప్రయత్నించండి. ఇది app-side limit మాత్రమే; Google billing limit కాదు."
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", key)
            }
            val body = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text",
                    "You are JARVIS AI, a helpful Telugu and English Android assistant. Reply clearly and concisely. Never claim an action was completed unless the app confirms it."))))
                .put("contents", JSONArray().put(JSONObject().put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", prompt.take(12000))))))
                .put("generationConfig", JSONObject().put("temperature", 0.5).put("maxOutputTokens", 700))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val message = try { JSONObject(response).optJSONObject("error")?.optString("message") } catch (_: Exception) { null }
                return "Gemini API error ($code): ${message ?: "Request failed"}"
            }
            prefs.edit().putInt("requests", used + 1).apply()
            val json = JSONObject(response)
            val parts = json.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")
            val answer = buildString {
                if (parts != null) for (i in 0 until parts.length()) append(parts.optJSONObject(i)?.optString("text").orEmpty())
            }.trim()
            val tokens = json.optJSONObject("usageMetadata")?.optInt("totalTokenCount", 0) ?: 0
            if (answer.isBlank()) return "Gemini నుంచి text response రాలేదు. మళ్లీ ప్రయత్నించండి."
            return answer + if (tokens > 0) "\\n\\n— Gemini • $tokens tokens • ${used + 1}/20 today" else "\\n\\n— Gemini • ${used + 1}/20 today"
        } catch (e: Exception) {
            return "Connection error: ${e.message ?: "Please check internet"}"
        } finally {
            connection?.disconnect()
        }
    }
}
