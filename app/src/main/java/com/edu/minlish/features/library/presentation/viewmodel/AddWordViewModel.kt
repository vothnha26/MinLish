package com.edu.minlish.features.library.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.library.data.DictionaryEntry
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import com.edu.minlish.features.library.domain.repository.TranslationStrategy
import com.edu.minlish.features.library.domain.repository.CollocationStrategy
import com.edu.minlish.features.library.data.repository.GoogleTranslationStrategy
import com.edu.minlish.features.library.data.repository.DatamuseCollocationStrategy
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.edu.minlish.core.ai.AIModule
import com.edu.minlish.core.ai.model.AIAutoFillResult
import com.google.gson.Gson
import kotlinx.coroutines.flow.update

sealed class AddWordUiState {
    object Idle : AddWordUiState()
    object Loading : AddWordUiState()
    object Success : AddWordUiState()
    data class Error(val message: String) : AddWordUiState()
}

class AddWordViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl(),
    private val translationStrategy: TranslationStrategy = GoogleTranslationStrategy(),
    private val collocationStrategy: CollocationStrategy = DatamuseCollocationStrategy()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddWordUiState>(AddWordUiState.Idle)
    val uiState: StateFlow<AddWordUiState> = _uiState.asStateFlow()

    private val _loadedWordId = MutableStateFlow<String?>(null)
    val loadedWordId: StateFlow<String?> = _loadedWordId.asStateFlow()
    
    private var isInitialized = false

    fun initEditMode(wordId: String) {
        if (isInitialized) return
        isInitialized = true
        _loadedWordId.update { wordId }
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

    // Selection management for Smart Search
    private val _searchResults = MutableStateFlow<List<DictionaryEntry>>(emptyList())
    val searchResults: StateFlow<List<DictionaryEntry>> = _searchResults.asStateFlow()

    private val _selectionItems = MutableStateFlow<List<SelectionItem>>(emptyList())
    val selectionItems: StateFlow<List<SelectionItem>> = _selectionItems.asStateFlow()

    private val _showSelectionDialog = MutableStateFlow(false)
    val showSelectionDialog: StateFlow<Boolean> = _showSelectionDialog.asStateFlow()

    private val _selectedItemIndex = MutableStateFlow(-1)
    val selectedItemIndex: StateFlow<Int> = _selectedItemIndex.asStateFlow()

    fun updateWordText(value: String) { _wordText.update { value } }
    fun updatePronunciationText(value: String) { _pronunciationText.update { value } }
    fun updateAudioUrl(value: String) { _audioUrl.update { value } }
    fun updateImageUrl(value: String) { _imageUrl.update { value } }
    fun updateCollocationText(value: String) { _collocationText.update { value } }
    fun updatePersonalNoteText(value: String) { _personalNoteText.update { value } }
    fun updateShowSelectionDialog(value: Boolean) { _showSelectionDialog.update { value } }
    fun updateSelectedItemIndex(value: Int) { _selectedItemIndex.update { value } }

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
            
            // Auto Image from LoremFlickr (keyword-based image search)
            val cleanWord = _wordText.value.lowercase().trim()
            val lockSeed = Math.abs(cleanWord.hashCode())
            _imageUrl.update { "https://loremflickr.com/600/400/$cleanWord?lock=$lockSeed" }
            
            // Fetch translation and collocations in parallel with dictionary details
            val translationDeferred = async {
                translationStrategy.translate(cleanWord, "en", "vi").getOrDefault("")
            }
            
            val collocationsDeferred = async {
                collocationStrategy.fetchCollocations(cleanWord).getOrDefault(emptyList())
            }
            
            repository.fetchWordDetails(cleanWord)
                .onSuccess { entries ->
                    if (entries.isNotEmpty()) {
                        _searchResults.update { entries }
                        
                        // Await translation and collocations
                        val translationResult = translationDeferred.await()
                        val collocationsResult = collocationsDeferred.await()
                        
                        // Populate collocations text separated by comma
                        if (collocationsResult.isNotEmpty()) {
                            _collocationText.update { collocationsResult.joinToString(", ") }
                        }
                        
                        // Populate pronunciation and audio directly
                        val firstEntry = entries.firstOrNull()
                        val phonetic = firstEntry?.phonetic 
                            ?: firstEntry?.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text 
                            ?: ""
                        _pronunciationText.update { phonetic }

                        val audio = firstEntry?.phonetics?.firstOrNull { !it.audio.isNullOrBlank() }?.audio ?: ""
                        _audioUrl.update { if (audio.startsWith("//")) "https:$audio" else audio }

                        // For each part of speech, take at most 3 definitions.
                        val rawItems = mutableListOf<SelectionItem>()
                        entries.forEachIndexed { entryIdx, entry ->
                            val entryPhonetic = entry.phonetic 
                                ?: entry.phonetics.firstOrNull { !it.text.isNullOrBlank() }?.text 
                                ?: ""
                            entry.meanings.forEachIndexed { meaningIdx, meaning ->
                                // Limit definitions per meaning (part of speech) to top 3
                                meaning.definitions.take(3).forEachIndexed { defIdx, definition ->
                                    // Default selected is true only for the first definition of each meaning
                                    val isDefaultSelected = defIdx == 0
                                    
                                    val allSyns = ((definition.synonyms) + (meaning.synonyms ?: emptyList()))
                                        .distinct()
                                        .take(5)
                                        
                                    val allAnts = ((definition.antonyms) + (meaning.antonyms ?: emptyList()))
                                        .distinct()
                                        .take(5)

                                    rawItems.add(
                                        SelectionItem(
                                            entryIndex = entryIdx,
                                            meaningIndex = meaningIdx,
                                            definitionIndex = defIdx,
                                            partOfSpeech = meaning.partOfSpeech,
                                            definition = definition.definition,
                                            example = definition.example,
                                            phonetic = entryPhonetic,
                                            meaningVietnamese = translationResult, // fallback
                                            synonyms = allSyns,
                                            antonyms = allAnts,
                                            isDefaultSelected = isDefaultSelected
                                        )
                                    )
                                }
                            }
                        }

                        // Try to improve meanings with AI if available
                        try {
                            val aiResult = AIModule.geminiService.lookupWordDetail(cleanWord)
                            aiResult.onSuccess { jsonStr ->
                                val cleanJson = if (jsonStr.contains("```json")) {
                                    jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
                                } else if (jsonStr.contains("```")) {
                                    jsonStr.substringAfter("```").substringBeforeLast("```").trim()
                                } else {
                                    jsonStr
                                }
                                val aiData = Gson().fromJson(cleanJson, AIAutoFillResult::class.java)
                                if (aiData != null && aiData.definitions.isNotEmpty()) {
                                    // Map AI definitions to selection items or just add them if they are better
                                    // For simplicity and quality, if AI succeeds, we combine or prefer AI meanings
                                    val aiItems = aiData.definitions.map { aiDef ->
                                        SelectionItem(
                                            entryIndex = -1, // Mark as AI source
                                            meaningIndex = -1,
                                            definitionIndex = -1,
                                            partOfSpeech = aiDef.pos,
                                            definition = aiDef.definitionEnglish,
                                            example = aiDef.exampleSentence,
                                            phonetic = aiData.pronunciation,
                                            meaningVietnamese = aiDef.meaningVietnamese,
                                            synonyms = aiDef.synonyms,
                                            antonyms = aiDef.antonyms,
                                            isDefaultSelected = true
                                        )
                                    }
                                    // If AI provides good data, we can either replace or append. 
                                    // Let's replace the rawItems with AI items as they have specific meanings.
                                    _selectionItems.update { aiItems }
                                } else {
                                    _selectionItems.update { rawItems }
                                }
                            }.onFailure {
                                _selectionItems.update { rawItems }
                            }
                        } catch (e: Exception) {
                            _selectionItems.update { rawItems }
                        }
                        
                        _showSelectionDialog.update { true }
                        _uiState.update { AddWordUiState.Idle }
                    } else {
                        _uiState.update { AddWordUiState.Error("Word not found") }
                    }
                }
                .onFailure { e ->
                    _uiState.update { AddWordUiState.Error(e.message ?: "Failed to fetch word details") }
                }
        }
    }

    fun importSelectedDefinitions(selected: List<SelectionItem>) {
        if (selected.isEmpty()) return

        _definitions.update { currentList ->
            val newList = currentList.toMutableList()
            // 1. Clear initial empty state if needed
            if (newList.size == 1 && newList[0].definitionEnglish.isBlank() && newList[0].meaningVietnamese.isBlank()) {
                newList.clear()
            }

            // 2. Add selected definitions using data directly from SelectionItem
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
                id = _loadedWordId.value ?: "",
                vocabularySetId = setId,
                word = _wordText.value,
                pronunciation = _pronunciationText.value,
                audioUrl = _audioUrl.value,
                imageUrl = _imageUrl.value,
                definitions = _definitions.value.filter { it.meaningVietnamese.isNotBlank() || it.definitionEnglish.isNotBlank() },
                collocations = _collocationText.value,
                personalNote = _personalNoteText.value
            )

            val result = if (_loadedWordId.value != null) {
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
    val entryIndex: Int,
    val meaningIndex: Int,
    val definitionIndex: Int,
    val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val phonetic: String?,
    val meaningVietnamese: String = "",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val isDefaultSelected: Boolean = false
)
