package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class AndroidSpeechEngine(private val context: Context) : SpeechEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listener: SpeechEngineListener? = null
    private var isListening = false
    private var currentLanguage = "bn-BD"

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override val isCurrentlyListening: Boolean
        get() = isListening

    override fun startListening(languageCode: String, listener: SpeechEngineListener) {
        this.listener = listener
        this.currentLanguage = languageCode

        mainHandler.post {
            try {
                if (recognizer == null) {
                    recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                recognizer?.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                isListening = false
                listener.onError(-1, "Speech recognition initialization error: ${e.message}")
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                isListening = false
                recognizer?.stopListening()
            } catch (_: Exception) {}
        }
    }

    override fun pauseListening() {
        mainHandler.post {
            try {
                isListening = false
                recognizer?.cancel()
            } catch (_: Exception) {}
        }
    }

    override fun resumeListening() {
        listener?.let {
            startListening(currentLanguage, it)
        }
    }

    override fun release() {
        mainHandler.post {
            try {
                isListening = false
                recognizer?.destroy()
                recognizer = null
                listener = null
            } catch (_: Exception) {}
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listener?.onReadyForSpeech()
            }

            override fun onBeginningOfSpeech() {
                listener?.onBeginningOfSpeech()
            }

            override fun onRmsChanged(rmsdB: Float) {
                listener?.onRmsChanged(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                listener?.onEndOfSpeech()
            }

            override fun onError(error: Int) {
                isListening = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout (silence)"
                    else -> "Speech recognition error code: $error"
                }
                listener?.onError(error, msg)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""
                listener?.onResults(recognizedText)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull() ?: ""
                if (partialText.isNotBlank()) {
                    listener?.onPartialResults(partialText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
