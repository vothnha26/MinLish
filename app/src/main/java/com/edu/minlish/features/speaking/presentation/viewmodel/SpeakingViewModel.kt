package com.edu.minlish.features.speaking.presentation.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.core.ai.AIModule
import com.edu.minlish.core.util.AudioRecorder
import com.edu.minlish.features.speaking.domain.model.MessageSender
import com.edu.minlish.features.speaking.domain.model.SpeakingChatMessage
import com.edu.minlish.features.speaking.domain.model.SpeakingResult
import com.edu.minlish.features.speaking.domain.model.SpeakingTopic
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

sealed class SpeakingUiState {
    object TopicSelection : SpeakingUiState()
    object SessionActive : SpeakingUiState()
    data class ProcessingTurn(val message: String) : SpeakingUiState()
    data class Evaluating(val message: String) : SpeakingUiState()
    data class Report(val result: SpeakingResult) : SpeakingUiState()
    data class Error(val message: String) : SpeakingUiState()
}

data class AITurnResponse(
    val transcript: String,
    val reply: String,
    val nextQuestion: String,
    val turnFeedback: String
)

class SpeakingViewModel(context: Context) : ViewModel(), TextToSpeech.OnInitListener {

    private val audioRecorder = AudioRecorder(context)
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var recordStartTime = 0L

    val topics = listOf(
        SpeakingTopic("1", "Hobbies", "Talk about your favorite hobby. What is it, when did you start, and why do you enjoy it?"),
        SpeakingTopic("2", "Travel", "Describe a memorable trip you took. Where did you go, who did you go with, and what did you do?"),
        SpeakingTopic("3", "Future Goals", "What are your main goals for the next 5 years? How do you plan to achieve them?")
    )

    private val _uiState = MutableStateFlow<SpeakingUiState>(SpeakingUiState.TopicSelection)
    val uiState: StateFlow<SpeakingUiState> = _uiState.asStateFlow()

    private val _selectedTopic = MutableStateFlow(topics[0])
    val selectedTopic: StateFlow<SpeakingTopic> = _selectedTopic.asStateFlow()

    private val _customTopicText = MutableStateFlow("")
    val customTopicText: StateFlow<String> = _customTopicText.asStateFlow()

    private val _useCustomTopic = MutableStateFlow(false)
    val useCustomTopic: StateFlow<Boolean> = _useCustomTopic.asStateFlow()

    private val _selectedMode = MutableStateFlow("Daily Conversation")
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<SpeakingChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<SpeakingChatMessage>> = _chatMessages.asStateFlow()

    private val _currentTurn = MutableStateFlow(0)
    val currentTurn: StateFlow<Int> = _currentTurn.asStateFlow()

