package com.edu.minlish.features.library.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.library.data.importer.VocabularyImportManager
import com.edu.minlish.features.library.domain.model.ImportVocabularyPreview
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import java.util.Date
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update

import com.edu.minlish.features.library.data.exporter.VocabularyExportManager

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(val sets: List<VocabularySet>) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}

sealed class ImportUiState {
    object Idle : ImportUiState()
    object Parsing : ImportUiState()
    data class Preview(val preview: ImportVocabularyPreview) : ImportUiState()
    object Importing : ImportUiState()
    data class Success(val importedCount: Int) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}

class LibraryViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val importManager: VocabularyImportManager = VocabularyImportManager(),
    private val exportManager: VocabularyExportManager = VocabularyExportManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _importUiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importUiState: StateFlow<ImportUiState> = _importUiState.asStateFlow()

    private val _exportUiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportUiState: StateFlow<ExportUiState> = _exportUiState.asStateFlow()

    private val _importSetTitle = MutableStateFlow("")
    val importSetTitle: StateFlow<String> = _importSetTitle.asStateFlow()

    private val _importCategory = MutableStateFlow("Custom")
    val importCategory: StateFlow<String> = _importCategory.asStateFlow()

    var exportWordsList = emptyList<VocabularyWord>()

    val filteredSets: StateFlow<List<VocabularySet>> = combine(
        uiState,
        searchQuery,
        selectedCategory
    ) { state, query, category ->
        if (state !is LibraryUiState.Success) return@combine emptyList()
        state.sets.filter { wordSet ->
            val matchesCategory = category == "All" || wordSet.category.equals(category, ignoreCase = true)
            val matchesSearch = query.isBlank() || 
                    wordSet.title.contains(query, ignoreCase = true) || 
                    wordSet.description.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _categoriesList = MutableStateFlow<List<com.edu.minlish.features.library.domain.model.Category>>(emptyList())
    val categoriesList: StateFlow<List<com.edu.minlish.features.library.domain.model.Category>> = _categoriesList.asStateFlow()

    private val _progressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progressMap: StateFlow<Map<String, Float>> = _progressMap.asStateFlow()

    val displayCategories: List<String>
        get() = (listOf("All") + _categoriesList.value.map { it.name } + listOf("IELTS", "Business", "Travel", "Daily", "Custom")).distinct()

    init {
        loadUserSets()
        loadCategories()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.update { query }
    }

    fun updateSelectedCategory(category: String) {
        _selectedCategory.update { category }
    }

    fun updateImportSetTitle(title: String) {
        _importSetTitle.update { title }
    }

    fun updateImportCategory(cat: String) {
        _importCategory.update { cat }
    }

    fun loadCategories() {
        val currentUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            repository.getCategories(currentUser.id)
                .onSuccess { categories ->
                    _categoriesList.update { categories }
                }
        }
    }

    fun addCategory(name: String) {
        val currentUser = authRepository.getCurrentUser() ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val cat = com.edu.minlish.features.library.domain.model.Category(
                name = name,
                creatorId = currentUser.id
            )
            repository.addCategory(cat).onSuccess {
                loadCategories()
            }
        }
    }

    fun updateCategory(category: com.edu.minlish.features.library.domain.model.Category, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateCategory(category.copy(name = newName)).onSuccess {
                loadCategories()
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId).onSuccess {
                loadCategories()
            }
        }
    }

    fun loadUserSets() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.update { LibraryUiState.Error("User not logged in") }
            return
        }

        viewModelScope.launch {
            if (_uiState.value !is LibraryUiState.Success) {
                _uiState.update { LibraryUiState.Loading }
            }
            repository.getUserSets(currentUser.id)
                .onSuccess { sets ->
                    try {
                        val progressSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("user_word_progress")
                            .whereEqualTo("userId", currentUser.id)
                            .get()
                            .await()

                        val masteredThreshold = com.edu.minlish.core.util.AppSettings.masteredThreshold
                        val list = progressSnapshot.documents.mapNotNull { doc ->
                            val setId = doc.getString("setId") ?: return@mapNotNull null
                            val status = doc.getString("status") ?: ""
                            val interval = doc.getLong("interval") ?: 0L
                            val isMastered = status == "mastered" || interval > masteredThreshold
                            setId to isMastered
                        }

                        val masteredBySet = list.filter { it.second }.groupBy { it.first }.mapValues { it.value.size }

                        val calculatedProgress = sets.associate { set ->
                            val total = set.wordCount.toFloat()
                            val mastered = (masteredBySet[set.id] ?: 0).toFloat()
                            val pct = if (total > 0) mastered / total else 0.0f
                            set.id to pct
                        }

                        _progressMap.update { calculatedProgress }
                        _uiState.update { LibraryUiState.Success(sets) }
                    } catch (e: Exception) {
                        _progressMap.update { emptyMap() }
                        _uiState.update { LibraryUiState.Success(sets) }
                    }
                }
                .onFailure { e ->
                    _uiState.update { LibraryUiState.Error(e.message ?: "Failed to load sets") }
                }
        }
    }

    fun prepareExportAll(onReady: () -> Unit) {
        val state = _uiState.value
        if (state !is LibraryUiState.Success) return
        
        viewModelScope.launch {
            _exportUiState.update { ExportUiState.FetchingData }
            val allWords = mutableListOf<VocabularyWord>()
            var hasError = false
            
            for (set in state.sets) {
                repository.getWordsBySet(set.id)
                    .onSuccess { allWords.addAll(it) }
                    .onFailure { hasError = true }
            }
            
            if (hasError) {
                _exportUiState.update { ExportUiState.Error("Failed to fetch some vocabulary data") }
            } else if (allWords.isEmpty()) {
                _exportUiState.update { ExportUiState.Error("No words found to export") }
            } else {
                exportWordsList = allWords
                onReady()
                _exportUiState.update { ExportUiState.Idle }
            }
        }
    }

    fun startExport(context: Context, uri: Uri) {
        viewModelScope.launch {
            _exportUiState.update { ExportUiState.Exporting }
            exportManager.exportToCsv(context, uri, exportWordsList)
                .onSuccess {
                    _exportUiState.update { ExportUiState.Success(uri.path?.substringAfterLast('/') ?: "vocabulary.csv") }
                }
                .onFailure { e ->
                    _exportUiState.update { ExportUiState.Error(e.message ?: "Failed to export vocabulary") }
                }
        }
    }

    fun clearExportState() {
        _exportUiState.update { ExportUiState.Idle }
    }

    fun parseImportFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _importUiState.update { ImportUiState.Parsing }
            importManager.parseFromUri(context, uri)
                .onSuccess { preview ->
                    if (preview.validRows.isEmpty()) {
                        _importUiState.update { ImportUiState.Error(
                            preview.errors.firstOrNull()?.message ?: "No valid vocabulary rows found"
                        ) }
                        return@onSuccess
                    }

                    _importSetTitle.update { 
                        preview.fileName
                            .substringBeforeLast('.', preview.fileName)
                            .replace("_", " ")
                            .replace("-", " ")
                            .trim()
                            .ifBlank { "Imported Vocabulary" }
                    }

                    _importCategory.update { if (_selectedCategory.value != "All") _selectedCategory.value else "Custom" }
                    _importUiState.update { ImportUiState.Preview(preview) }
                }
                .onFailure { e ->
                    _importUiState.update { ImportUiState.Error(e.message ?: "Failed to parse import file") }
                }
        }
    }

    fun confirmImport(title: String, category: String) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _importUiState.update { ImportUiState.Error("User not logged in") }
            return
        }

        val previewState = _importUiState.value as? ImportUiState.Preview ?: return
        val normalizedTitle = title.trim()
        val normalizedCategory = category.trim().ifBlank { "Custom" }

        if (normalizedTitle.isBlank()) {
            _importUiState.update { ImportUiState.Error("Set title is required") }
            return
        }

        viewModelScope.launch {
            _importUiState.update { ImportUiState.Importing }
            val importedAt = Date()
            val set = VocabularySet(
                creatorId = currentUser.id,
                title = normalizedTitle,
                description = "Imported from ${previewState.preview.fileName}",
                category = normalizedCategory,
                wordCount = previewState.preview.validRows.size,
                createdAt = importedAt
            )
            val words = previewState.preview.validRows.map { row ->
                VocabularyWord(
                    word = row.word,
                    pronunciation = row.pronunciation,
                    definitions = listOf(
                        WordDefinition(
                            pos = row.pos,
                            meaningVietnamese = row.meaningVietnamese,
                            definitionEnglish = row.definitionEnglish,
                            exampleSentence = row.exampleSentence,
                            synonyms = row.synonyms,
                            antonyms = row.antonyms
                        )
                    ),
                    collocations = row.collocations,
                    personalNote = row.personalNote,
                    createdAt = importedAt
                )
            }

            repository.importWords(set, words)
                .onSuccess {
                    _importUiState.update { ImportUiState.Success(words.size) }
                    loadUserSets()
                }
                .onFailure { e ->
                    _importUiState.update { ImportUiState.Error(e.message ?: "Failed to import vocabulary") }
                }
        }
    }

    fun clearImportState() {
        _importUiState.update { ImportUiState.Idle }
    }
}
