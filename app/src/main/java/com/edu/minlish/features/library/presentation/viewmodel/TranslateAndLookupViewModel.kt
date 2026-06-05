package com.edu.minlish.features.library.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.core.ai.AIModule
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.data.repository.GoogleTranslationStrategy
import com.edu.minlish.features.library.data.repository.LookupStrategyFactory
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.repository.LookupStrategy
import com.edu.minlish.features.library.domain.repository.TranslationStrategy
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import com.edu.minlish.core.util.SessionDataManager
import com.edu.minlish.core.util.AppSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
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
    private val translationStrategy: TranslationStrategy = GoogleTranslationStrategy(),
    private val lookupStrategy: LookupStrategy = LookupStrategyFactory.create(useAi = false),
    private val getUserId: () -> String? = { FirebaseAuth.getInstance().currentUser?.uid }
) : ViewModel() {

    var uiState by mutableStateOf<TranslateAndLookupUiState>(TranslateAndLookupUiState.Idle)
        private set

    var inputText by mutableStateOf("")
    var translatedText by mutableStateOf("")

    // Language state for translation
    var sourceLang by mutableStateOf("Tiếng Anh")
    var targetLang by mutableStateOf("Tiếng Việt")
    var sourceLangCode by mutableStateOf("en")
    var targetLangCode by mutableStateOf("vi")

    // Recent history list for word lookup
    val recentHistory = mutableStateListOf<String>()
    
    // Extracted words from translation
    val extractedWords = mutableStateListOf<VocabularyWord>()
    
    // Detailed single word lookup result
    var lookupResult by mutableStateOf<VocabularyWord?>(null)
        private set

    val userSets = mutableStateListOf<VocabularySet>()
    
    // Saved status for words (word string -> isSaved)
    val wordSavedStatus = mutableStateMapOf<String, Boolean>()

    // We use getUserId function to avoid static dependency on FirebaseAuth during testing

    init {
        loadUserSets()
        loadRecentHistory()
    }

    fun loadRecentHistory() {
        val historyStr = AppSettings.recentLookupHistory
        if (historyStr.isNotBlank()) {
            val list = historyStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            recentHistory.clear()
            recentHistory.addAll(list.take(5))
        }
    }

    fun addWordToHistory(word: String) {
        val cleanWord = word.trim()
        if (cleanWord.isBlank()) return
        recentHistory.remove(cleanWord)
        recentHistory.add(0, cleanWord)
        if (recentHistory.size > 5) {
            recentHistory.removeLast()
        }
        AppSettings.recentLookupHistory = recentHistory.joinToString(",")
    }

    fun clearRecentHistory() {
        recentHistory.clear()
        AppSettings.recentLookupHistory = ""
    }

    fun swapLanguages() {
        val tempLang = sourceLang
        sourceLang = targetLang
        targetLang = tempLang

        val tempCode = sourceLangCode
        sourceLangCode = targetLangCode
        targetLangCode = tempCode

        val tempText = inputText
        inputText = translatedText
        translatedText = tempText
    }

    fun loadUserSets() {
        val userId = getUserId() ?: return
        
        // Load from SessionDataManager cache first for instant UI response
        SessionDataManager.vocabularySets?.let { cachedSets ->
            userSets.clear()
            userSets.addAll(cachedSets)
        }

        viewModelScope.launch {
            repository.getUserSets(userId).onSuccess { sets ->
                userSets.clear()
                userSets.addAll(sets)
                // Cache it back to SessionDataManager
                SessionDataManager.vocabularySets = sets
            }
        }
    }

    fun translateText() {
        val text = inputText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            uiState = TranslateAndLookupUiState.Loading
            extractedWords.clear()
            translatedText = ""
            wordSavedStatus.clear()

            // Dịch thuật thuần túy sử dụng API Google Translate sẵn có (Nhanh, miễn phí, không tốn AI)
            translationStrategy.translate(text, sourceLangCode, targetLangCode).onSuccess { translation ->
                translatedText = translation
                uiState = TranslateAndLookupUiState.Success
            }.onFailure { e ->
                uiState = TranslateAndLookupUiState.Error(e.message ?: "Dịch thuật thất bại. Vui lòng kiểm tra lại kết nối.")
            }
        }
    }

    fun lookupWord() {
        val word = inputText.trim()
        if (word.isBlank()) return

        viewModelScope.launch {
            uiState = TranslateAndLookupUiState.Loading
            lookupResult = null
            wordSavedStatus.clear()

            lookupStrategy.lookupWord(word).onSuccess { result ->
                lookupResult = result
                addWordToHistory(word)
                checkIfWordsSaved(listOf(result))
                uiState = TranslateAndLookupUiState.Success
            }.onFailure { e ->
                uiState = TranslateAndLookupUiState.Error(e.message ?: "Không tìm thấy thông tin từ vựng này.")
            }
        }
    }

    private fun checkIfWordsSaved(words: List<VocabularyWord>) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            // Fetch words across all sets of user to check saved status
            userSets.forEach { set ->
                repository.getWordsBySet(set.id).onSuccess { setWords ->
                    setWords.forEach { savedWord ->
                        words.forEach { word ->
                            if (savedWord.word.equals(word.word, ignoreCase = true)) {
                                wordSavedStatus[word.word] = true
                            }
                        }
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
                val existingSet = userSets.find { it.title.equals(defaultSetName, ignoreCase = true) }
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
                wordSavedStatus[word.word] = true
                
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
        uiState = TranslateAndLookupUiState.Idle
    }
}
