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

/** Optional OpenAI chat provider. API keys are encrypted locally with Android Keystore. */
object OpenAIApiClient {
    private const val PREFS = "jarvis_secure"
    private const val KEY_ALIAS = "jarvis_openai_aes"
    private const val MODEL = "gpt-6.2-astra"

    fun hasKey(context: Context): Boolean = readKey(context).isNotBlank()

    fun saveKey(context: Context, raw: String) {
        val key = raw.trim()
        require(key.startsWith("sk-") && key.length >= 20) { "Enter a valid OpenAI API key." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(key.toByteArray(Charsets.UTF_8))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("openai_key", Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)).apply()
    }

    fun clearKey(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("openai_key").apply()
    }

    private fun readKey(context: Context): String {
        val packed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("openai_key", null) ?: return ""
        return try {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    fun ask(context: Context, prompt: String): String {
        val key = readKey(context)
        if (key.isBlank()) return "OpenAI API key is not configured. Add it in AI Provider Settings."
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $key")
            }
            val body = JSONObject()
                .put("model", MODEL)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", "You are JARVIS AI, a safe Telugu and English personal assistant. Never claim to have completed a device action unless Android confirms it. Ask for confirmation before sensitive actions."))
                    .put(JSONObject().put("role", "user").put("content", prompt.take(20000))))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = try { JSONObject(response).optJSONObject("error")?.optString("message") } catch (_: Exception) { null }
                return "OpenAI API error ($code): ${message ?: "Request failed"}"
            }
            val choices = JSONObject(response).optJSONArray("choices")
            choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.trim()
                ?.takeIf { it.isNotBlank() } ?: "OpenAI returned an empty response."
        } catch (e: Exception) {
            "OpenAI connection error: ${e.message ?: "Check internet and API key"}"
        } finally { connection?.disconnect() }
    }
}
