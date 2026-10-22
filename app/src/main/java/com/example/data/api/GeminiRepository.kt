package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.SearchSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class GeminiResponseResult(
    val text: String,
    val sources: List<SearchSource> = emptyList()
)

class GeminiRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResponse(
        history: List<ChatMessage>,
        userPrompt: String,
        imageUri: Uri? = null,
        enableWebSearch: Boolean = false,
        preferredLanguage: String = "AUTO" // "AUTO", "HI", "EN"
    ): Result<GeminiResponseResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException(
                    "Gemini API key is not configured. Please add your GEMINI_API_KEY in the Secrets panel in AI Studio or .env file."
                )
            )
        }

        val model = "gemini-3.5-flash"
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val rootJson = JSONObject()

            // System instructions
            val langInstruction = when (preferredLanguage) {
                "HI" -> " Respond primarily in polite, natural Hindi (हिंदी - Devanagari script)."
                "EN" -> " Respond in polished, engaging English."
                else -> " You are fluent in both English and Hindi (हिंदी). Match the user's language naturally."
            }

            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(
                JSONObject().put(
                    "text",
                    "You are AD AI, a modern, highly intelligent, friendly, and helpful mobile AI assistant created by AD.$langInstruction Use clear formatting, bullet points, and concise language suitable for mobile reading. Provide direct, helpful answers."
                )
            )
            systemInstruction.put("parts", sysParts)
            rootJson.put("systemInstruction", systemInstruction)

            // Contents array
            val contentsArray = JSONArray()

            // Add previous conversation turns (limit to last 10 messages for speed and context)
            val recentHistory = history.takeLast(10)
            for (msg in recentHistory) {
                val role = if (msg.role == "user") "user" else "model"
                val contentObj = JSONObject()
                contentObj.put("role", role)
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", msg.content))
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // Current user turn
            val currentUserTurn = JSONObject()
            currentUserTurn.put("role", "user")
            val currentParts = JSONArray()

            // If prompt is empty but image is present, set default question
            val promptText = if (userPrompt.isBlank() && imageUri != null) {
                "What is in this image? Explain in detail in Hindi and English."
            } else {
                userPrompt
            }

            currentParts.put(JSONObject().put("text", promptText))

            // Attached image if present
            if (imageUri != null) {
                val base64Image = encodeImageToBase64(imageUri)
                if (base64Image != null) {
                    val inlineData = JSONObject()
                    inlineData.put("mimeType", "image/jpeg")
                    inlineData.put("data", base64Image)
                    val imgPart = JSONObject()
                    imgPart.put("inlineData", inlineData)
                    currentParts.put(imgPart)
                }
            }

            currentUserTurn.put("parts", currentParts)
            contentsArray.put(currentUserTurn)
            rootJson.put("contents", contentsArray)

            // Optional Web Search Grounding Tool
            if (enableWebSearch) {
                val toolsArray = JSONArray()
                val googleSearchTool = JSONObject()
                googleSearchTool.put("googleSearch", JSONObject())
                toolsArray.put(googleSearchTool)
                rootJson.put("tools", toolsArray)
            }

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            rootJson.put("generationConfig", genConfig)

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e("GeminiRepository", "Error code ${response.code}: $responseBody")
                val errorMsg = try {
                    val errJson = JSONObject(responseBody).optJSONObject("error")
                    errJson?.optString("message") ?: "API Error (${response.code})"
                } catch (e: Exception) {
                    "Request failed with status code ${response.code}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No response received from AD AI."))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textBuilder.append(part.getString("text"))
                    }
                }
            }

            val replyText = textBuilder.toString().trim()

            // Parse grounding sources if present
            val sources = mutableListOf<SearchSource>()
            val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
            if (groundingMetadata != null) {
                val groundingChunks = groundingMetadata.optJSONArray("groundingChunks")
                if (groundingChunks != null) {
                    for (i in 0 until groundingChunks.length()) {
                        val chunk = groundingChunks.getJSONObject(i)
                        val web = chunk.optJSONObject("web")
                        if (web != null) {
                            val uri = web.optString("uri", "")
                            val title = web.optString("title", "Web Source")
                            if (uri.isNotBlank()) {
                                sources.add(SearchSource(title = title, url = uri))
                            }
                        }
                    }
                }
            }

            // Filter unique sources by url
            val uniqueSources = sources.distinctBy { it.url }

            Result.success(
                GeminiResponseResult(
                    text = if (replyText.isNotEmpty()) replyText else "I couldn't generate a text response. Please try asking again.",
                    sources = uniqueSources
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Exception calling Gemini", e)
            Result.failure(e)
        }
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Downscale to max 1024px to prevent large payloads while maintaining high vision quality
            val maxDimension = 1024
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW: Int
                val targetH: Int
                if (width >= height) {
                    targetW = maxDimension
                    targetH = (maxDimension / ratio).toInt()
                } else {
                    targetH = maxDimension
                    targetW = (maxDimension * ratio).toInt()
                }
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error encoding image", e)
            null
        }
    }
}
