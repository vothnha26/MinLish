package com.edu.minlish.features.speaking.presentation.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.core.ai.AIModule
import com.edu.minlish.core.util.AudioRecorder
import com.edu.minlish.features.speaking.domain.model.MessageSender
import com.edu.minlish.features.speaking.domain.model.SpeakingChatMessage
import com.edu.minlish.features.speaking.domain.model.SpeakingResult
import com.edu.minlish.features.speaking.domain.model.SpeakingTopic
import com.google.gson.Gson
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

    var uiState by mutableStateOf<SpeakingUiState>(SpeakingUiState.TopicSelection)
        private set

    val topics = listOf(
        SpeakingTopic("1", "Hobbies", "Talk about your favorite hobby. What is it, when did you start, and why do you enjoy it?"),
        SpeakingTopic("2", "Travel", "Describe a memorable trip you took. Where did you go, who did you go with, and what did you do?"),
        SpeakingTopic("3", "Future Goals", "What are your main goals for the next 5 years? How do you plan to achieve them?")
    )

    var selectedTopic by mutableStateOf(topics[0])
    var customTopicText by mutableStateOf("")
    var useCustomTopic by mutableStateOf(false)
    var selectedMode by mutableStateOf("Daily Conversation")

    val chatMessages = mutableStateListOf<SpeakingChatMessage>()
    
    var currentTurn by mutableStateOf(0)
        private set
        
    var maxTurns by mutableStateOf(3)

    var isRecording by mutableStateOf(false)
        private set

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
        useCustomTopic = false
        selectedTopic = topic
    }

    fun useTopicSelection() {
        stopSpeaking()
        uiState = SpeakingUiState.TopicSelection
    }

    fun startSession() {
        val topicPrompt = if (useCustomTopic) {
            if (customTopicText.isBlank()) {
                uiState = SpeakingUiState.Error("Vui lòng nhập chủ đề bạn muốn luyện nói!")
                return
            }
            customTopicText
        } else {
            selectedTopic.prompt
        }

        uiState = SpeakingUiState.ProcessingTurn("AI đang khởi động buổi trò chuyện...")
        chatMessages.clear()
        currentTurn = 0

        viewModelScope.launch {
            try {
                val firstQuestionResult = AIModule.geminiService.generateFirstQuestion(topicPrompt, selectedMode)
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

                        chatMessages.add(
                            SpeakingChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = MessageSender.AI,
                                text = question
                            )
                        )
                        uiState = SpeakingUiState.SessionActive
                        speak(question)
                    } catch (e: Exception) {
                        val fallback = jsonStr.trim()
                        chatMessages.add(
                            SpeakingChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = MessageSender.AI,
                                text = fallback
                            )
                        )
                        uiState = SpeakingUiState.SessionActive
                        speak(fallback)
                    }
                }
                firstQuestionResult.onFailure { e ->
                    uiState = SpeakingUiState.Error("Không thể tạo câu hỏi đầu tiên: ${e.message}")
                }
            } catch (e: Exception) {
                uiState = SpeakingUiState.Error("Đã xảy ra lỗi: ${e.message}")
            }
        }
    }

    fun toggleRecording() {
        if (isRecording) {
            stopRecordingAndAnalyze()
        } else {
            stopSpeaking()
            val started = audioRecorder.startRecording()
            if (started) {
                isRecording = true
                recordStartTime = System.currentTimeMillis()
                uiState = SpeakingUiState.SessionActive
            } else {
                uiState = SpeakingUiState.Error("Không thể bắt đầu ghi âm. Hãy kiểm tra quyền Microphone.")
            }
        }
    }

    private fun stopRecordingAndAnalyze() {
        isRecording = false
        val duration = System.currentTimeMillis() - recordStartTime
        val file = audioRecorder.stopRecording()
        
        if (file == null || !file.exists()) {
            uiState = SpeakingUiState.Error("Lỗi ghi âm: File không tồn tại")
            return
        }

        if (duration < 1000) {
            uiState = SpeakingUiState.Error("Thời gian nói quá ngắn (tối thiểu 1 giây). Vui lòng thử lại!")
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                if (uiState is SpeakingUiState.Error) {
                    uiState = SpeakingUiState.SessionActive
                }
            }
            try { file.delete() } catch(e: Exception) {}
            return
        }

        val sampleCount = audioRecorder.getAboveThresholdSampleCount()
        if (sampleCount < 8) {
            // Cần ít nhất 8 samples (400ms) vượt ngưỡng amplitude → có giọng nói thật
            uiState = SpeakingUiState.Error("Không nghe rõ giọng nói. Vui lòng nói to rõ hơn và thử lại!")
            viewModelScope.launch {
                kotlinx.coroutines.delay(2500)
                if (uiState is SpeakingUiState.Error) {
                    uiState = SpeakingUiState.SessionActive
                }
            }
            try { file.delete() } catch(e: Exception) {}
            return
        }

        // Kiểm tra kích thước file - file quá nhỏ (< 10KB) thường là im lặng hoàn toàn
        val fileSizeKb = file.length() / 1024
        if (fileSizeKb < 10) {
            uiState = SpeakingUiState.Error("Không ghi âm được giọng nói. Vui lòng kiểm tra microphone!")
            viewModelScope.launch {
                kotlinx.coroutines.delay(2500)
                if (uiState is SpeakingUiState.Error) {
                    uiState = SpeakingUiState.SessionActive
                }
            }
            try { file.delete() } catch(e: Exception) {}
            return
        }

        val topicPrompt = if (useCustomTopic) customTopicText else selectedTopic.prompt
        uiState = SpeakingUiState.ProcessingTurn("AI đang nhận diện giọng nói và suy nghĩ câu hỏi tiếp theo...")

        viewModelScope.launch {
            try {
                val audioBytes = file.readBytes()
                if (audioBytes.isEmpty()) {
                    uiState = SpeakingUiState.Error("Lỗi: File ghi âm trống (0 bytes).")
                    return@launch
                }

                val responseResult = AIModule.geminiService.generateNextTurn(
                    topic = topicPrompt,
                    mode = selectedMode,
                    history = chatMessages,
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
                            uiState = SpeakingUiState.Error("Không nghe rõ giọng nói. Vui lòng nói to rõ hơn và thử lại!")
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(2500)
                                if (uiState is SpeakingUiState.Error) {
                                    uiState = SpeakingUiState.SessionActive
                                }
                            }
                            return@onSuccess
                        }

                        // Add User's transcribed message
                        chatMessages.add(
                            SpeakingChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = MessageSender.USER,
                                text = aiTurn.transcript,
                                transcript = aiTurn.transcript,
                                turnFeedback = aiTurn.turnFeedback.ifBlank { null }
                            )
                        )

                        // Add AI's reply and next question
                        val aiResponseText = "${aiTurn.reply}\n\n${aiTurn.nextQuestion}"
                        chatMessages.add(
                            SpeakingChatMessage(
                                id = UUID.randomUUID().toString(),
                                sender = MessageSender.AI,
                                text = aiResponseText
                            )
                        )

                        currentTurn++
                        
                        // Set state first so user sees the text
                        if (currentTurn >= maxTurns) {
                            endSessionAndGetReport()
                        } else {
                            uiState = SpeakingUiState.SessionActive
                            speak(aiResponseText)
                        }
                    } catch (e: Exception) {
                        uiState = SpeakingUiState.Error("Lỗi phân tích cú pháp AI: ${e.message}")
                    }
                }.onFailure { e ->
                    uiState = SpeakingUiState.Error("AI Error: ${e.message}")
                }
            } catch (e: Exception) {
                uiState = SpeakingUiState.Error("Lỗi xử lý file âm thanh: ${e.message}")
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
        val topicPrompt = if (useCustomTopic) customTopicText else selectedTopic.prompt
        uiState = SpeakingUiState.Evaluating("AI đang tổng hợp và chấm điểm cho buổi luyện nói của bạn...")

        viewModelScope.launch {
            try {
                val reportResult = AIModule.geminiService.evaluateSession(
                    topic = topicPrompt,
                    mode = selectedMode,
                    history = chatMessages
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
                        uiState = SpeakingUiState.Report(report)
                    } catch (e: Exception) {
                        uiState = SpeakingUiState.Error("Lỗi phân tích cú pháp báo cáo: ${e.message}")
                    }
                }.onFailure { e ->
                    uiState = SpeakingUiState.Error("AI Error: ${e.message}")
                }
            } catch (e: Exception) {
                uiState = SpeakingUiState.Error("Lỗi gửi báo cáo: ${e.message}")
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
