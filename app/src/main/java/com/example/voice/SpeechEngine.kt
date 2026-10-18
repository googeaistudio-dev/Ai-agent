package com.example.voice

interface SpeechEngineListener {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onRmsChanged(rmsdB: Float)
    fun onPartialResults(partialText: String)
    fun onResults(text: String)
    fun onError(errorCode: Int, errorMessage: String)
    fun onEndOfSpeech()
}

interface SpeechEngine {
    fun isAvailable(): Boolean
    fun startListening(languageCode: String, listener: SpeechEngineListener)
    fun stopListening()
    fun pauseListening()
    fun resumeListening()
    fun release()
    val isCurrentlyListening: Boolean
}
