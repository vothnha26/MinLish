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
import kotlinx.coroutines.tasks.await

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(val sets: List<VocabularySet>) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}

class LibraryViewModel(
    private val repository: VocabularyRepository = FirestoreVocabularyRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf<LibraryUiState>(LibraryUiState.Loading)
        private set

    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf("All")

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
}
