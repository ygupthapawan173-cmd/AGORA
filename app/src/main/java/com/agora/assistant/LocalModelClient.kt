package com.agora.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Connects to an optional llama.cpp server running locally in Termux. No API key is sent. */
object LocalModelClient {
    private const val ENDPOINT = "http://localhost:8080/v1/chat/completions"

    fun ask(prompt: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 1800
                readTimeout = 25000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val body = JSONObject()
                .put("model", "local-model")
                .put("temperature", 0.4)
                .put("max_tokens", 220)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", "You are JARVIS AI, a concise Telugu and English Android assistant. Reply briefly and never claim you performed phone actions unless the app confirms them."))
                    .put(JSONObject().put("role", "user").put("content", prompt)))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return null
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(response).optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                ?.trim()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
