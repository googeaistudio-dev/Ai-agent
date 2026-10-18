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
                id = "gemini-2.5-flash",
                displayName = "Gemini 2.5 Flash (Recommended)",
                supportsVision = true,
                description = "Fast, state-of-the-art multimodal model with Google Search grounding support."
            ),
            DiscoveredModel(
                id = "gemini-2.0-flash",
                displayName = "Gemini 2.0 Flash (Next-Gen)",
                supportsVision = true,
                description = "Ultra-low latency multimodal model optimized for real-time mobile agents."
            ),
            DiscoveredModel(
                id = "gemini-1.5-flash",
                displayName = "Gemini 1.5 Flash (Lightweight)",
                supportsVision = true,
                description = "High-speed, cost-effective multimodal model for instant task automation."
            ),
            DiscoveredModel(
                id = "gemini-1.5-pro",
                displayName = "Gemini 1.5 Pro (Deep Reasoning)",
                supportsVision = true,
                description = "Complex reasoning model for intricate multi-step UI workflows."
            ),
            DiscoveredModel(
                id = "gemini-2.5-pro",
                displayName = "Gemini 2.5 Pro (Advanced Intelligence)",
                supportsVision = true,
                description = "Highest capability model for complex problem solving and code reasoning."
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
