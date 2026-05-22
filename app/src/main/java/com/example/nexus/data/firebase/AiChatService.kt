package com.example.nexus.data.firebase

import com.example.nexus.BuildConfig
import com.example.nexus.data.model.Message
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── MiMo API Data Classes (OpenAI-compatible) ──

private data class MiMoRequest(
    val model: String = "mimo-v2-flash",
    val messages: List<MiMoMessage>,
    val temperature: Double = 0.7
)

private data class MiMoMessage(
    val role: String,
    val content: String
)

private data class MiMoResponse(
    val choices: List<MiMoChoice>
)

private data class MiMoChoice(
    val message: MiMoMessage
)

// ── Service ──

@Singleton
class AiChatService @Inject constructor() {

    private val apiKey = BuildConfig.MIMO_API_KEY
    private val endpoint = "https://api.xiaomimimo.com/v1/chat/completions"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun makeApiCall(systemPrompt: String, userContent: String): String {
        return withContext(Dispatchers.IO) {
            val request = MiMoRequest(
                messages = listOf(
                    MiMoMessage(role = "system", content = systemPrompt),
                    MiMoMessage(role = "user", content = userContent)
                )
            )

            val jsonBody = gson.toJson(request)
            val body = jsonBody.toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: $responseBody")
            }

            val mimoResponse = gson.fromJson(responseBody, MiMoResponse::class.java)
            mimoResponse.choices.firstOrNull()?.message?.content
                ?: throw Exception("No content in response")
        }
    }

    suspend fun summarizeMessages(messages: List<Message>, currentUserId: String?): String {
        return try {
            val textMessages = messages
                .filter { it.type == "text" && it.text.isNotBlank() }
                .takeLast(20)
                .reversed()

            if (textMessages.isEmpty()) return "Không có tin nhắn để tóm tắt."

            val conversation = textMessages.joinToString("\n") { msg ->
                val sender = if (msg.senderId == currentUserId) "Me" else msg.senderName.ifEmpty { "Other" }
                "$sender: ${msg.text}"
            }

            val systemPrompt = "Bạn là trợ lý AI tóm tắt cuộc trò chuyện. " +
                "Tóm tắt nội dung trong 2-3 câu ngắn gọn, tập trung vào chủ đề chính. " +
                "Trả lời bằng ngôn ngữ của cuộc trò chuyện."

            makeApiCall(systemPrompt, conversation)
        } catch (e: Exception) {
            "Lỗi kết nối AI: ${e.message}"
        }
    }

    suspend fun getSmartReplies(messages: List<Message>, currentUserId: String?): List<String> {
        return try {
            val recentMessages = messages
                .filter { it.type == "text" && it.text.isNotBlank() }
                .take(10)
                .reversed()

            if (recentMessages.isEmpty()) return emptyList()

            val lastMessage = recentMessages.last()
            if (lastMessage.senderId == currentUserId) return emptyList()

            val conversation = recentMessages.joinToString("\n") { msg ->
                val sender = if (msg.senderId == currentUserId) "Me" else msg.senderName.ifEmpty { "Other" }
                "$sender: ${msg.text}"
            }

            val systemPrompt = "Bạn là trợ lý AI gợi ý trả lời tin nhắn. " +
                "Dựa vào cuộc trò chuyện, gợi ý 3 câu trả lời ngắn gọn tự nhiên. " +
                "Bắt buộc trả về đúng 3 gợi ý, phân tách nhau bằng ký tự | (dọc). " +
                "Mỗi gợi ý dưới 10 từ. Không đánh số, không ngoặc kép. " +
                "Trả lời bằng ngôn ngữ của cuộc trò chuyện."

            val result = makeApiCall(systemPrompt, conversation)

            result.split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(3)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
