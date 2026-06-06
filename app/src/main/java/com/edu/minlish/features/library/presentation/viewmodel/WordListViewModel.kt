package com.edu.minlish.features.library.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import android.content.Context
import android.net.Uri
import com.edu.minlish.features.library.data.exporter.VocabularyExportManager

sealed class WordListUiState {
    object Loading : WordListUiState()
    data class Success(val words: List<VocabularyWord>) : WordListUiState()
    data class Error(val message: String) : WordListUiState()
}

class WordListViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl(),
    private val exportManager: VocabularyExportManager = VocabularyExportManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WordListUiState>(WordListUiState.Loading)
    val uiState: StateFlow<WordListUiState> = _uiState.asStateFlow()

    private val _exportUiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportUiState: StateFlow<ExportUiState> = _exportUiState.asStateFlow()

    private val _masteryPercentage = MutableStateFlow(0.0f)
    val masteryPercentage: StateFlow<Float> = _masteryPercentage.asStateFlow()

    private val _wordProgresses = MutableStateFlow<Map<String, com.edu.minlish.features.learning.domain.model.UserWordProgress>>(emptyMap())
    val wordProgresses: StateFlow<Map<String, com.edu.minlish.features.learning.domain.model.UserWordProgress>> = _wordProgresses.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredWords: StateFlow<List<VocabularyWord>> = combine(uiState, searchQuery) { state, query ->
        if (state !is WordListUiState.Success) return@combine emptyList<VocabularyWord>()
        state.words.filter { word ->
            query.isBlank() || 
                    word.word.contains(query, ignoreCase = true) || 
                    word.definitions.any { def -> 
                        def.meaningVietnamese.contains(query, ignoreCase = true) || 
                        def.definitionEnglish.contains(query, ignoreCase = true) 
                    }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _vocabularySet = MutableStateFlow<com.edu.minlish.features.library.domain.model.VocabularySet?>(null)
    val vocabularySet: StateFlow<com.edu.minlish.features.library.domain.model.VocabularySet?> = _vocabularySet.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadWords(setId: String) {
        viewModelScope.launch {
            if (_uiState.value !is WordListUiState.Success) {
                _uiState.value = WordListUiState.Loading
            }
            
            val setRepoResult: Result<com.edu.minlish.features.library.domain.model.VocabularySet> = repository.getSetById(setId)
            setRepoResult.onSuccess { set ->
                _vocabularySet.value = set
            }

            repository.getWordsBySet(setId)
                .onSuccess { words ->
                    _uiState.value = WordListUiState.Success(words)
                    
                    val currentUser = com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl().getCurrentUser()
                    if (currentUser != null && words.isNotEmpty()) {
                        try {
                            val progressSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("user_word_progress")
                                .whereEqualTo("userId", currentUser.id)
                                .whereEqualTo("setId", setId)
                                .get()
                                .await()
                            
                            val progresses = progressSnapshot.toObjects(com.edu.minlish.features.learning.domain.model.UserWordProgress::class.java)
                            _wordProgresses.value = progresses.associateBy { it.wordId }
                            val masteredThreshold = com.edu.minlish.core.util.AppSettings.masteredThreshold
                            val mastered = progresses.count { it.status == "mastered" || it.interval > masteredThreshold }
                            _masteryPercentage.value = mastered.toFloat() / words.size.toFloat()
                        } catch (e: Exception) {
                            _wordProgresses.value = emptyMap()
                            _masteryPercentage.value = 0.0f
                        }
                    } else {
                        _masteryPercentage.value = 0.0f
                    }
                }
                .onFailure { e ->
                    _uiState.value = WordListUiState.Error(e.message ?: "Failed to load words")
                }
        }
    }

    fun startExport(context: Context, uri: Uri) {
        val state = _uiState.value
        if (state !is WordListUiState.Success) return

        viewModelScope.launch {
            _exportUiState.value = ExportUiState.Exporting
            exportManager.exportToCsv(context, uri, state.words)
                .onSuccess {
                    _exportUiState.value = ExportUiState.Success(uri.path?.substringAfterLast('/') ?: "vocabulary.csv")
                }
                .onFailure { e ->
                    _exportUiState.value = ExportUiState.Error(e.message ?: "Failed to export vocabulary")
                }
        }
    }

    fun clearExportState() {
        _exportUiState.value = ExportUiState.Idle
    }
}