    private val _maxTurns = MutableStateFlow(3)
    val maxTurns: StateFlow<Int> = _maxTurns.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsInitialized = true
            }
        }
    }

    fun speak(text: String) {
        if (isTtsInitialized) {
            // Stop any ongoing speech first
            tts?.stop()
            // Clean text of emojis or double newlines for smoother TTS
            val cleanText = text.replace(Regex("[\\p{So}\\p{Cn}]"), "")
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "SpeakingPlayId")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun selectTopic(topic: SpeakingTopic) {
        _useCustomTopic.update { false }
        _selectedTopic.update { topic }
    }

    fun updateCustomTopicText(text: String) {
        _customTopicText.update { text }
    }

    fun updateUseCustomTopic(value: Boolean) {
        _useCustomTopic.update { value }
    }

    fun updateSelectedMode(mode: String) {
        _selectedMode.update { mode }
    }

    fun updateMaxTurns(turns: Int) {
        _maxTurns.update { turns }
    }

    fun useTopicSelection() {
        stopSpeaking()
        _uiState.update { SpeakingUiState.TopicSelection }
    }

    fun startSession() {
        val topicPrompt = if (_useCustomTopic.value) {
            if (_customTopicText.value.isBlank()) {
                _uiState.update { SpeakingUiState.Error("Vui lòng nhập chủ đề bạn muốn luyện nói!") }
                return
            }
            _customTopicText.value
        } else {
            _selectedTopic.value.prompt
        }

        _uiState.update { SpeakingUiState.ProcessingTurn("AI đang khởi động buổi trò chuyện...") }
        _chatMessages.update { emptyList() }
        _currentTurn.update { 0 }

        viewModelScope.launch {
            try {
                val firstQuestionResult = AIModule.geminiService.generateFirstQuestion(topicPrompt, _selectedMode.value)
                firstQuestionResult.onSuccess { jsonStr ->
                    try {
                        val cleanJson = if (jsonStr.contains("```json")) {
                            jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
                        } else if (jsonStr.contains("```")) {
                            jsonStr.substringAfter("```").substringBeforeLast("```").trim()
                        } else {
                            jsonStr.trim()
                        }

                        val jsonObject = Gson().fromJson(cleanJson, Map::class.java)
                        val question = (jsonObject["question"] ?: jsonObject["response"] ?: jsonObject["text"] ?: jsonStr).toString()

                        _chatMessages.update { current ->
                            current + SpeakingChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = MessageSender.AI,
                                text = question
                            )
                        }
                        _uiState.update { SpeakingUiState.SessionActive }
                        speak(question)
                    } catch (e: Exception) {
                        val fallback = jsonStr.trim()
                        _chatMessages.update { current ->
                            current + SpeakingChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = MessageSender.AI,
                                text = fallback
                            )
                        }
                        _uiState.update { SpeakingUiState.SessionActive }
                        speak(fallback)
                    }
                }
                firstQuestionResult.onFailure { e ->
                    _uiState.update { SpeakingUiState.Error("Không thể tạo câu hỏi đầu tiên: ${e.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { SpeakingUiState.Error("Đã xảy ra lỗi: ${e.message}") }
            }
        }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecordingAndAnalyze()
        } else {
            stopSpeaking()
            val started = audioRecorder.startRecording()
            if (started) {
                _isRecording.update { true }
                recordStartTime = System.currentTimeMillis()
                _uiState.update { SpeakingUiState.SessionActive }
            } else {
                _uiState.update { SpeakingUiState.Error("Không thể bắt đầu ghi âm. Hãy kiểm tra quyền Microphone.") }
            }
        }
    }

    private fun stopRecordingAndAnalyze() {
        _isRecording.update { false }
        val duration = System.currentTimeMillis() - recordStartTime
        val file = audioRecorder.stopRecording()
        
        if (file == null || !file.exists()) {
            _uiState.update { SpeakingUiState.Error("Lỗi ghi âm: File không tồn tại") }
            return
        }

        if (duration < 1000) {
            _uiState.update { SpeakingUiState.Error("Thời gian nói quá ngắn (tối thiểu 1 giây). Vui lòng thử lại!") }
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                if (_uiState.value is SpeakingUiState.Error) {
                    _uiState.update { SpeakingUiState.SessionActive }
                }
            }
            try { file.delete() } catch(e: Exception) {}
            return
        }

        val sampleCount = audioRecorder.getAboveThresholdSampleCount()
        if (sampleCount < 8) {
            // Cần ít nhất 8 samples (400ms) vượt ngưỡng amplitude → có giọng nói thật
            _uiState.update { SpeakingUiState.Error("Không nghe rõ giọng nói. Vui lòng nói to rõ hơn và thử lại!") }
            viewModelScope.launch {
                kotlinx.coroutines.delay(2500)
                if (_uiState.value is SpeakingUiState.Error) {
                    _uiState.update { SpeakingUiState.SessionActive }
                }
            }
            try { file.delete() } catch(e: Exception) {}
            return
        }

        // Kiểm tra kích thước file - file quá nhỏ (< 10KB) thường là im lặng hoàn toàn
        val fileSizeKb = file.length() / 1024
        if (fileSizeKb < 10) {
            _uiState.update { SpeakingUiState.Error("Không ghi âm được giọng nói. Vui lòng kiểm tra microphone!") }
            viewModelScope.launch {
                kotlinx.coroutines.delay(2500)
                if (_uiState.value is SpeakingUiState.Error) {
                    _uiState.update { SpeakingUiState.SessionActive }
                }
            }
            try { file.delete() } catch(e: Exception) {}
            return
        }

        val topicPrompt = if (_useCustomTopic.value) _customTopicText.value else _selectedTopic.value.prompt
        _uiState.update { SpeakingUiState.ProcessingTurn("AI đang nhận diện giọng nói và suy nghĩ câu hỏi tiếp theo...") }

        viewModelScope.launch {
            try {
                val audioBytes = file.readBytes()
                if (audioBytes.isEmpty()) {
                    _uiState.update { SpeakingUiState.Error("Lỗi: File ghi âm trống (0 bytes).") }
                    return@launch
                }

                val responseResult = AIModule.geminiService.generateNextTurn(
                    topic = topicPrompt,
                    mode = _selectedMode.value,
                    history = _chatMessages.value,
                    audioBytes = audioBytes
                )

                responseResult.onSuccess { jsonStr ->
                    try {
                        val cleanJson = if (jsonStr.contains("```json")) {
                            jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
                        } else if (jsonStr.contains("```")) {
                            jsonStr.substringAfter("```").substringBeforeLast("```").trim()
                        } else {
                            jsonStr.trim()
                        }

                        val aiTurn = Gson().fromJson(cleanJson, AITurnResponse::class.java)

                        // Kiểm tra nếu transcript trống hoặc là hallucination của AI
                        val transcriptTrimmed = aiTurn.transcript.trim()
                        val isSilenceOrHallucination = transcriptTrimmed.isBlank() ||
                            transcriptTrimmed == "SILENCE_DETECTED" || // sentinel từ Gemini
                            transcriptTrimmed == "..." ||
                            transcriptTrimmed.length < 3 ||
                            transcriptTrimmed.contains("[silence]", ignoreCase = true) ||
                            transcriptTrimmed.contains("[no audio]", ignoreCase = true) ||
                            transcriptTrimmed.contains("[inaudible]", ignoreCase = true) ||
                            transcriptTrimmed.contains("[không nói]", ignoreCase = true) ||
                            transcriptTrimmed.contains("[tiếng ồn]", ignoreCase = true) ||
                            transcriptTrimmed.contains("[ambient]", ignoreCase = true) ||
                            transcriptTrimmed.matches(Regex("^[.\\s,!?…]+$")) // chỉ toàn dấu câu/khoảng trắng

                        if (isSilenceOrHallucination) {
                            _uiState.update { SpeakingUiState.Error("Không nghe rõ giọng nói. Vui lòng nói to rõ hơn và thử lại!") }
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(2500)
                                if (_uiState.value is SpeakingUiState.Error) {
                                    _uiState.update { SpeakingUiState.SessionActive }
                                }
                            }
                            return@onSuccess
                        }

                        // Add User's transcribed message
                        _chatMessages.update { current ->
                            current + SpeakingChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = MessageSender.USER,
                                text = aiTurn.transcript,
                                transcript = aiTurn.transcript,
                                turnFeedback = aiTurn.turnFeedback.ifBlank { null }
                            )
                        }

                        // Add AI's reply and next question
                        val aiResponseText = "${aiTurn.reply}\n\n${aiTurn.nextQuestion}"
                        _chatMessages.update { current ->
                            current + SpeakingChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = MessageSender.AI,
                                text = aiResponseText
                            )
                        }

                        _currentTurn.update { it + 1 }
                        
                        // Set state first so user sees the text
                        if (_currentTurn.value >= _maxTurns.value) {
                            endSessionAndGetReport()
                        } else {
                            _uiState.update { SpeakingUiState.SessionActive }
                            speak(aiResponseText)
                        }
                    } catch (e: Exception) {
                        _uiState.update { SpeakingUiState.Error("Lỗi phân tích cú pháp AI: ${e.message}") }
                    }
                }.onFailure { e ->
                    _uiState.update { SpeakingUiState.Error("AI Error: ${e.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { SpeakingUiState.Error("Lỗi xử lý file âm thanh: ${e.message}") }
            } finally {
                try {
                    file.delete()
                } catch (ex: Exception) {
                    // Ignore delete errors
                }
            }
        }
    }

    fun endSessionAndGetReport() {
        stopSpeaking()
        val topicPrompt = if (_useCustomTopic.value) _customTopicText.value else _selectedTopic.value.prompt
        _uiState.update { SpeakingUiState.Evaluating("AI đang tổng hợp và chấm điểm cho buổi luyện nói của bạn...") }

        viewModelScope.launch {
            try {
                val reportResult = AIModule.geminiService.evaluateSession(
                    topic = topicPrompt,
                    mode = _selectedMode.value,
                    history = _chatMessages.value
                )

                reportResult.onSuccess { jsonStr ->
                    try {
                        val cleanJson = if (jsonStr.contains("```json")) {
                            jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
                        } else if (jsonStr.contains("```")) {
                            jsonStr.substringAfter("```").substringBeforeLast("```").trim()
                        } else {
                            jsonStr.trim()
                        }

                        val report = Gson().fromJson(cleanJson, SpeakingResult::class.java)
                        _uiState.update { SpeakingUiState.Report(report) }
                    } catch (e: Exception) {
                        _uiState.update { SpeakingUiState.Error("Lỗi phân tích cú pháp báo cáo: ${e.message}") }
                    }
                }.onFailure { e ->
                    _uiState.update { SpeakingUiState.Error("AI Error: ${e.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { SpeakingUiState.Error("Lỗi gửi báo cáo: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
