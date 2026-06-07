package com.edu.minlish.features.library.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.core.ai.AIModule
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.data.repository.LookupStrategyFactory
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.repository.LookupStrategy
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import com.edu.minlish.core.util.SessionDataManager
import com.edu.minlish.core.util.AppSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

sealed class TranslateAndLookupUiState {
    object Idle : TranslateAndLookupUiState()
    object Loading : TranslateAndLookupUiState()
    object Success : TranslateAndLookupUiState()
    data class Error(val message: String) : TranslateAndLookupUiState()
}

data class TranslationAndExtractionResult(
    val translatedText: String = "",
    val extractedWords: List<ExtractedWordItem> = emptyList()
)

data class ExtractedWordItem(
    val word: String = "",
    val pronunciation: String = "",
    val definitions: List<WordDefinition> = emptyList(),
    val collocations: String = "",
    val personalNote: String = ""
)

class TranslateAndLookupViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl(),
    private val lookupStrategy: LookupStrategy = LookupStrategyFactory.create(useAi = true),
    private val getUserId: () -> String? = { FirebaseAuth.getInstance().currentUser?.uid }
) : ViewModel() {

    private val _uiState = MutableStateFlow<TranslateAndLookupUiState>(TranslateAndLookupUiState.Idle)
    val uiState: StateFlow<TranslateAndLookupUiState> = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText.asStateFlow()

    // Language state for translation
    private val _sourceLang = MutableStateFlow("Tiếng Anh")
    val sourceLang: StateFlow<String> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow("Tiếng Việt")
    val targetLang: StateFlow<String> = _targetLang.asStateFlow()

    private val _sourceLangCode = MutableStateFlow("en")
    val sourceLangCode: StateFlow<String> = _sourceLangCode.asStateFlow()

    private val _targetLangCode = MutableStateFlow("vi")
    val targetLangCode: StateFlow<String> = _targetLangCode.asStateFlow()

    // Recent history list for word lookup
    private val _recentHistory = MutableStateFlow<List<String>>(emptyList())
    val recentHistory: StateFlow<List<String>> = _recentHistory.asStateFlow()
    
    // Extracted words from translation
    private val _extractedWords = MutableStateFlow<List<VocabularyWord>>(emptyList())
    val extractedWords: StateFlow<List<VocabularyWord>> = _extractedWords.asStateFlow()
    
    // Detailed single word lookup result
    private val _lookupResult = MutableStateFlow<VocabularyWord?>(null)
    val lookupResult: StateFlow<VocabularyWord?> = _lookupResult.asStateFlow()

    private val _userSets = MutableStateFlow<List<VocabularySet>>(emptyList())
    val userSets: StateFlow<List<VocabularySet>> = _userSets.asStateFlow()
    
    // Saved status for words (word string -> isSaved)
    private val _wordSavedStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val wordSavedStatus: StateFlow<Map<String, Boolean>> = _wordSavedStatus.asStateFlow()

    // We use getUserId function to avoid static dependency on FirebaseAuth during testing

    init {
        loadUserSets()
        loadRecentHistory()
    }

    fun updateInputText(text: String) {
        _inputText.update { text }
    }

    fun updateTranslatedText(text: String) {
        _translatedText.update { text }
    }

    fun loadRecentHistory() {
        val historyStr = AppSettings.recentLookupHistory
        if (historyStr.isNotBlank()) {
            val list = historyStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            _recentHistory.update { list.take(5) }
        } else {
            _recentHistory.update { emptyList() }
        }
    }

    fun addWordToHistory(word: String) {
        val cleanWord = word.trim()
        if (cleanWord.isBlank()) return
        _recentHistory.update { currentList ->
            val newList = currentList.toMutableList()
            newList.remove(cleanWord)
            newList.add(0, cleanWord)
            if (newList.size > 5) {
                newList.removeAt(newList.lastIndex)
            }
            AppSettings.recentLookupHistory = newList.joinToString(",")
            newList
        }
    }

    fun clearRecentHistory() {
        _recentHistory.update { emptyList() }
        AppSettings.recentLookupHistory = ""
    }

    fun swapLanguages() {
        val tempLang = _sourceLang.value
        val tempCode = _sourceLangCode.value
        val tempText = _inputText.value

        _sourceLang.update { _targetLang.value }
        _targetLang.update { tempLang }

        _sourceLangCode.update { _targetLangCode.value }
        _targetLangCode.update { tempCode }

        _inputText.update { _translatedText.value }
        _translatedText.update { tempText }
    }

    fun loadUserSets() {
        val userId = getUserId() ?: return
        
        // Load from SessionDataManager cache first for instant UI response
        SessionDataManager.vocabularySets?.let { cachedSets ->
            _userSets.update { cachedSets }
        }

        viewModelScope.launch {
            repository.getUserSets(userId).onSuccess { sets ->
                _userSets.update { sets }
                // Cache it back to SessionDataManager
                SessionDataManager.vocabularySets = sets
            }
        }
    }

    fun translateText() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { TranslateAndLookupUiState.Loading }
            _extractedWords.update { emptyList() }
            _translatedText.update { "" }
            _wordSavedStatus.update { emptyMap() }

            // Sử dụng Gemini AI để dịch thuật chất lượng cao và tự động trích xuất từ vựng nổi bật giống smartSearch
            AIModule.geminiService.translateAndExtractVocabulary(
                text = text,
                sourceLang = _sourceLang.value,
                targetLang = _targetLang.value
            ).onSuccess { jsonStr ->
                try {
                    val cleanJson = if (jsonStr.contains("```json")) {
                        jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
                    } else if (jsonStr.contains("```")) {
                        jsonStr.substringAfter("```").substringBeforeLast("```").trim()
                    } else {
                        jsonStr.trim()
                    }
                    
                    val result = Gson().fromJson(cleanJson, TranslationAndExtractionResult::class.java)
                    _translatedText.update { result.translatedText }
                    
                    val words = result.extractedWords.map { item ->
                        VocabularyWord(
                            word = item.word,
                            pronunciation = item.pronunciation,
                            definitions = item.definitions,
                            collocations = item.collocations,
                            personalNote = item.personalNote
                        )
                    }
                    _extractedWords.update { words }
                    checkIfWordsSaved(words)
                    
                    _uiState.update { TranslateAndLookupUiState.Success }
                } catch (e: Exception) {
                    _uiState.update { TranslateAndLookupUiState.Error("Lỗi xử lý dữ liệu từ AI: ${e.message}") }
                }
            }.onFailure { e ->
                _uiState.update { TranslateAndLookupUiState.Error(e.message ?: "Dịch thuật và trích xuất thất bại. Vui lòng kiểm tra lại kết nối.") }
            }
        }
    }

    fun lookupWord() {
        val word = _inputText.value.trim()
        if (word.isBlank()) return

        viewModelScope.launch {
            _uiState.update { TranslateAndLookupUiState.Loading }
            _lookupResult.update { null }
            _wordSavedStatus.update { emptyMap() }

            lookupStrategy.lookupWord(word).onSuccess { result ->
                _lookupResult.update { result }
                addWordToHistory(word)
                checkIfWordsSaved(listOf(result))
                _uiState.update { TranslateAndLookupUiState.Success }
            }.onFailure { e ->
                _uiState.update { TranslateAndLookupUiState.Error(e.message ?: "Không tìm thấy thông tin từ vựng này.") }
            }
        }
    }

    private fun checkIfWordsSaved(words: List<VocabularyWord>) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            // Fetch words across all sets of user to check saved status
            _userSets.value.forEach { set ->
                repository.getWordsBySet(set.id).onSuccess { setWords ->
                    _wordSavedStatus.update { currentMap ->
                        val statusMap = currentMap.toMutableMap()
                        setWords.forEach { savedWord ->
                            words.forEach { word ->
                                if (savedWord.word.equals(word.word, ignoreCase = true)) {
                                    statusMap[word.word] = true
                                }
                            }
                        }
                        statusMap
                    }
                }
            }
        }
    }

    fun quickAddWord(word: VocabularyWord, targetSetId: String?, onComplete: (Result<Unit>) -> Unit) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            val setId = if (targetSetId != null) {
                targetSetId
            } else {
                // Find or create default set "Quick Notes"
                val defaultSetName = "Quick Notes"
                val existingSet = _userSets.value.find { it.title.equals(defaultSetName, ignoreCase = true) }
                if (existingSet != null) {
                    existingSet.id
                } else {
                    val newSet = VocabularySet(
                        title = defaultSetName,
                        description = "Từ vựng tra cứu nhanh từ tính năng Dịch",
                        category = "General",
                        creatorId = userId,
                        createdAt = Date()
                    )
                    val createResult = repository.createSetAndGetId(newSet)
                    if (createResult.isSuccess) {
                        val newId = createResult.getOrThrow()
                        loadUserSets() // Refresh list
                        newId
                    } else {
                        onComplete(Result.failure(createResult.exceptionOrNull() ?: Exception("Failed to create default set")))
                        return@launch
                    }
                }
            }

            val finalWord = word.copy(vocabularySetId = setId)
            repository.addWord(finalWord).onSuccess {
                _wordSavedStatus.update { currentMap ->
                    val statusMap = currentMap.toMutableMap()
                    statusMap[word.word] = true
                    statusMap
                }
                
                // Refresh local session data in background for instant updates across the app
                viewModelScope.launch {
                    try {
                        SessionDataManager.preFetchUserData(userId)
                    } catch (e: Exception) {
                        // ignore cache refresh failures
                    }
                }
                
                onComplete(Result.success(Unit))
            }.onFailure { e ->
                onComplete(Result.failure(e))
            }
        }
    }

    fun resetState() {
        _uiState.update { TranslateAndLookupUiState.Idle }
    }
}
