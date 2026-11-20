package com.agora.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/** Selects an installed Android TTS voice; actual male/female availability depends on the device TTS engine. */
object VoicePreferences {
    private const val PREFS = "jarvis_voice"
    const val GENDER_MALE = "male"
    const val GENDER_FEMALE = "female"
    const val LANG_AUTO = "auto"
    const val LANG_TELUGU = "te-IN"
    const val LANG_ENGLISH = "en-IN"

    fun gender(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("gender", GENDER_MALE) ?: GENDER_MALE

    fun language(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("language", LANG_AUTO) ?: LANG_AUTO

    fun save(context: Context, gender: String, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("gender", gender).putString("language", language).apply()
    }

    fun apply(tts: TextToSpeech?, context: Context, text: String? = null): Boolean {
        if (tts == null) return false
        val chosenLanguage = language(context)
        val teluguText = text?.any { it in '\u0C00'..'\u0C7F' } == true
        val tag = when (chosenLanguage) {
            LANG_TELUGU -> LANG_TELUGU
            LANG_ENGLISH -> LANG_ENGLISH
            else -> if (teluguText) LANG_TELUGU else LANG_ENGLISH
        }
        val locale = Locale.forLanguageTag(tag)
        val available = tts.isLanguageAvailable(locale)
        if (available < TextToSpeech.LANG_AVAILABLE) {
            tts.language = Locale.forLanguageTag(LANG_ENGLISH)
            return false
        }
        tts.language = locale
        val wanted = gender(context)
        val voices = tts.voices.orEmpty().filter { it.locale.language == locale.language }
        val preferred = voices.firstOrNull { voice ->
            val description = (voice.name + " " + voice.features.joinToString(" ")).lowercase(Locale.ROOT)
            description.contains(wanted)
        } ?: voices.firstOrNull { !it.isNetworkConnectionRequired } ?: voices.firstOrNull()
        if (preferred != null) tts.voice = preferred
        return true
    }

    fun installedVoices(tts: TextToSpeech?): List<Voice> = tts?.voices?.toList().orEmpty()
}
