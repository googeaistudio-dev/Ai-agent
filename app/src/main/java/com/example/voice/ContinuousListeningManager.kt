package com.example.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContinuousListeningManager(
    private val context: Context,
    private val speechEngine: SpeechEngine,
    val synthesizer: SpeechSynthesizer
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isContinuousListening = MutableStateFlow(false)
    val isContinuousListening: StateFlow<Boolean> = _isContinuousListening.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    var languageCode: String = "bn-BD"
    var echoGuardDelayMs: Long = 700L
    var onCommandRecognized: ((text: String) -> Unit)? = null

    private var isPaused = false
    private var isMutedDueToSpeech = false

    private val speechListener = object : SpeechEngineListener {
        override fun onReadyForSpeech() {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            _rmsLevel.value = rmsdB
        }

        override fun onPartialResults(partialText: String) {
            _partialText.value = partialText
        }

        override fun onResults(text: String) {
            _isListening.value = false
            _partialText.value = ""
            val trimmed = text.trim()

            if (trimmed.isNotBlank() && !isMutedDueToSpeech) {
                _lastRecognizedText.value = trimmed
                onCommandRecognized?.invoke(trimmed)
            }

            // If continuous listening is enabled and we are not paused or currently speaking, rearm
            if (_isContinuousListening.value && !isPaused && !_isSpeaking.value) {
                scheduleRestart(350L)
            }
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            _isListening.value = false
            _partialText.value = ""

            // If microphone permission is missing or speech engine is unavailable, do not loop
            if (errorCode == 9 /* ERROR_INSUFFICIENT_PERMISSIONS */ || errorCode == -1) {
                _isContinuousListening.value = false
                isPaused = true
                return
            }

            // On silence or timeout, smoothly restart if continuous mode is enabled
            if (_isContinuousListening.value && !isPaused && !_isSpeaking.value) {
                val delayTime = if (errorCode == 7 /* ERROR_NO_MATCH */ || errorCode == 6 /* ERROR_SPEECH_TIMEOUT */) {
                    400L
                } else {
                    1200L
                }
                scheduleRestart(delayTime)
            }
        }

        override fun onEndOfSpeech() {
            _isListening.value = false
        }
    }

    fun startContinuousListening() {
        _isContinuousListening.value = true
        isPaused = false
        if (!_isSpeaking.value) {
            armSession()
        }
    }

    fun stopContinuousListening() {
        _isContinuousListening.value = false
        isPaused = true
        mainHandler.removeCallbacksAndMessages(null)
        speechEngine.stopListening()
        _isListening.value = false
        _partialText.value = ""
    }

    fun pause() {
        isPaused = true
        mainHandler.removeCallbacksAndMessages(null)
        speechEngine.pauseListening()
        _isListening.value = false
    }

    fun resume() {
        if (_isContinuousListening.value) {
            isPaused = false
            if (!_isSpeaking.value) {
                armSession()
            }
        }
    }

    /**
     * Prevents self-triggering: when Nafsi speaks aloud, pause listening.
     * When speaking finishes, apply an acoustic echo guard delay before resuming.
     */
    fun onSpeechOutputStateChanged(speaking: Boolean) {
        _isSpeaking.value = speaking
        if (speaking) {
            isMutedDueToSpeech = true
            mainHandler.removeCallbacksAndMessages(null)
            speechEngine.pauseListening()
            _isListening.value = false
        } else {
            // Post-speech guard delay
            scope.launch {
                delay(echoGuardDelayMs)
                isMutedDueToSpeech = false
                if (_isContinuousListening.value && !isPaused) {
                    armSession()
                }
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (_isContinuousListening.value && !isPaused && !_isSpeaking.value) {
                armSession()
            }
        }, delayMs)
    }

    private fun armSession() {
        try {
            speechEngine.startListening(languageCode, speechListener)
        } catch (_: Exception) {}
    }

    fun release() {
        stopContinuousListening()
        speechEngine.release()
    }
}
