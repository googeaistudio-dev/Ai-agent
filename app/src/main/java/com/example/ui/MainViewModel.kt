package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NafsiRepository
import com.example.domain.model.AIProviderConfig
import com.example.domain.model.AgentTask
import com.example.domain.model.ChatMessage
import com.example.domain.model.DiscoveredModel
import com.example.domain.model.MascotState
import com.example.domain.model.VoiceSettings
import com.example.overlay.MascotOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = NafsiRepository(application)

    val mascotState: StateFlow<MascotState> = repository.mascotState
    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
    val activeTask: StateFlow<AgentTask?> = repository.activeTask
    val isListening: StateFlow<Boolean> = repository.listeningManager.isListening
    val isContinuousListening: StateFlow<Boolean> = repository.listeningManager.isContinuousListening
    val rmsLevel: StateFlow<Float> = repository.listeningManager.rmsLevel
    val partialText: StateFlow<String> = repository.listeningManager.partialText
    val providerConfig: StateFlow<AIProviderConfig> = repository.preferences.providerConfig
    val voiceSettings: StateFlow<VoiceSettings> = repository.preferences.voiceSettings
    val floatingMascotEnabled: StateFlow<Boolean> = repository.preferences.floatingMascotEnabled
    val reduceMotion: StateFlow<Boolean> = repository.preferences.reduceMotion
    val diagnostics: StateFlow<List<String>> = repository.diagnostics

    private val _connectionTestStatus = MutableStateFlow<String?>(null)
    val connectionTestStatus: StateFlow<String?> = _connectionTestStatus.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _discoveredModels = MutableStateFlow<List<DiscoveredModel>>(repository.modelDiscoveryService.getGeminiCatalog())
    val discoveredModels: StateFlow<List<DiscoveredModel>> = _discoveredModels.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    init {
        // Auto-start continuous listening if enabled in settings
        if (voiceSettings.value.continuousListening) {
            repository.listeningManager.startContinuousListening()
        }
    }

    fun onUserCommand(text: String) {
        repository.handleUserCommand(text)
    }

    fun toggleContinuousListening() {
        if (isContinuousListening.value) {
            repository.listeningManager.stopContinuousListening()
            repository.setMascotState(MascotState.PAUSED)
        } else {
            repository.listeningManager.startContinuousListening()
            repository.setMascotState(MascotState.LISTENING)
        }
    }

    fun setMascotState(state: MascotState) {
        repository.setMascotState(state)
    }

    fun cancelActiveTask() {
        repository.cancelActiveTask()
    }

    fun clearChat() {
        repository.clearChatHistory()
    }

    fun saveProviderConfig(config: AIProviderConfig) {
        repository.preferences.saveProviderConfig(config)
        repository.logDiagnostic("Saved AI configuration for ${config.providerType} (${config.modelId})")
    }

    fun saveVoiceSettings(settings: VoiceSettings) {
        repository.preferences.saveVoiceSettings(settings)
        repository.speechSynthesizer.speechRate = settings.speechRate
        repository.speechSynthesizer.speechPitch = settings.speechPitch
        repository.speechSynthesizer.isMuted = settings.muteSpokenResponses
        repository.listeningManager.languageCode = settings.language
        repository.listeningManager.echoGuardDelayMs = settings.echoPreventionDelayMs

        if (settings.continuousListening && !isContinuousListening.value) {
            repository.listeningManager.startContinuousListening()
        } else if (!settings.continuousListening && isContinuousListening.value) {
            repository.listeningManager.stopContinuousListening()
        }
        repository.logDiagnostic("Updated voice settings: ${settings.language}, continuous=${settings.continuousListening}")
    }

    fun setFloatingMascotEnabled(enabled: Boolean) {
        repository.preferences.setFloatingMascotEnabled(enabled)
        if (enabled) {
            MascotOverlayService.start(getApplication())
        } else {
            MascotOverlayService.stop(getApplication())
        }
    }

    fun setReduceMotion(reduce: Boolean) {
        repository.preferences.setReduceMotion(reduce)
    }

    fun testConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestStatus.value = "Testing connection..."
            val result = repository.modelDiscoveryService.testConnection(providerConfig.value)
            _isTestingConnection.value = false
            if (result.isSuccess) {
                _connectionTestStatus.value = "✓ " + result.getOrNull()
            } else {
                _connectionTestStatus.value = "✕ " + result.exceptionOrNull()?.message
            }
        }
    }

    fun discoverModels() {
        viewModelScope.launch {
            _isDiscovering.value = true
            val result = repository.modelDiscoveryService.discoverModels(providerConfig.value)
            _isDiscovering.value = false
            if (result.isSuccess) {
                val list = result.getOrNull().orEmpty()
                _discoveredModels.value = list
                repository.logDiagnostic("Discovered ${list.size} models from provider.")
            } else {
                repository.logDiagnostic("Model discovery error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun runCalculatorDemo() {
        onUserCommand("ক্যালকুলেটর খোলো এবং দুই যোগ দুই করো")
    }

    fun isAccessibilityEnabled(): Boolean = repository.isAccessibilityEnabled()

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }
}
