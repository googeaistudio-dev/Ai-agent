package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class SpeechSynthesizer(
    private val context: Context,
    var onStateChange: ((isSpeaking: Boolean) -> Unit)? = null
) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isCurrentlySpeaking = false

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var speechPitch: Float = 1.0f
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    var isMuted: Boolean = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                configureLocale("bn")
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(speechPitch)
                setupProgressListener()
            }
        }
    }

    private fun configureLocale(langCode: String) {
        val targetLocale = if (langCode.startsWith("bn")) {
            Locale("bn", "BD")
        } else {
            Locale.US
        }

        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to default or English
            tts?.language = Locale.getDefault()
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isCurrentlySpeaking = true
                onStateChange?.invoke(true)
            }

            override fun onDone(utteranceId: String?) {
                isCurrentlySpeaking = false
                onStateChange?.invoke(false)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isCurrentlySpeaking = false
                onStateChange?.invoke(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isCurrentlySpeaking = false
                onStateChange?.invoke(false)
            }
        })
    }

    fun speak(text: String, languageCode: String = "bn-BD", onComplete: (() -> Unit)? = null) {
        if (isMuted || text.isBlank() || !isInitialized) {
            onComplete?.invoke()
            return
        }

        // Clean text of markdown formatting for speech clarity
        val cleanText = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("[#*_`~]"), "")
            .trim()

        configureLocale(languageCode)
        val utteranceId = "nafsi_speech_${System.currentTimeMillis()}"

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
            isCurrentlySpeaking = false
            onStateChange?.invoke(false)
        } catch (_: Exception) {}
    }

    fun isSpeaking(): Boolean = isCurrentlySpeaking

    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
