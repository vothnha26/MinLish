package com.edu.minlish.features.library.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.library.data.repository.FirestoreVocabularyRepositoryImpl
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import kotlinx.coroutines.launch

sealed class CreateSetUiState {
    object Idle : CreateSetUiState()
    object Loading : CreateSetUiState()
    object Success : CreateSetUiState()
    data class Error(val message: String) : CreateSetUiState()
}

class CreateSetViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf<CreateSetUiState>(CreateSetUiState.Idle)
        private set

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var category by mutableStateOf("IELTS")

    var loadedSetId by mutableStateOf<String?>(null)
        private set
    private var isInitialized = false

    fun initEditMode(setId: String) {
        if (isInitialized) return
        isInitialized = true
        loadedSetId = setId
        viewModelScope.launch {
            uiState = CreateSetUiState.Loading
            val setRepoResult: Result<VocabularySet> = repository.getSetById(setId)
            setRepoResult
                .onSuccess { set ->
                    title = set.title
                    description = set.description
                    category = set.category
                    uiState = CreateSetUiState.Idle
                }
                .onFailure { e ->
                    uiState = CreateSetUiState.Error(e.message ?: "Failed to load set details")
                }
        }
    }

    fun saveSet() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = CreateSetUiState.Error("User not logged in")
            return
        }

        if (title.isBlank()) {
            uiState = CreateSetUiState.Error("Title cannot be empty")
            return
        }

        viewModelScope.launch {
            uiState = CreateSetUiState.Loading
            val set = VocabularySet(
                id = loadedSetId ?: "",
                creatorId = currentUser.id,
                title = title,
                description = description,
                category = category
            )

            val result: Result<Unit> = if (loadedSetId != null) {
                repository.updateSet(set)
            } else {
                repository.createSet(set)
            }

            result
                .onSuccess {
                    uiState = CreateSetUiState.Success
                }
                .onFailure { e ->
                    uiState = CreateSetUiState.Error(e.message ?: "Failed to save set")
                }
        }
    }

    fun deleteSet(onSuccess: () -> Unit) {
        val setId = loadedSetId ?: return
        viewModelScope.launch {
            uiState = CreateSetUiState.Loading
            repository.deleteSet(setId)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { e ->
                    uiState = CreateSetUiState.Error(e.message ?: "Failed to delete set")
                }
        }
    }
}
