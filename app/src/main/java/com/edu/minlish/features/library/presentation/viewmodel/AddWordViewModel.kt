package com.edu.minlish.features.library.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.data.repository.LookupStrategyFactory
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.repository.LookupStrategy
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class AddWordUiState {
    object Idle : AddWordUiState()
    object Loading : AddWordUiState()
    object Success : AddWordUiState()
    data class Error(val message: String) : AddWordUiState()
}

class AddWordViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl(),
    private val lookupStrategy: LookupStrategy = LookupStrategyFactory.create(useAi = true)
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddWordUiState>(AddWordUiState.Idle)
    val uiState: StateFlow<AddWordUiState> = _uiState.asStateFlow()

    private var loadedWordId: String? = null
    
    private var isInitialized = false

    fun initEditMode(wordId: String) {
        if (isInitialized) return
        isInitialized = true
        loadedWordId = wordId
        viewModelScope.launch {
            _uiState.update { AddWordUiState.Loading }
            repository.getWordById(wordId)
                .onSuccess { word ->
                    _wordText.update { word.word }
                    _pronunciationText.update { word.pronunciation }
                    _audioUrl.update { word.audioUrl }
                    _imageUrl.update { word.imageUrl }
                    _collocationText.update { word.collocations }
                    _personalNoteText.update { word.personalNote }
                    
                    val list = word.definitions.toMutableList()
                    if (list.isEmpty()) {
                        list.add(WordDefinition())
                    }
                    _definitions.update { list }
                    _uiState.update { AddWordUiState.Idle }
                }
                .onFailure { e ->
                    _uiState.update { AddWordUiState.Error(e.message ?: "Failed to load vocabulary for editing") }
                }
        }
    }

    private val _wordText = MutableStateFlow("")
    val wordText: StateFlow<String> = _wordText.asStateFlow()

    private val _pronunciationText = MutableStateFlow("")
    val pronunciationText: StateFlow<String> = _pronunciationText.asStateFlow()

    private val _audioUrl = MutableStateFlow("")
    val audioUrl: StateFlow<String> = _audioUrl.asStateFlow()

    private val _imageUrl = MutableStateFlow("")
    val imageUrl: StateFlow<String> = _imageUrl.asStateFlow()

    private val _collocationText = MutableStateFlow("")
    val collocationText: StateFlow<String> = _collocationText.asStateFlow()

    private val _personalNoteText = MutableStateFlow("")
    val personalNoteText: StateFlow<String> = _personalNoteText.asStateFlow()

    // List of dynamic definitions
    private val _definitions = MutableStateFlow<List<WordDefinition>>(listOf(WordDefinition()))
    val definitions: StateFlow<List<WordDefinition>> = _definitions.asStateFlow()

    private val _selectionItems = MutableStateFlow<List<SelectionItem>>(emptyList())
    val selectionItems: StateFlow<List<SelectionItem>> = _selectionItems.asStateFlow()

    private val _showSelectionDialog = MutableStateFlow(false)
    val showSelectionDialog: StateFlow<Boolean> = _showSelectionDialog.asStateFlow()

    fun updateWordText(value: String) { _wordText.update { value } }
    fun updatePronunciationText(value: String) { _pronunciationText.update { value } }
    fun updateCollocationText(value: String) { _collocationText.update { value } }
    fun updatePersonalNoteText(value: String) { _personalNoteText.update { value } }
    fun updateShowSelectionDialog(value: Boolean) { _showSelectionDialog.update { value } }

    fun addDefinitionField() {
        _definitions.update { it + WordDefinition() }
    }

    fun removeDefinitionField(index: Int) {
        _definitions.update { 
            if (it.size > 1) {
                it.toMutableList().apply { removeAt(index) }
            } else it
        }
    }

    fun updateDefinition(index: Int, updated: WordDefinition) {
        _definitions.update { 
            it.toMutableList().apply { this[index] = updated }
        }
    }

    fun smartSearch() {
        if (_wordText.value.isBlank()) return

        viewModelScope.launch {
            _uiState.update { AddWordUiState.Loading }
            val cleanWord = _wordText.value.lowercase().trim()

            lookupStrategy.lookupWord(cleanWord)
                .onSuccess { word ->
                    _pronunciationText.update { word.pronunciation }
                    _collocationText.update { word.collocations }
                    if (word.personalNote.isNotBlank()) {
                        _personalNoteText.update { word.personalNote }
                    }
                    _imageUrl.update { word.imageUrl }
                    _audioUrl.update { "" }
                    
                    val items = word.definitions.map { def ->
                        SelectionItem(
                            partOfSpeech = def.pos,
                            definition = def.definitionEnglish,
                            example = def.exampleSentence,
                            meaningVietnamese = def.meaningVietnamese,
                            synonyms = def.synonyms,
                            antonyms = def.antonyms,
                            isDefaultSelected = true
                        )
                    }
                    _selectionItems.update { items }
                    _showSelectionDialog.update { true }
                    _uiState.update { AddWordUiState.Idle }
                }
                .onFailure { e ->
                    _uiState.update { AddWordUiState.Error(e.message ?: "AI lookup failed") }
                }
        }
    }

    fun importSelectedDefinitions(selected: List<SelectionItem>) {
        if (selected.isEmpty()) return

        _definitions.update { currentList ->
            val newList = currentList.toMutableList()
            if (newList.size == 1 && newList[0].definitionEnglish.isBlank() && newList[0].meaningVietnamese.isBlank()) {
                newList.clear()
            }
            selected.forEach { item ->
                newList.add(
                    WordDefinition(
                        pos = item.partOfSpeech,
                        definitionEnglish = item.definition,
                        meaningVietnamese = item.meaningVietnamese,
                        exampleSentence = item.example ?: "",
                        synonyms = item.synonyms,
                        antonyms = item.antonyms
                    )
                )
            }
            newList
        }
        _showSelectionDialog.update { false }
    }

    fun playAudio() {
        if (_audioUrl.value.isNotBlank() || _wordText.value.isNotBlank()) {
            com.edu.minlish.core.util.AudioPlayer.play(_audioUrl.value, _wordText.value)
        }
    }

    fun saveWord(setId: String) {
        if (_wordText.value.isBlank() || _definitions.value.all { it.meaningVietnamese.isBlank() && it.definitionEnglish.isBlank() }) {
            _uiState.update { AddWordUiState.Error("Word and at least one meaning are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { AddWordUiState.Loading }
            val word = VocabularyWord(
                id = loadedWordId ?: "",
                vocabularySetId = setId,
                word = _wordText.value,
                pronunciation = _pronunciationText.value,
                audioUrl = _audioUrl.value,
                imageUrl = _imageUrl.value,
                definitions = _definitions.value.filter { it.meaningVietnamese.isNotBlank() || it.definitionEnglish.isNotBlank() },
                collocations = _collocationText.value,
                personalNote = _personalNoteText.value
            )

            val result = if (loadedWordId != null) {
                repository.updateWord(word)
            } else {
                repository.addWord(word)
            }

            result
                .onSuccess {
                    _uiState.update { AddWordUiState.Success }
                }
                .onFailure { e ->
                    _uiState.update { AddWordUiState.Error(e.message ?: "Failed to save word") }
                }
        }
    }

    fun resetError() {
        if (_uiState.value is AddWordUiState.Error) {
            _uiState.value = AddWordUiState.Idle
        }
    }
}

data class SelectionItem(
    val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val meaningVietnamese: String = "",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val isDefaultSelected: Boolean = false
)
