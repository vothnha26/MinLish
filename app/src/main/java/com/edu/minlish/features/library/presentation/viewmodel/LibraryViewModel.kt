package com.edu.minlish.features.library.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    var uiState by mutableStateOf<LibraryUiState>(LibraryUiState.Loading)
        private set

    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf("All")

    var importUiState by mutableStateOf<ImportUiState>(ImportUiState.Idle)
        private set

    var exportUiState by mutableStateOf<ExportUiState>(ExportUiState.Idle)
        private set

    var importSetTitle by mutableStateOf("")

    var importCategory by mutableStateOf("Custom")

    var exportWordsList = emptyList<VocabularyWord>()

    val filteredSets: List<VocabularySet>
        get() {
            val state = uiState
            if (state !is LibraryUiState.Success) return emptyList()
            return state.sets.filter { wordSet ->
                val matchesCategory = selectedCategory == "All" || wordSet.category.equals(selectedCategory, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() || 
                        wordSet.title.contains(searchQuery, ignoreCase = true) || 
                        wordSet.description.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch
            }
        }

    var categoriesList by mutableStateOf<List<com.edu.minlish.features.library.domain.model.Category>>(emptyList())
        private set

    var progressMap by mutableStateOf<Map<String, Float>>(emptyMap())
        private set

    val displayCategories: List<String>
        get() = (listOf("All") + categoriesList.map { it.name } + listOf("IELTS", "Business", "Travel", "Daily", "Custom")).distinct()

    init {
        loadUserSets()
        loadCategories()
    }

    fun loadCategories() {
        val currentUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            repository.getCategories(currentUser.id)
                .onSuccess { categories ->
                    categoriesList = categories
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
            uiState = LibraryUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            uiState = LibraryUiState.Loading
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

                        progressMap = calculatedProgress
                        uiState = LibraryUiState.Success(sets)
                    } catch (e: Exception) {
                        progressMap = emptyMap()
                        uiState = LibraryUiState.Success(sets)
                    }
                }
                .onFailure { e ->
                    uiState = LibraryUiState.Error(e.message ?: "Failed to load sets")
                }
        }
    }

    fun prepareExportAll(onReady: () -> Unit) {
        val state = uiState
        if (state !is LibraryUiState.Success) return
        
        viewModelScope.launch {
            exportUiState = ExportUiState.FetchingData
            val allWords = mutableListOf<VocabularyWord>()
            var hasError = false
            
            for (set in state.sets) {
                repository.getWordsBySet(set.id)
                    .onSuccess { allWords.addAll(it) }
                    .onFailure { hasError = true }
            }
            
            if (hasError) {
                exportUiState = ExportUiState.Error("Failed to fetch some vocabulary data")
            } else if (allWords.isEmpty()) {
                exportUiState = ExportUiState.Error("No words found to export")
            } else {
                exportWordsList = allWords
                onReady()
                exportUiState = ExportUiState.Idle
            }
        }
    }

    fun startExport(context: Context, uri: Uri) {
        viewModelScope.launch {
            exportUiState = ExportUiState.Exporting
            exportManager.exportToCsv(context, uri, exportWordsList)
                .onSuccess {
                    exportUiState = ExportUiState.Success(uri.path?.substringAfterLast('/') ?: "vocabulary.csv")
                }
                .onFailure { e ->
                    exportUiState = ExportUiState.Error(e.message ?: "Failed to export vocabulary")
                }
        }
    }

    fun clearExportState() {
        exportUiState = ExportUiState.Idle
    }

    fun parseImportFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            importUiState = ImportUiState.Parsing
            importManager.parseFromUri(context, uri)
                .onSuccess { preview ->
                    if (preview.validRows.isEmpty()) {
                        importUiState = ImportUiState.Error(
                            preview.errors.firstOrNull()?.message ?: "No valid vocabulary rows found"
                        )
                        return@onSuccess
                    }

                    importSetTitle = preview.fileName
                        .substringBeforeLast('.', preview.fileName)
                        .replace("_", " ")
                        .replace("-", " ")
                        .trim()
                        .ifBlank { "Imported Vocabulary" }

                    importCategory = if (selectedCategory != "All") selectedCategory else "Custom"
                    importUiState = ImportUiState.Preview(preview)
                }
                .onFailure { e ->
                    importUiState = ImportUiState.Error(e.message ?: "Failed to parse import file")
                }
        }
    }

    fun confirmImport(title: String, category: String) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            importUiState = ImportUiState.Error("User not logged in")
            return
        }

        val previewState = importUiState as? ImportUiState.Preview ?: return
        val normalizedTitle = title.trim()
        val normalizedCategory = category.trim().ifBlank { "Custom" }

        if (normalizedTitle.isBlank()) {
            importUiState = ImportUiState.Error("Set title is required")
            return
        }

        viewModelScope.launch {
            importUiState = ImportUiState.Importing
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
                    importUiState = ImportUiState.Success(words.size)
                    loadUserSets()
                }
                .onFailure { e ->
                    importUiState = ImportUiState.Error(e.message ?: "Failed to import vocabulary")
                }
        }
    }

    fun clearImportState() {
        importUiState = ImportUiState.Idle
    }
}
