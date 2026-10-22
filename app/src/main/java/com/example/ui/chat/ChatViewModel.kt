package com.example.ui.chat

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.SearchSource
import com.example.util.SpeechLanguage
import com.example.util.SpeechManager
import com.example.util.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val geminiRepo = GeminiRepository(application)
    val ttsManager = TtsManager(application)
    val speechManager = SpeechManager(application)

    val sessions: StateFlow<List<ChatSession>> = chatDao.getAllSessions()
        .map { list ->
            list.map { ChatSession(it.id, it.title, it.createdAt, it.updatedAt) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val inputPrompt = MutableStateFlow("")
    val attachedImageUri = MutableStateFlow<Uri?>(null)
    val isWebSearchEnabled = MutableStateFlow(false)
    val preferredLanguage = MutableStateFlow("AUTO") // "AUTO", "HI", "EN"
    val speechLanguage = MutableStateFlow(SpeechLanguage.ENGLISH)

    val isListening: StateFlow<Boolean> = speechManager.isListening
    val speakingMessageId: StateFlow<String?> = ttsManager.speakingMessageId

    init {
        // Observe sessions to select latest if current is null
        viewModelScope.launch {
            sessions.collectLatest { sessionList ->
                if (_currentSessionId.value == null && sessionList.isNotEmpty()) {
                    selectSession(sessionList.first().id)
                } else if (sessionList.isEmpty()) {
                    createNewChat()
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            chatDao.getMessagesForSession(sessionId).collectLatest { entityList ->
                _messages.value = entityList.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        sessionId = entity.sessionId,
                        role = entity.role,
                        content = entity.content,
                        imageUri = entity.imageUri,
                        timestamp = entity.timestamp,
                        sources = parseSources(entity.sourcesJson),
                        isError = entity.isError
                    )
                }
            }
        }
    }

    fun createNewChat() {
        val newId = UUID.randomUUID().toString()
        val defaultTitle = "New Chat"
        viewModelScope.launch {
            chatDao.insertSession(
                ChatSessionEntity(
                    id = newId,
                    title = defaultTitle,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            selectSession(newId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatDao.deleteMessagesForSession(sessionId)
            chatDao.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            chatDao.deleteAllSessions()
            _currentSessionId.value = null
            _messages.value = emptyList()
            createNewChat()
        }
    }

    fun sendMessage(customPrompt: String? = null) {
        val promptToSend = (customPrompt ?: inputPrompt.value).trim()
        val imageToSend = attachedImageUri.value

        if (promptToSend.isBlank() && imageToSend == null) return

        var sessionId = _currentSessionId.value
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString()
            val newSession = ChatSessionEntity(
                id = sessionId,
                title = generateTitle(promptToSend),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            viewModelScope.launch {
                chatDao.insertSession(newSession)
                selectSession(sessionId)
                executeSend(sessionId, promptToSend, imageToSend)
            }
            return
        }

        executeSend(sessionId, promptToSend, imageToSend)
    }

    private fun executeSend(sessionId: String, prompt: String, imageUri: Uri?) {
        val userMsgId = UUID.randomUUID().toString()
        val userMessage = ChatMessageEntity(
            id = userMsgId,
            sessionId = sessionId,
            role = "user",
            content = prompt,
            imageUri = imageUri?.toString(),
            timestamp = System.currentTimeMillis(),
            sourcesJson = "",
            isError = false
        )

        // Clear input state immediately
        inputPrompt.value = ""
        attachedImageUri.value = null
        _errorMessage.value = null
        _isLoading.value = true

        viewModelScope.launch {
            chatDao.insertMessage(userMessage)

            // Update session title if it was "New Chat"
            val currentSession = chatDao.getSessionById(sessionId)
            if (currentSession != null && (currentSession.title == "New Chat" || currentSession.title.isBlank())) {
                chatDao.updateSession(
                    currentSession.copy(
                        title = generateTitle(prompt),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Call Gemini
            val currentHistory = _messages.value
            val result = geminiRepo.generateResponse(
                history = currentHistory,
                userPrompt = prompt,
                imageUri = imageUri,
                enableWebSearch = isWebSearchEnabled.value,
                preferredLanguage = preferredLanguage.value
            )

            _isLoading.value = false

            result.onSuccess { response ->
                val assistantMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "model",
                    content = response.text,
                    imageUri = null,
                    timestamp = System.currentTimeMillis(),
                    sourcesJson = serializeSources(response.sources),
                    isError = false
                )
                chatDao.insertMessage(assistantMsg)
            }.onFailure { ex ->
                val errorMsg = ex.message ?: "Failed to generate response."
                _errorMessage.value = errorMsg
                val assistantErrorMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "model",
                    content = "⚠️ $errorMsg",
                    imageUri = null,
                    timestamp = System.currentTimeMillis(),
                    sourcesJson = "",
                    isError = true
                )
                chatDao.insertMessage(assistantErrorMsg)
            }
        }
    }

    fun regenerateLastResponse() {
        val currentMsgs = _messages.value
        if (currentMsgs.isEmpty()) return

        val sessionId = _currentSessionId.value ?: return

        // Find last user message and last assistant message
        val lastAssistantMsg = currentMsgs.lastOrNull { it.role == "model" }
        val lastUserMsg = currentMsgs.lastOrNull { it.role == "user" } ?: return

        viewModelScope.launch {
            if (lastAssistantMsg != null) {
                chatDao.deleteMessage(lastAssistantMsg.id)
            }
            _isLoading.value = true
            _errorMessage.value = null

            val historyBefore = currentMsgs.filter { it.id != lastAssistantMsg?.id && it.id != lastUserMsg.id }
            val imageUri = lastUserMsg.imageUri?.let { Uri.parse(it) }

            val result = geminiRepo.generateResponse(
                history = historyBefore,
                userPrompt = lastUserMsg.content,
                imageUri = imageUri,
                enableWebSearch = isWebSearchEnabled.value,
                preferredLanguage = preferredLanguage.value
            )

            _isLoading.value = false

            result.onSuccess { response ->
                val newAssistantMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "model",
                    content = response.text,
                    imageUri = null,
                    timestamp = System.currentTimeMillis(),
                    sourcesJson = serializeSources(response.sources),
                    isError = false
                )
                chatDao.insertMessage(newAssistantMsg)
            }.onFailure { ex ->
                val errorMsg = ex.message ?: "Regeneration failed."
                _errorMessage.value = errorMsg
                val assistantErrorMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "model",
                    content = "⚠️ $errorMsg",
                    imageUri = null,
                    timestamp = System.currentTimeMillis(),
                    sourcesJson = "",
                    isError = true
                )
                chatDao.insertMessage(assistantErrorMsg)
            }
        }
    }

    fun speakMessage(messageId: String, text: String) {
        ttsManager.speak(messageId, text)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun startListening() {
        speechManager.startListening(speechLanguage.value) { resultText ->
            if (resultText.isNotBlank()) {
                val current = inputPrompt.value
                inputPrompt.value = if (current.isBlank()) resultText else "$current $resultText"
            }
        }
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AD AI Answer", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun generateTitle(prompt: String): String {
        val clean = prompt.trim().lines().firstOrNull() ?: "Chat"
        return if (clean.length > 32) clean.take(29) + "..." else clean
    }

    private fun serializeSources(sources: List<SearchSource>): String {
        if (sources.isEmpty()) return ""
        val array = JSONArray()
        for (src in sources) {
            val obj = JSONObject()
            obj.put("title", src.title)
            obj.put("url", src.url)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseSources(json: String): List<SearchSource> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<SearchSource>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SearchSource(
                        title = obj.optString("title", "Source"),
                        url = obj.optString("url", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        speechManager.stopListening()
    }
}
