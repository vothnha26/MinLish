package com.edu.minlish.features.library.presentation.viewmodel

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
import com.edu.minlish.features.library.domain.usecase.ManageCategoryUseCase
import com.edu.minlish.features.library.data.repository.CategoryRepositoryImpl
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val lookupStrategy: LookupStrategy = LookupStrategyFactory.create(useAi = false),
    private val manageCategoryUseCase: ManageCategoryUseCase = ManageCategoryUseCase(CategoryRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<AICreateSetUiState>(AICreateSetUiState.Idle)
    val uiState: StateFlow<AICreateSetUiState> = _uiState.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _category = MutableStateFlow("IELTS")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(listOf("IELTS", "TOEIC", "Business", "Travel", "Daily", "Custom"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        val currentUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            manageCategoryUseCase.getCategories(currentUser.id)
                .onSuccess { list ->
                    _categories.update {
                        (list.map { it.name } + listOf("IELTS", "TOEIC", "Business", "Travel", "Daily", "Custom")).distinct()
                    }
                }
                .onFailure {
                    _categories.update {
                        listOf("IELTS", "TOEIC", "Business", "Travel", "Daily", "Custom")
                    }
                }
        }
    }

    private val _wordCount = MutableStateFlow(5)
    val wordCount: StateFlow<Int> = _wordCount.asStateFlow()

    private val _includeCollocations = MutableStateFlow(true)
    val includeCollocations: StateFlow<Boolean> = _includeCollocations.asStateFlow()

    fun updatePrompt(value: String) {
        _prompt.update { value }
    }

    fun updateCategory(value: String) {
        _category.update { value }
    }

    fun updateWordCount(value: Int) {
        _wordCount.update { value }
    }

    fun updateIncludeCollocations(value: Boolean) {
        _includeCollocations.update { value }
    }

    fun generateSet() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.update { AICreateSetUiState.Error("User not logged in") }
            return
        }

        if (_prompt.value.isBlank()) {
            _uiState.update { AICreateSetUiState.Error("Vui lòng nhập chủ đề yêu cầu!") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { AICreateSetUiState.Loading("AI đang sinh bộ từ vựng, vui lòng đợi...") }
                
                val aiResponseResult = AIModule.geminiService.generateVocabularySet(
                    prompt = _prompt.value,
                    category = _category.value,
                    wordCount = _wordCount.value,
                    includeCollocations = _includeCollocations.value
                )

                aiResponseResult.onSuccess { jsonStr ->
                    _uiState.update { AICreateSetUiState.Loading("Đang phân tích kết quả trả về...") }
                    
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

                    _uiState.update { AICreateSetUiState.Loading("Đang tạo bộ từ vựng mới...") }
                    
                    val newSet = VocabularySet(
                        creatorId = currentUser.id,
                        title = aiSet.title.ifBlank { "Bộ từ vựng: ${_prompt.value}" },
                        description = aiSet.description.ifBlank { "Tạo tự động bằng AI từ yêu cầu: ${_prompt.value}" },
                        category = _category.value,
                        wordCount = 0
                    )

                    val createSetResult = repository.createSetAndGetId(newSet)
                    
                    createSetResult.onSuccess { setId ->
                        _uiState.update { AICreateSetUiState.Loading("Đang tự động tra cứu và dịch nghĩa từ vựng...") }
                        
                        // Chạy song song việc tra cứu chi tiết từ vựng qua API từ điển truyền thống (Google Translate + FreeDict)
                        val wordObjectsDeferred = aiSet.words.map { aiWord ->
                            async {
                                lookupStrategy.lookupWord(aiWord.word).mapCatching { vocabWord ->
                                    // Trộn thông tin: Lấy nghĩa tiếng Việt và từ loại chuẩn ngữ cảnh từ AI ghi đè lên từ điển
                                    val updatedDefinitions = if (vocabWord.definitions.isNotEmpty()) {
                                        val hasMatchingPos = vocabWord.definitions.any { it.pos.equals(aiWord.pos, ignoreCase = true) }
                                        var overwrittenFirst = false
                                        vocabWord.definitions.map { def ->
                                            val isMatch = def.pos.equals(aiWord.pos, ignoreCase = true) || (!hasMatchingPos && !overwrittenFirst)
                                            if (isMatch) {
                                                overwrittenFirst = true
                                                def.copy(
                                                    pos = aiWord.pos.ifBlank { def.pos },
                                                    meaningVietnamese = aiWord.meaningVietnamese.ifBlank { def.meaningVietnamese }
                                                )
                                            } else {
                                                def
                                            }
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
                        
                        _uiState.update { AICreateSetUiState.Loading("Đang lưu các từ vựng vào bộ từ (0/${wordObjects.size})...") }
                        var savedCount = 0
                        
                        for (wordObj in wordObjects) {
                            val addWordResult = repository.addWord(wordObj)
                            if (addWordResult.isSuccess) {
                                savedCount++
                                _uiState.update { AICreateSetUiState.Loading("Đang lưu các từ vựng vào bộ từ ($savedCount/${wordObjects.size})...") }
                            }
                        }
                        
                        _uiState.update { AICreateSetUiState.Success }
                    }.onFailure { e ->
                        throw Exception("Không thể lưu bộ từ vào Database: ${e.message}")
                    }

                }.onFailure { e ->
                    throw Exception("AI gặp lỗi khi sinh từ vựng: ${e.message}")
                }

            } catch (e: Exception) {
                _uiState.update { AICreateSetUiState.Error(e.message ?: "Đã xảy ra lỗi không xác định.") }
            }
        }
    }
}
