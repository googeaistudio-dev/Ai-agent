package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.domain.model.AIProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(config: AIProviderConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = config.apiKey.trim()
            if (key.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("API Key is empty. Please set your Gemini API key."))
            }

            // Quick lightweight probe to test key & model existence
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${config.modelId}?key=$key"
            val request = Request.Builder().url(url).get().build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            when (code) {
                200 -> Result.success("Connected successfully to Gemini (${config.modelId})")
                400 -> Result.failure(Exception("Bad Request: Check model name or arguments. Response: $body"))
                401, 403 -> Result.failure(Exception("Authentication Failed (Code $code): Invalid API key or permission denied."))
                404 -> Result.failure(Exception("Model Not Found (404): '${config.modelId}' was not recognized."))
                429 -> Result.failure(Exception("Rate limit reached (429): Quota exhausted or too many requests."))
                else -> Result.failure(Exception("HTTP Error $code: $body"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun generateContent(
        prompt: String,
        config: AIProviderConfig,
        screenshotBitmap: Bitmap? = null,
        systemInstruction: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = config.apiKey.trim()
            if (key.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
            }

            val model = if (screenshotBitmap != null && config.visionModelId.isNotBlank()) {
                config.visionModelId
            } else {
                config.modelId.ifBlank { "gemini-3.8-flash" }
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

            val rootJson = JSONObject()

            // System instruction
            val sysInstruction = systemInstruction ?: config.systemInstruction
            if (!sysInstruction.isNullOrBlank()) {
                val sysContent = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", sysInstruction))
                    }
                    put("parts", parts)
                }
                rootJson.put("systemInstruction", sysContent)
            }

            // Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Text part
            partsArray.put(JSONObject().put("text", prompt))

            // Multimodal image part if present
            if (screenshotBitmap != null) {
                val base64 = bitmapToBase64(screenshotBitmap)
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64)
                }
                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", config.temperature.toDouble())
                put("maxOutputTokens", config.maxTokens)
            }
            rootJson.put("generationConfig", genConfig)

            // Google Search Grounding tool if requested
            if (config.useGoogleSearch) {
                val toolsArray = JSONArray()
                val googleSearchTool = JSONObject().put("googleSearch", JSONObject())
                toolsArray.put(googleSearchTool)
                rootJson.put("tools", toolsArray)
            }

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini error ($code): $body"))
            }

            val responseJson = JSONObject(body)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                val promptFeedback = responseJson.optJSONObject("promptFeedback")
                return@withContext Result.failure(Exception("No response generated: ${promptFeedback?.toString() ?: "Empty candidate"}"))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val stringBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        stringBuilder.append(part.getString("text"))
                    }
                }
            }

            val resultText = stringBuilder.toString()
            if (resultText.isBlank()) {
                Result.failure(Exception("Empty text in Gemini response candidate"))
            } else {
                Result.success(resultText)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        // Resize if too large to conserve bandwidth and latency
        val scaled = if (bitmap.width > 1280 || bitmap.height > 1280) {
            val ratio = minOf(1280f / bitmap.width, 1280f / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
