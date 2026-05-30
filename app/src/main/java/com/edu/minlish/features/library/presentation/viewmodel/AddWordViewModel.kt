package com.edu.minlish.features.library.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    var uiState by mutableStateOf<AddWordUiState>(AddWordUiState.Idle)
        private set

    var loadedWordId by mutableStateOf<String?>(null)
    private var isInitialized = false

    fun initEditMode(wordId: String) {
        if (isInitialized) return
        isInitialized = true
        loadedWordId = wordId
        viewModelScope.launch {
            uiState = AddWordUiState.Loading
            repository.getWordById(wordId)
                .onSuccess { word ->
                    wordText = word.word
                    pronunciationText = word.pronunciation
                    audioUrl = word.audioUrl
                    imageUrl = word.imageUrl
                    collocationText = word.collocations
                    personalNoteText = word.personalNote
                    definitions.clear()
                    definitions.addAll(word.definitions)
                    if (definitions.isEmpty()) {
                        definitions.add(WordDefinition())
                    }
                    uiState = AddWordUiState.Idle
                }
                .onFailure { e ->
                    uiState = AddWordUiState.Error(e.message ?: "Failed to load vocabulary for editing")
                }
        }
    }

    var wordText by mutableStateOf("")
    var pronunciationText by mutableStateOf("")
    var audioUrl by mutableStateOf("")
    var imageUrl by mutableStateOf("")
    var collocationText by mutableStateOf("")
    var personalNoteText by mutableStateOf("")

    // List of dynamic definitions
    var definitions = mutableStateListOf<WordDefinition>(WordDefinition())

    // Selection management for Smart Search
    var searchResults by mutableStateOf<List<DictionaryEntry>>(emptyList())
    var selectionItems by mutableStateOf<List<SelectionItem>>(emptyList())
    var showSelectionDialog by mutableStateOf(false)
    var selectedItemIndex by mutableStateOf(-1)

    fun addDefinitionField() {
        definitions.add(WordDefinition())
    }

    fun removeDefinitionField(index: Int) {
        if (definitions.size > 1) {
            definitions.removeAt(index)
        }
    }

    fun updateDefinition(index: Int, updated: WordDefinition) {
        definitions[index] = updated
    }

    fun smartSearch() {
        if (wordText.isBlank()) return

        viewModelScope.launch {
            uiState = AddWordUiState.Loading
            
            // Auto Image from LoremFlickr (keyword-based image search)
            val cleanWord = wordText.lowercase().trim()
            val lockSeed = Math.abs(cleanWord.hashCode())
            imageUrl = "https://loremflickr.com/600/400/$cleanWord?lock=$lockSeed"
            
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
                        searchResults = entries
                        
                        // Await translation and collocations
                        val translationResult = translationDeferred.await()
                        val collocationsResult = collocationsDeferred.await()
                        
                        // Populate collocations text separated by comma
                        if (collocationsResult.isNotEmpty()) {
                            collocationText = collocationsResult.joinToString(", ")
                        }
                        
                        // Populate pronunciation and audio directly
                        val firstEntry = entries.firstOrNull()
                        val phonetic = firstEntry?.phonetic 
                            ?: firstEntry?.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text 
                            ?: ""
                        pronunciationText = phonetic

                        val audio = firstEntry?.phonetics?.firstOrNull { !it.audio.isNullOrBlank() }?.audio ?: ""
                        audioUrl = if (audio.startsWith("//")) "https:$audio" else audio
                        
                        // We will collect definitions we want to display.
                        // For each part of speech, we take at most 3 definitions.
                        
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
                                    
                                    rawItems.add(
                                        SelectionItem(
                                            entryIndex = entryIdx,
                                            meaningIndex = meaningIdx,
                                            definitionIndex = defIdx,
                                            partOfSpeech = meaning.partOfSpeech,
                                            definition = definition.definition,
                                            example = definition.example,
                                            phonetic = entryPhonetic,
                                            meaningVietnamese = translationResult, // populated with the main word's translation
                                            isDefaultSelected = isDefaultSelected
                                        )
                                    )
                                }
                            }
                        }
                        
                        selectionItems = rawItems
                        showSelectionDialog = true
                        uiState = AddWordUiState.Idle
                    } else {
                        uiState = AddWordUiState.Error("Word not found")
                    }
                }
                .onFailure { e ->
                    uiState = AddWordUiState.Error(e.message ?: "Failed to fetch word details")
                }
        }
    }

    fun importSelectedDefinitions(selected: List<SelectionItem>) {
        if (selected.isEmpty()) return

        // 1. Update general info (Pronunciation & Audio) from the first selected item
        val firstSelected = selected.firstOrNull()
        if (firstSelected != null) {
            pronunciationText = firstSelected.phonetic ?: ""
        }
        
        // Extract first valid audio URL from searchResults
        val firstEntry = searchResults.firstOrNull()
        if (firstEntry != null) {
            val audio = firstEntry.phonetics.firstOrNull { !it.audio.isNullOrBlank() }?.audio ?: ""
            audioUrl = if (audio.startsWith("//")) "https:$audio" else audio
        }

        // 2. Clear initial empty state if needed
        if (definitions.size == 1 && definitions[0].definitionEnglish.isBlank() && definitions[0].meaningVietnamese.isBlank()) {
            definitions.clear()
        }

        // 3. Add selected definitions
        selected.forEach { item ->
            val entry = searchResults.getOrNull(item.entryIndex)
            val meaning = entry?.meanings?.getOrNull(item.meaningIndex)
            val definition = meaning?.definitions?.getOrNull(item.definitionIndex)
            
            val meaningSynonyms = meaning?.synonyms?.take(3) ?: emptyList()
            val meaningAntonyms = meaning?.antonyms?.take(3) ?: emptyList()
            
            val allSyns = ((definition?.synonyms ?: emptyList()) + meaningSynonyms).distinct().take(5)
            val allAnts = ((definition?.antonyms ?: emptyList()) + meaningAntonyms).distinct().take(5)
            
            definitions.add(
                WordDefinition(
                    pos = item.partOfSpeech,
                    definitionEnglish = item.definition,
                    meaningVietnamese = item.meaningVietnamese,
                    exampleSentence = item.example ?: "",
                    synonyms = allSyns,
                    antonyms = allAnts
                )
            )
        }
        
        showSelectionDialog = false
    }

    fun playAudio() {
        if (audioUrl.isNotBlank() || wordText.isNotBlank()) {
            com.edu.minlish.core.util.AudioPlayer.play(audioUrl, wordText)
        }
    }

    fun saveWord(setId: String) {
        if (wordText.isBlank() || definitions.all { it.meaningVietnamese.isBlank() && it.definitionEnglish.isBlank() }) {
            uiState = AddWordUiState.Error("Word and at least one meaning are required")
            return
        }

        viewModelScope.launch {
            uiState = AddWordUiState.Loading
            val word = VocabularyWord(
                id = loadedWordId ?: "",
                vocabularySetId = setId,
                word = wordText,
                pronunciation = pronunciationText,
                audioUrl = audioUrl,
                imageUrl = imageUrl,
                definitions = definitions.filter { it.meaningVietnamese.isNotBlank() || it.definitionEnglish.isNotBlank() },
                collocations = collocationText,
                personalNote = personalNoteText
            )

            val result = if (loadedWordId != null) {
                repository.updateWord(word)
            } else {
                repository.addWord(word)
            }

            result
                .onSuccess {
                    uiState = AddWordUiState.Success
                }
                .onFailure { e ->
                    uiState = AddWordUiState.Error(e.message ?: "Failed to save word")
                }
        }
    }

    fun resetError() {
        if (uiState is AddWordUiState.Error) {
            uiState = AddWordUiState.Idle
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
    val isDefaultSelected: Boolean = false
)
