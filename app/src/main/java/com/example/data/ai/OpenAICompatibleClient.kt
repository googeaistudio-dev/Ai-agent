package com.example.data.ai

import com.example.domain.model.AIProviderConfig
import com.example.domain.model.DiscoveredModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAICompatibleClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun discoverModels(config: AIProviderConfig): Result<List<DiscoveredModel>> = withContext(Dispatchers.IO) {
        try {
            val key = config.apiKey.trim()
            val baseUrl = sanitizeBaseUrl(config.baseUrl)
            val url = "$baseUrl/models"

            val requestBuilder = Request.Builder().url(url).get()
            if (key.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $key")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Model discovery failed (HTTP $code): $body"))
            }

            val json = JSONObject(body)
            val dataArray = json.optJSONArray("data") ?: JSONArray()
            val models = mutableListOf<DiscoveredModel>()

            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id", "")
                if (id.isNotBlank()) {
                    val isVision = id.contains("vision", ignoreCase = true) || id.contains("flash", ignoreCase = true) || id.contains("4o", ignoreCase = true)
                    models.add(DiscoveredModel(
                        id = id,
                        displayName = id,
                        supportsVision = isVision,
                        description = item.optString("owned_by", "OpenAI-compatible")
                    ))
                }
            }
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(config: AIProviderConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = config.apiKey.trim()
            val baseUrl = sanitizeBaseUrl(config.baseUrl)
            val url = "$baseUrl/chat/completions"

            val json = JSONObject().apply {
                put("model", config.modelId.ifBlank { "gpt-4o-mini" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
                put("max_tokens", 5)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(jsonMediaType))

            if (key.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $key")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            when (code) {
                200 -> Result.success("Connected successfully to ${config.modelId}")
                401 -> Result.failure(Exception("Unauthorized (401): Invalid API Key."))
                403 -> Result.failure(Exception("Forbidden (403): Access denied or quota exceeded."))
                404 -> Result.failure(Exception("Not Found (404): Check Base URL or Model ID '${config.modelId}'."))
                429 -> Result.failure(Exception("Rate Limited (429): Quota exhausted or too many requests."))
                else -> Result.failure(Exception("HTTP Error $code: $body"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun generateContent(
        prompt: String,
        config: AIProviderConfig,
        systemInstruction: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = config.apiKey.trim()
            val baseUrl = sanitizeBaseUrl(config.baseUrl)
            val url = "$baseUrl/chat/completions"

            val json = JSONObject().apply {
                put("model", config.modelId)
                val messagesArray = JSONArray()

                val sys = systemInstruction ?: config.systemInstruction
                if (sys.isNotBlank()) {
                    messagesArray.put(JSONObject().apply {
                        put("role", "system")
                        put("content", sys)
                    })
                }

                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })

                put("messages", messagesArray)
                put("temperature", config.temperature.toDouble())
                put("max_tokens", config.maxTokens)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(jsonMediaType))

            if (key.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $key")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error $code: $body"))
            }

            val resJson = JSONObject(body)
            val choices = resJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(Exception("No choices returned from model"))
            }

            val first = choices.getJSONObject(0)
            val message = first.optJSONObject("message")
            val content = message?.optString("content", "") ?: ""

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitizeBaseUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }
        if (!trimmed.endsWith("/v1")) {
            trimmed = "$trimmed/v1"
        }
        return trimmed
    }
}
