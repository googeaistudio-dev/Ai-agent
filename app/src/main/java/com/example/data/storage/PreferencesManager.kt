package com.example.data.storage

import android.content.Context
import com.example.BuildConfig
import com.example.domain.model.AIProviderConfig
import com.example.domain.model.AIProviderType
import com.example.domain.model.VoiceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("nafsi_preferences", Context.MODE_PRIVATE)
    private val secureStore = SecureKeyStore(context)

    private val _providerConfig = MutableStateFlow(loadProviderConfig())
    val providerConfig: StateFlow<AIProviderConfig> = _providerConfig.asStateFlow()

    private val _voiceSettings = MutableStateFlow(loadVoiceSettings())
    val voiceSettings: StateFlow<VoiceSettings> = _voiceSettings.asStateFlow()

    private val _floatingMascotEnabled = MutableStateFlow(prefs.getBoolean("mascot_enabled", false))
    val floatingMascotEnabled: StateFlow<Boolean> = _floatingMascotEnabled.asStateFlow()

    private val _reduceMotion = MutableStateFlow(prefs.getBoolean("reduce_motion", false))
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    private fun loadProviderConfig(): AIProviderConfig {
        val typeStr = prefs.getString("provider_type", AIProviderType.GEMINI_DIRECT.name) ?: AIProviderType.GEMINI_DIRECT.name
        val type = try { AIProviderType.valueOf(typeStr) } catch (_: Exception) { AIProviderType.GEMINI_DIRECT }
        val storedKey = secureStore.getSecret("api_key")
        // Default to BuildConfig.GEMINI_API_KEY if user hasn't explicitly entered a custom key
        val key = if (storedKey.isNotBlank()) storedKey else try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (_: Throwable) { "" }

        return AIProviderConfig(
            providerType = type,
            apiKey = key,
            baseUrl = prefs.getString("base_url", "https://generativelanguage.googleapis.com/") ?: "https://generativelanguage.googleapis.com/",
            modelId = prefs.getString("model_id", "gemini-3.8-flash") ?: "gemini-3.8-flash",
            visionModelId = prefs.getString("vision_model_id", "gemini-3.8-flash") ?: "gemini-3.8-flash",
            temperature = prefs.getFloat("temperature", 0.3f),
            maxTokens = prefs.getInt("max_tokens", 2048),
            timeoutSeconds = prefs.getInt("timeout_seconds", 60),
            useGoogleSearch = prefs.getBoolean("use_google_search", true),
            systemInstruction = prefs.getString("system_instruction", "") ?: ""
        )
    }

    fun saveProviderConfig(config: AIProviderConfig) {
        secureStore.storeSecret("api_key", config.apiKey)
        prefs.edit()
            .putString("provider_type", config.providerType.name)
            .putString("base_url", config.baseUrl)
            .putString("model_id", config.modelId)
            .putString("vision_model_id", config.visionModelId)
            .putFloat("temperature", config.temperature)
            .putInt("max_tokens", config.maxTokens)
            .putInt("timeout_seconds", config.timeoutSeconds)
            .putBoolean("use_google_search", config.useGoogleSearch)
            .putString("system_instruction", config.systemInstruction)
            .apply()
        _providerConfig.value = config
    }

    private fun loadVoiceSettings(): VoiceSettings {
        return VoiceSettings(
            continuousListening = prefs.getBoolean("voice_continuous", true),
            language = prefs.getString("voice_language", "bn-BD") ?: "bn-BD",
            speechRate = prefs.getFloat("voice_rate", 1.0f),
            speechPitch = prefs.getFloat("voice_pitch", 1.0f),
            muteSpokenResponses = prefs.getBoolean("voice_mute", false),
            echoPreventionDelayMs = prefs.getLong("voice_echo_delay", 700L)
        )
    }

    fun saveVoiceSettings(settings: VoiceSettings) {
        prefs.edit()
            .putBoolean("voice_continuous", settings.continuousListening)
            .putString("voice_language", settings.language)
            .putFloat("voice_rate", settings.speechRate)
            .putFloat("voice_pitch", settings.speechPitch)
            .putBoolean("voice_mute", settings.muteSpokenResponses)
            .putLong("voice_echo_delay", settings.echoPreventionDelayMs)
            .apply()
        _voiceSettings.value = settings
    }

    fun setFloatingMascotEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("mascot_enabled", enabled).apply()
        _floatingMascotEnabled.value = enabled
    }

    fun setReduceMotion(reduce: Boolean) {
        prefs.edit().putBoolean("reduce_motion", reduce).apply()
        _reduceMotion.value = reduce
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
        secureStore.clearAllSecrets()
        _providerConfig.value = loadProviderConfig()
        _voiceSettings.value = loadVoiceSettings()
    }
}
