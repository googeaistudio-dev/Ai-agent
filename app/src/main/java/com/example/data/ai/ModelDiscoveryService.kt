package com.example.data.ai

import com.example.domain.model.AIProviderConfig
import com.example.domain.model.AIProviderType
import com.example.domain.model.DiscoveredModel

class ModelDiscoveryService {

    private val geminiClient = GeminiApiClient()
    private val openAIClient = OpenAICompatibleClient()

    fun getGeminiCatalog(): List<DiscoveredModel> {
        return listOf(
            DiscoveredModel(
                id = "gemini-3.8-flash",
                displayName = "Gemini 3.8 Flash (Recommended)",
                supportsVision = true,
                description = "Fast, multimodal model with Google Search grounding support for general tasks."
            ),
            DiscoveredModel(
                id = "gemini-3.8-live",
                displayName = "Gemini 3.8 Live (Voice Conversation)",
                supportsVision = true,
                description = "Ultra-low latency conversational live model for real-time natural voice interaction."
            ),
            DiscoveredModel(
                id = "gemini-4-argon",
                displayName = "Gemini 4 Argon (Advanced Planner)",
                supportsVision = true,
                description = "Deep reasoning model for complex multi-step UI action planning and verification."
            ),
            DiscoveredModel(
                id = "gemini-3.1-flash-lite",
                displayName = "Gemini 3.1 Flash-Lite (Speed)",
                supportsVision = true,
                description = "High-speed, lightweight model for instant screen perception and quick questions."
            ),
            DiscoveredModel(
                id = "gemini-3.5-flash",
                displayName = "Gemini 3.5 Flash",
                supportsVision = true,
                description = "Balanced versatile model for general question answering and screen understanding."
            )
        )
    }

    suspend fun discoverModels(config: AIProviderConfig): Result<List<DiscoveredModel>> {
        return when (config.providerType) {
            AIProviderType.GEMINI_DIRECT -> {
                Result.success(getGeminiCatalog())
            }
            AIProviderType.OPENAI_COMPATIBLE -> {
                openAIClient.discoverModels(config)
            }
        }
    }

    suspend fun testConnection(config: AIProviderConfig): Result<String> {
        return when (config.providerType) {
            AIProviderType.GEMINI_DIRECT -> geminiClient.testConnection(config)
            AIProviderType.OPENAI_COMPATIBLE -> openAIClient.testConnection(config)
        }
    }
}
