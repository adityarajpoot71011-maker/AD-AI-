package com.example.data.model

data class SearchSource(
    val title: String,
    val url: String
)

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: String, // "user" or "model"
    val content: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<SearchSource> = emptyList(),
    val isError: Boolean = false
)

data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
