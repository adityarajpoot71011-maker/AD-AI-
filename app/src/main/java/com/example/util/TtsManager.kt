package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _speakingMessageId.value = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _speakingMessageId.value = null
                    }
                })
            } else {
                Log.e("TtsManager", "TTS initialization failed with status: $status")
            }
        }
    }

    fun speak(messageId: String, text: String) {
        if (!isInitialized || tts == null) return

        // If currently speaking this message, stop it
        if (_speakingMessageId.value == messageId && _isSpeaking.value) {
            stop()
            return
        }

        stop()
        _speakingMessageId.value = messageId

        val cleanedText = cleanMarkdownForSpeech(text)

        // Detect if text contains Hindi (Devanagari) characters
        val containsDevanagari = cleanedText.any { it in '\u0900'..'\u097F' }
        val targetLocale = if (containsDevanagari) {
            Locale.forLanguageTag("hi-IN")
        } else {
            Locale.US
        }

        try {
            val result = tts?.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default
                tts?.setLanguage(Locale.getDefault())
            }
        } catch (e: Exception) {
            Log.e("TtsManager", "Error setting language", e)
        }

        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, messageId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun cleanMarkdownForSpeech(input: String): String {
        return input
            .replace(Regex("```[a-zA-Z]*\\n[\\s\\S]*?\\n```"), "code block omitted")
            .replace(Regex("`[^`]+`"), "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")
            .replace(Regex("^[*-]\\s+", RegexOption.MULTILINE), "")
            .trim()
    }
}
