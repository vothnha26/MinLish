package com.edu.minlish.features.library.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.library.data.exporter.VocabularyExportManager
import com.edu.minlish.features.library.data.importer.VocabularyImportManager
import com.edu.minlish.features.library.data.repository.*
import com.edu.minlish.features.library.domain.model.Category
import com.edu.minlish.features.library.domain.model.ImportVocabularyPreview
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(val items: List<LibrarySetItem>) : LibraryUiState()
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
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val getLibrarySetsUseCase: GetLibrarySetsUseCase = GetLibrarySetsUseCase(
        VocabularySetRepositoryImpl(),
        VocabularyProgressRepositoryImpl()
    ),
    private val importVocabularyUseCase: ImportVocabularyUseCase = ImportVocabularyUseCase(
        VocabularyWordRepositoryImpl()
    ),
    private val exportVocabularyUseCase: ExportVocabularyUseCase = ExportVocabularyUseCase(
        VocabularySetRepositoryImpl(),
        VocabularyWordRepositoryImpl()
    ),
    private val manageCategoryUseCase: ManageCategoryUseCase = ManageCategoryUseCase(
        CategoryRepositoryImpl()
    ),
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

    private val _categoriesList = MutableStateFlow<List<Category>>(emptyList())
    val categoriesList: StateFlow<List<Category>> = _categoriesList.asStateFlow()

    private var exportWordsList = emptyList<VocabularyWord>()

    val displayCategories: List<String>
        get() = (listOf("All") + _categoriesList.value.map { it.name } + listOf("IELTS", "Business", "Travel", "Daily", "Custom")).distinct()

    val filteredSets: StateFlow<List<VocabularySet>> = combine(
        uiState,
        searchQuery,
        selectedCategory
    ) { state, query, category ->
        if (state !is LibraryUiState.Success) return@combine emptyList()
        state.items.map { it.set }.filter { wordSet ->
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

    // Helper map for UI to get progress easily
    val progressMap: StateFlow<Map<String, Float>> = uiState.map { state ->
        if (state is LibraryUiState.Success) {
            state.items.associate { it.set.id to it.progress }
        } else {
            emptyMap()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadUserSets()
        loadCategories()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateImportSetTitle(title: String) {
        _importSetTitle.value = title
    }

    fun updateImportCategory(cat: String) {
        _importCategory.value = cat
    }

    fun loadCategories() {
        val currentUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            manageCategoryUseCase.getCategories(currentUser.id)
                .onSuccess { _categoriesList.value = it }
        }
    }

    fun addCategory(name: String) {
        val currentUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            manageCategoryUseCase.addCategory(name, currentUser.id)
                .onSuccess { loadCategories() }
        }
    }

    fun updateCategory(category: Category, newName: String) {
        viewModelScope.launch {
            manageCategoryUseCase.updateCategory(category.copy(name = newName))
                .onSuccess { loadCategories() }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            manageCategoryUseCase.deleteCategory(categoryId)
                .onSuccess { loadCategories() }
        }
    }

    fun loadUserSets() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = LibraryUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            if (_uiState.value !is LibraryUiState.Success) {
                _uiState.value = LibraryUiState.Loading
            }
            getLibrarySetsUseCase(currentUser.id)
                .onSuccess { _uiState.value = LibraryUiState.Success(it) }
                .onFailure { _uiState.value = LibraryUiState.Error(it.message ?: "Failed to load sets") }
        }
    }

    fun prepareExportAll(onReady: () -> Unit) {
        val currentUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            _exportUiState.value = ExportUiState.FetchingData
            exportVocabularyUseCase(currentUser.id)
                .onSuccess {
                    exportWordsList = it
                    onReady()
                    _exportUiState.value = ExportUiState.Idle
                }
                .onFailure { _exportUiState.value = ExportUiState.Error(it.message ?: "Failed to fetch data") }
        }
    }

    fun startExport(context: Context, uri: Uri) {
        viewModelScope.launch {
            _exportUiState.value = ExportUiState.Exporting
            exportManager.exportToCsv(context, uri, exportWordsList)
                .onSuccess {
                    _exportUiState.value = ExportUiState.Success(uri.path?.substringAfterLast('/') ?: "vocabulary.csv")
                }
                .onFailure { _exportUiState.value = ExportUiState.Error(it.message ?: "Failed to export") }
        }
    }

    fun clearExportState() {
        _exportUiState.value = ExportUiState.Idle
    }

    fun parseImportFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _importUiState.value = ImportUiState.Parsing
            importManager.parseFromUri(context, uri)
                .onSuccess { preview ->
                    if (preview.validRows.isEmpty()) {
                        _importUiState.value = ImportUiState.Error("No valid vocabulary rows found")
                        return@onSuccess
                    }
                    _importSetTitle.value = preview.fileName.substringBeforeLast('.')
                    _importUiState.value = ImportUiState.Preview(preview)
                }
                .onFailure { _importUiState.value = ImportUiState.Error(it.message ?: "Failed to parse file") }
        }
    }

    fun confirmImport(title: String, category: String) {
        val currentUser = authRepository.getCurrentUser() ?: return
        val previewState = _importUiState.value as? ImportUiState.Preview ?: return

        viewModelScope.launch {
            _importUiState.value = ImportUiState.Importing
            importVocabularyUseCase(currentUser.id, title, category, previewState.preview)
                .onSuccess {
                    _importUiState.value = ImportUiState.Success(it)
                    loadUserSets()
                }
                .onFailure { _importUiState.value = ImportUiState.Error(it.message ?: "Failed to import") }
        }
    }

    fun clearImportState() {
        _importUiState.value = ImportUiState.Idle
    }
}
