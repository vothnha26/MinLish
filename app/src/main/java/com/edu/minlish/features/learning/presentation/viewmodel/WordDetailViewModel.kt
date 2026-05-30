package com.edu.minlish.features.learning.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import kotlinx.coroutines.launch

sealed class WordDetailUiState {
    object Loading : WordDetailUiState()
    data class Success(val word: VocabularyWord) : WordDetailUiState()
    data class Error(val message: String) : WordDetailUiState()
}

class WordDetailViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf<WordDetailUiState>(WordDetailUiState.Loading)
        private set

    fun loadWord(wordId: String) {
        viewModelScope.launch {
            uiState = WordDetailUiState.Loading
            repository.getWordById(wordId)
                .onSuccess { word ->
                    uiState = WordDetailUiState.Success(word)
                }
                .onFailure { e ->
                    uiState = WordDetailUiState.Error(e.message ?: "Failed to load word")
                }
        }
    }

    fun deleteWord(wordId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteWord(wordId)
                .onSuccess {
                    onDeleted()
                }
                .onFailure { e ->
                    uiState = WordDetailUiState.Error(e.message ?: "Failed to delete word")
                }
        }
    }
}
