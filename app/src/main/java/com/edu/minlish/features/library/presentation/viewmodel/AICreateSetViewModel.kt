package com.edu.minlish.features.library.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.core.ai.AIModule
import com.edu.minlish.core.ai.model.AIGeneratedSet
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.data.repository.LookupStrategyFactory
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import com.edu.minlish.features.library.domain.repository.LookupStrategy
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.Date

sealed class AICreateSetUiState {
    object Idle : AICreateSetUiState()
    data class Loading(val message: String) : AICreateSetUiState()
    object Success : AICreateSetUiState()
    data class Error(val message: String) : AICreateSetUiState()
}

class AICreateSetViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val lookupStrategy: LookupStrategy = LookupStrategyFactory.create(useAi = false)
) : ViewModel() {

    var uiState by mutableStateOf<AICreateSetUiState>(AICreateSetUiState.Idle)
        private set

    var prompt by mutableStateOf("")
    var category by mutableStateOf("IELTS")
    var wordCount by mutableStateOf(5)
    var includeCollocations by mutableStateOf(true)

    fun generateSet() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = AICreateSetUiState.Error("User not logged in")
            return
        }

        if (prompt.isBlank()) {
            uiState = AICreateSetUiState.Error("Vui lòng nhập chủ đề yêu cầu!")
            return
        }

        viewModelScope.launch {
            try {
                uiState = AICreateSetUiState.Loading("AI đang sinh bộ từ vựng, vui lòng đợi...")
                
                val aiResponseResult = AIModule.geminiService.generateVocabularySet(
                    prompt = prompt,
                    category = category,
                    wordCount = wordCount,
                    includeCollocations = includeCollocations
                )

                aiResponseResult.onSuccess { jsonStr ->
                    uiState = AICreateSetUiState.Loading("Đang phân tích kết quả trả về...")
                    
                    val cleanJson = if (jsonStr.contains("```json")) {
                        jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
                    } else if (jsonStr.contains("```")) {
                        jsonStr.substringAfter("```").substringBeforeLast("```").trim()
                    } else {
                        jsonStr.trim()
                    }

                    val aiSet = try {
                        Gson().fromJson(cleanJson, AIGeneratedSet::class.java)
                    } catch (e: Exception) {
                        throw Exception("Không thể parse JSON từ AI: ${e.message}")
                    }

                    if (aiSet == null || aiSet.words.isEmpty()) {
                        throw Exception("AI không tạo được từ vựng nào.")
                    }

                    uiState = AICreateSetUiState.Loading("Đang tạo bộ từ vựng mới...")
                    
                    val newSet = VocabularySet(
                        creatorId = currentUser.id,
                        title = aiSet.title.ifBlank { "Bộ từ vựng: $prompt" },
                        description = aiSet.description.ifBlank { "Tạo tự động bằng AI từ yêu cầu: $prompt" },
                        category = category,
                        wordCount = 0
                    )

                    val createSetResult = repository.createSetAndGetId(newSet)
                    
                    createSetResult.onSuccess { setId ->
                        uiState = AICreateSetUiState.Loading("Đang tự động tra cứu và dịch nghĩa từ vựng...")
                        
                        // Chạy song song việc tra cứu chi tiết từ vựng qua API từ điển truyền thống (Google Translate + FreeDict)
                        val wordObjectsDeferred = aiSet.words.map { aiWord ->
                            async {
                                lookupStrategy.lookupWord(aiWord.word).mapCatching { vocabWord ->
                                    // Trộn thông tin: Lấy nghĩa tiếng Việt và từ loại chuẩn ngữ cảnh từ AI ghi đè lên từ điển
                                    val updatedDefinitions = if (vocabWord.definitions.isNotEmpty()) {
                                        vocabWord.definitions.map { def ->
                                            def.copy(
                                                pos = aiWord.pos.ifBlank { def.pos },
                                                meaningVietnamese = aiWord.meaningVietnamese.ifBlank { def.meaningVietnamese }
                                            )
                                        }
                                    } else {
                                        listOf(
                                            WordDefinition(
                                                pos = aiWord.pos,
                                                meaningVietnamese = aiWord.meaningVietnamese
                                            )
                                        )
                                    }
                                    
                                    vocabWord.copy(
                                        vocabularySetId = setId,
                                        pronunciation = aiWord.pronunciation.ifBlank { vocabWord.pronunciation },
                                        definitions = updatedDefinitions,
                                        createdAt = Date()
                                    )
                                }.getOrElse {
                                    // Fallback nếu API tra cứu bị lỗi, ta vẫn tạo từ vựng có sẵn thông tin từ AI
                                    VocabularyWord(
                                        vocabularySetId = setId,
                                        word = aiWord.word,
                                        pronunciation = aiWord.pronunciation,
                                        definitions = listOf(
                                            WordDefinition(
                                                pos = aiWord.pos,
                                                meaningVietnamese = aiWord.meaningVietnamese
                                            )
                                        ),
                                        createdAt = Date()
                                    )
                                }
                            }
                        }
                        
                        val wordObjects = wordObjectsDeferred.awaitAll()
                        
                        uiState = AICreateSetUiState.Loading("Đang lưu các từ vựng vào bộ từ (0/${wordObjects.size})...")
                        var savedCount = 0
                        
                        for (wordObj in wordObjects) {
                            val addWordResult = repository.addWord(wordObj)
                            if (addWordResult.isSuccess) {
                                savedCount++
                                uiState = AICreateSetUiState.Loading("Đang lưu các từ vựng vào bộ từ ($savedCount/${wordObjects.size})...")
                            }
                        }
                        
                        uiState = AICreateSetUiState.Success
                    }.onFailure { e ->
                        throw Exception("Không thể lưu bộ từ vào Database: ${e.message}")
                    }

                }.onFailure { e ->
                    throw Exception("AI gặp lỗi khi sinh từ vựng: ${e.message}")
                }

            } catch (e: Exception) {
                uiState = AICreateSetUiState.Error(e.message ?: "Đã xảy ra lỗi không xác định.")
            }
        }
    }
}
