package com.edu.minlish.features.learning.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class WordDetailUiState {
    object Loading : WordDetailUiState()
    data class Success(val word: VocabularyWord) : WordDetailUiState()
    data class Error(val message: String) : WordDetailUiState()
}

class WordDetailViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WordDetailUiState>(WordDetailUiState.Loading)
    val uiState: StateFlow<WordDetailUiState> = _uiState.asStateFlow()

    fun loadWord(wordId: String) {
        viewModelScope.launch {
            _uiState.update { WordDetailUiState.Loading }
            repository.getWordById(wordId)
                .onSuccess { word ->
                    _uiState.update { WordDetailUiState.Success(word) }
                }
                .onFailure { e ->
                    _uiState.update { WordDetailUiState.Error(e.message ?: "Failed to load word") }
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
                    _uiState.update { WordDetailUiState.Error(e.message ?: "Failed to delete word") }
                }
        }
    }
}
