package com.agora.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * First runnable wake-phrase implementation.
 * Prefers Android's on-device speech recognizer. If the device has no offline
 * recognizer, it falls back to the system recognizer (which may use network).
 * A dedicated Sherpa-ONNX KWS engine is the next integration step for reliable
 * always-on offline wake detection.
 */
class WakeWordService : Service() {
    companion object {
        const val ACTION_START = "com.agora.assistant.START_WAKE"
        const val ACTION_STOP = "com.agora.assistant.STOP_WAKE"
        private const val CHANNEL = "agora_wake"
        private const val NOTIFICATION_ID = 73
    }

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    @Volatile private var speaking = false
    private var waitingForCommand = false
    private var active = false
    private var useOnDevice = false
    private var commandLocale = "te-IN"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("te-IN")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { speaking = true }
                    override fun onDone(utteranceId: String?) {
                        speaking = false
                        main.post { scheduleRestart(350) }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        speaking = false
                        main.post { scheduleRestart(500) }
                    }
                })
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf(); return START_NOT_STICKY
        }
        createChannel()
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("JARVIS AI is active")
            .setContentText("Wake phrase enabled: Hey Jarvis")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(NOTIFICATION_ID, notification)

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition service not available")
            stopSelf(); return START_NOT_STICKY
        }
        useOnDevice = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        active = true
        waitingForCommand = false
        startRecognition()
        return START_NOT_STICKY
    }

    private fun startRecognition() {
        if (!active) return
        recognizer?.destroy()
        recognizer = try {
            if (useOnDevice && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            else SpeechRecognizer.createSpeechRecognizer(this)
        } catch (_: Exception) { SpeechRecognizer.createSpeechRecognizer(this) }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { scheduleRestart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1400 else 650) }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                handleSpeech(spoken)
                if (!speaking) scheduleRestart(450)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                // Wake phrase is acted on final results only to reduce false triggers.
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val locale = if (waitingForCommand) commandLocale else "en-IN"
        val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { recognizer?.startListening(request) } catch (_: Exception) { scheduleRestart(1200) }
    }

    private fun handleSpeech(raw: String) {
        val text = raw.trim()
        if (text.isBlank()) return
        if (waitingForCommand) {
            waitingForCommand = false
            runCommand(text)
            return
        }
        val normalized = text.lowercase(Locale.ROOT)
        val keys = listOf("hey jarvis", "hey jarvis ai", "హే జార్విస్")
        val key = keys.firstOrNull { normalized.contains(it) } ?: return
        val command = text.substring(normalized.indexOf(key) + key.length).trim(' ', ',', '.', '!', '?')
        if (command.isNotBlank()) runCommand(command) else {
            waitingForCommand = true
            speak("Cheppandi")
        }
    }


    private fun runCommand(command: String) {
        val result = LocalCommandRouter(this, tts).run(command)
        if (result.startsWith("__LOCAL_LLM__:")) {
            val prompt = result.substringAfter(":")
            speak("Thinking locally")
            Thread {
                val answer = LocalModelClient.ask(prompt)
                    ?: "Offline AI model is not configured. Built-in offline commands are available; Gemini needs internet."
                main.post { if (active) speak(answer) }
            }.start()
        } else speak(result)
    }

    private fun scheduleRestart(delayMs: Long) {
        if (active && !speaking) main.postDelayed({ if (active && !speaking) startRecognition() }, delayMs)
    }

    private fun speak(text: String) {
        if (text.isBlank()) return
        tts?.language = Locale.forLanguageTag(if (waitingForCommand) "te-IN" else "te-IN")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-${System.currentTimeMillis()}")
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "JARVIS Voice Assistant", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onDestroy() {
        active = false
        main.removeCallbacksAndMessages(null)
        recognizer?.destroy(); recognizer = null
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
