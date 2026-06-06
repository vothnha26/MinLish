package com.edu.minlish.features.profilesetup.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.model.User
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.profilesetup.data.repository.FirestoreProfileRepositoryImpl
import com.edu.minlish.features.profilesetup.domain.model.UserProfile
import com.edu.minlish.features.profilesetup.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: User, val profile: UserProfile?) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository = FirestoreProfileRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = ProfileUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            if (_uiState.value !is ProfileUiState.Success) {
                _uiState.value = ProfileUiState.Loading
            }
            
            // 1. Fetch Full Name from Firestore to ensure it's up to date
            val fetchedNameResult = authRepository.fetchUserFullName(currentUser.id)
            val fetchedName = fetchedNameResult.getOrNull()
            val updatedUser = currentUser.copy(fullName = fetchedName ?: currentUser.fullName)

            // 2. Fetch User Profile
            profileRepository.getProfile(currentUser.id)
                .onSuccess { profile ->
                    _uiState.value = ProfileUiState.Success(updatedUser, profile)
                }
                .onFailure { e ->
                    _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
                }
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogoutSuccess()
        }
    }
}
