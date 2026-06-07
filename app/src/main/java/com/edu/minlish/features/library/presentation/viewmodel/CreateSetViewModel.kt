package com.edu.minlish.features.library.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import com.edu.minlish.features.library.domain.usecase.ManageCategoryUseCase
import com.edu.minlish.features.library.data.repository.CategoryRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class CreateSetUiState {
    object Idle : CreateSetUiState()
    object Loading : CreateSetUiState()
    object Success : CreateSetUiState()
    data class Error(val message: String) : CreateSetUiState()
}

class CreateSetViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val manageCategoryUseCase: ManageCategoryUseCase = ManageCategoryUseCase(CategoryRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateSetUiState>(CreateSetUiState.Idle)
    val uiState: StateFlow<CreateSetUiState> = _uiState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _category = MutableStateFlow("IELTS")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(listOf("IELTS", "TOEIC", "Business", "Travel", "Daily", "Custom"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private var loadedSetId: String? = null
    
    private var isInitialized = false

    init {
        loadCategories()
    }

    private fun loadCategories() {
        val currentUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            manageCategoryUseCase.getCategories(currentUser.id)
                .onSuccess { list ->
                    _categories.update {
                        (list.map { it.name } + listOf("IELTS", "TOEIC", "Business", "Travel", "Daily", "Custom")).distinct()
                    }
                }
                .onFailure {
                    _categories.update {
                        listOf("IELTS", "TOEIC", "Business", "Travel", "Daily", "Custom")
                    }
                }
        }
    }

    fun updateTitle(value: String) {
        _title.update { value }
    }

    fun updateDescription(value: String) {
        _description.update { value }
    }

    fun updateCategory(value: String) {
        _category.update { value }
    }

    fun initEditMode(setId: String) {
        if (isInitialized) return
        isInitialized = true
        loadedSetId = setId
        viewModelScope.launch {
            _uiState.update { CreateSetUiState.Loading }
            val setRepoResult: Result<VocabularySet> = repository.getSetById(setId)
            setRepoResult
                .onSuccess { set ->
                    _title.update { set.title }
                    _description.update { set.description }
                    _category.update { set.category }
                    _uiState.update { CreateSetUiState.Idle }
                }
                .onFailure { e ->
                    _uiState.update { CreateSetUiState.Error(e.message ?: "Failed to load set details") }
                }
        }
    }

    fun saveSet() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.update { CreateSetUiState.Error("User not logged in") }
            return
        }

        if (_title.value.isBlank()) {
            _uiState.update { CreateSetUiState.Error("Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { CreateSetUiState.Loading }
            val set = VocabularySet(
                id = loadedSetId ?: "",
                creatorId = currentUser.id,
                title = _title.value,
                description = _description.value,
                category = _category.value
            )

            val result: Result<Unit> = if (loadedSetId != null) {
                repository.updateSet(set)
            } else {
                repository.createSet(set)
            }

            result
                .onSuccess {
                    _uiState.update { CreateSetUiState.Success }
                }
                .onFailure { e ->
                    _uiState.update { CreateSetUiState.Error(e.message ?: "Failed to save set") }
                }
        }
    }

    fun deleteSet(onSuccess: () -> Unit) {
        val setId = loadedSetId ?: return
        viewModelScope.launch {
            _uiState.update { CreateSetUiState.Loading }
            repository.deleteSet(setId)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update { CreateSetUiState.Error(e.message ?: "Failed to delete set") }
                }
        }
    }
}
