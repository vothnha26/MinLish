package com.edu.minlish.features.speaking.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.core.ai.AIModule
import com.edu.minlish.core.util.AudioRecorder
import com.edu.minlish.features.speaking.domain.model.SpeakingResult
import com.edu.minlish.features.speaking.domain.model.SpeakingTopic
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File

sealed class SpeakingUiState {
    object Idle : SpeakingUiState()
    object Recording : SpeakingUiState()
    object Processing : SpeakingUiState()
    data class Result(val result: SpeakingResult) : SpeakingUiState()
    data class Error(val message: String) : SpeakingUiState()
}

class SpeakingViewModel(context: Context) : ViewModel() {

    private val audioRecorder = AudioRecorder(context)

    var uiState by mutableStateOf<SpeakingUiState>(SpeakingUiState.Idle)
        private set

    val topics = listOf(
        SpeakingTopic("1", "Hobbies", "Talk about your favorite hobby. What is it, when did you start, and why do you enjoy it?"),
        SpeakingTopic("2", "Travel", "Describe a memorable trip you took. Where did you go, who did you go with, and what did you do?"),
        SpeakingTopic("3", "Future Goals", "What are your main goals for the next 5 years? How do you plan to achieve them?")
    )

    var selectedTopic by mutableStateOf(topics[0])

    fun selectTopic(topic: SpeakingTopic) {
        if (uiState !is SpeakingUiState.Recording && uiState !is SpeakingUiState.Processing) {
            selectedTopic = topic
            uiState = SpeakingUiState.Idle
        }
    }

    fun toggleRecording() {
        if (audioRecorder.isRecording) {
            stopRecordingAndAnalyze()
        } else {
            val started = audioRecorder.startRecording()
            if (started) {
                uiState = SpeakingUiState.Recording
            } else {
                uiState = SpeakingUiState.Error("Không thể bắt đầu ghi âm. Hãy kiểm tra quyền Microphone.")
            }
        }
    }

    private fun stopRecordingAndAnalyze() {
        val file = audioRecorder.stopRecording()
        if (file == null || !file.exists()) {
            uiState = SpeakingUiState.Error("Lỗi ghi âm: File không tồn tại")
            return
        }

        uiState = SpeakingUiState.Processing

        viewModelScope.launch {
            try {
                val audioBytes = file.readBytes()
                val response = AIModule.geminiService.evaluateSpeaking(selectedTopic.prompt, audioBytes)

                response.onSuccess { jsonStr ->
                    try {
                        val cleanJson = if (jsonStr.contains("```json")) {
                            jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
                        } else if (jsonStr.contains("```")) {
                            jsonStr.substringAfter("```").substringBeforeLast("```").trim()
                        } else {
                            jsonStr
                        }

                        val result = Gson().fromJson(cleanJson, SpeakingResult::class.java)
                        uiState = SpeakingUiState.Result(result)
                    } catch (e: Exception) {
                        uiState = SpeakingUiState.Error("Lỗi phân tích kết quả AI: ${e.message}")
                    }
                }.onFailure { e ->
                    uiState = SpeakingUiState.Error("AI Error: ${e.message}")
                }
            } catch (e: Exception) {
                 uiState = SpeakingUiState.Error("Lỗi đọc file âm thanh: ${e.message}")
            } finally {
                // Xóa file tạm sau khi đã xử lý xong (nếu muốn)
                // file.delete()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
    }
}
