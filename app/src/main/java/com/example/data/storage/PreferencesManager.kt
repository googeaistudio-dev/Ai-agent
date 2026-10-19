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

    fun getKeyForProvider(type: AIProviderType): String {
        val specificKey = if (type == AIProviderType.GEMINI_DIRECT) {
            prefs.getString("saved_gemini_api_key", null)
        } else {
            prefs.getString("saved_openai_api_key", null)
        }
        if (!specificKey.isNullOrBlank()) return specificKey

        val generalKey = prefs.getString("saved_api_key", null)
        if (!generalKey.isNullOrBlank()) return generalKey

        val secureKey = try { secureStore.getSecret("api_key") } catch (_: Throwable) { "" }
        if (secureKey.isNotBlank()) return secureKey

        if (type == AIProviderType.GEMINI_DIRECT) {
            try {
                val buildKey = BuildConfig.GEMINI_API_KEY
                if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey
            } catch (_: Throwable) {}
        }
        return ""
    }

    private fun loadProviderConfig(): AIProviderConfig {
        val typeStr = prefs.getString("provider_type", AIProviderType.GEMINI_DIRECT.name) ?: AIProviderType.GEMINI_DIRECT.name
        val type = try { AIProviderType.valueOf(typeStr) } catch (_: Exception) { AIProviderType.GEMINI_DIRECT }

        val key = getKeyForProvider(type)

        val rawModelId = prefs.getString("model_id", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        val normalizedModelId = if (rawModelId.contains("gemini-3.") || rawModelId.contains("gemini-4") || rawModelId.isBlank()) {
            if (type == AIProviderType.GEMINI_DIRECT) "gemini-2.5-flash" else "google/gemini-2.0-flash-001"
        } else rawModelId

        val rawVisionModel = prefs.getString("vision_model_id", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        val normalizedVisionModel = if (rawVisionModel.contains("gemini-3.") || rawVisionModel.contains("gemini-4") || rawVisionModel.isBlank()) {
            if (type == AIProviderType.GEMINI_DIRECT) "gemini-2.5-flash" else "google/gemini-2.0-flash-001"
        } else rawVisionModel

        val defaultBaseUrl = if (type == AIProviderType.GEMINI_DIRECT) {
            "https://generativelanguage.googleapis.com/"
        } else {
            "https://openrouter.ai/api/v1"
        }
        val savedBaseUrl = prefs.getString("base_url", defaultBaseUrl) ?: defaultBaseUrl

        return AIProviderConfig(
            providerType = type,
            apiKey = key,
            baseUrl = savedBaseUrl,
            modelId = normalizedModelId,
            visionModelId = normalizedVisionModel,
            temperature = prefs.getFloat("temperature", 0.3f),
            maxTokens = prefs.getInt("max_tokens", 2048),
            timeoutSeconds = prefs.getInt("timeout_seconds", 60),
            useGoogleSearch = prefs.getBoolean("use_google_search", true),
            systemInstruction = prefs.getString("system_instruction", "") ?: ""
        )
    }

    fun saveProviderConfig(config: AIProviderConfig) {
        val cleanKey = config.apiKey.trim()
        val editor = prefs.edit()

        // 1. Save general key and secure store
        editor.putString("saved_api_key", cleanKey)
        secureStore.storeSecret("api_key", cleanKey)

        // 2. Save provider-specific key so switching providers preserves both keys
        if (config.providerType == AIProviderType.GEMINI_DIRECT) {
            editor.putString("saved_gemini_api_key", cleanKey)
        } else {
            editor.putString("saved_openai_api_key", cleanKey)
        }

        // 3. Normalize model ID to prevent broken gemini-3.8-flash
        val cleanModel = if (config.modelId.contains("gemini-3.") || config.modelId.contains("gemini-4")) {
            "gemini-2.5-flash"
        } else {
            config.modelId.trim()
        }

        editor.putString("provider_type", config.providerType.name)
            .putString("base_url", config.baseUrl.trim())
            .putString("model_id", cleanModel)
            .putString("vision_model_id", config.visionModelId.trim().ifBlank { cleanModel })
            .putFloat("temperature", config.temperature)
            .putInt("max_tokens", config.maxTokens)
            .putInt("timeout_seconds", config.timeoutSeconds)
            .putBoolean("use_google_search", config.useGoogleSearch)
            .putString("system_instruction", config.systemInstruction)
            .commit() // Commit synchronously to ensure immediate disk persistence

        _providerConfig.value = config.copy(
            apiKey = cleanKey,
            baseUrl = config.baseUrl.trim(),
            modelId = cleanModel
        )
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
            .commit()
        _voiceSettings.value = settings
    }

    fun setFloatingMascotEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("mascot_enabled", enabled).commit()
        _floatingMascotEnabled.value = enabled
    }

    fun setReduceMotion(reduce: Boolean) {
        prefs.edit().putBoolean("reduce_motion", reduce).commit()
        _reduceMotion.value = reduce
    }

    fun clearAllData() {
        prefs.edit().clear().commit()
        secureStore.clearAllSecrets()
        _providerConfig.value = loadProviderConfig()
        _voiceSettings.value = loadVoiceSettings()
    }
}
