package com.edu.minlish.features.library.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class WordListUiState {
    object Loading : WordListUiState()
    data class Success(val words: List<VocabularyWord>) : WordListUiState()
    data class Error(val message: String) : WordListUiState()
}

class WordListViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf<WordListUiState>(WordListUiState.Loading)
        private set

    var masteryPercentage by mutableStateOf(0.0f)
        private set

    var wordProgresses by mutableStateOf<Map<String, com.edu.minlish.features.learning.domain.model.UserWordProgress>>(emptyMap())
        private set

    var searchQuery by mutableStateOf("")

    val filteredWords: List<VocabularyWord>
        get() {
            val state = uiState
            if (state !is WordListUiState.Success) return emptyList()
            return state.words.filter { word ->
                searchQuery.isBlank() || 
                        word.word.contains(searchQuery, ignoreCase = true) || 
                        word.definitions.any { def -> 
                            def.meaningVietnamese.contains(searchQuery, ignoreCase = true) || 
                            def.definitionEnglish.contains(searchQuery, ignoreCase = true) 
                        }
            }
        }

    var vocabularySet by mutableStateOf<com.edu.minlish.features.library.domain.model.VocabularySet?>(null)
        private set

    fun loadWords(setId: String) {
        viewModelScope.launch {
            uiState = WordListUiState.Loading
            
            val setRepoResult: Result<com.edu.minlish.features.library.domain.model.VocabularySet> = repository.getSetById(setId)
            setRepoResult.onSuccess { set ->
                vocabularySet = set
            }

            repository.getWordsBySet(setId)
                .onSuccess { words ->
                    uiState = WordListUiState.Success(words)
                    
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
                            wordProgresses = progresses.associateBy { it.wordId }
                            val masteredThreshold = com.edu.minlish.core.util.AppSettings.masteredThreshold
                            val mastered = progresses.count { it.status == "mastered" || it.interval > masteredThreshold }
                            masteryPercentage = mastered.toFloat() / words.size.toFloat()
                        } catch (e: Exception) {
                            wordProgresses = emptyMap()
                            masteryPercentage = 0.0f
                        }
                    } else {
                        masteryPercentage = 0.0f
                    }
                }
                .onFailure { e ->
                    uiState = WordListUiState.Error(e.message ?: "Failed to load words")
                }
        }
    }
}
