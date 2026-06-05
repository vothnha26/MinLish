package com.edu.minlish.features.profilesetup.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.model.User
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.profilesetup.data.repository.FirestoreProfileRepositoryImpl
import com.edu.minlish.features.profilesetup.domain.model.UserProfile
import com.edu.minlish.features.profilesetup.domain.repository.ProfileRepository
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

    var uiState by mutableStateOf<ProfileUiState>(ProfileUiState.Loading)
        private set

    init {
        loadProfile()
    }

    fun loadProfile() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = ProfileUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            if (uiState !is ProfileUiState.Success) {
                uiState = ProfileUiState.Loading
            }
            
            // 1. Fetch Full Name from Firestore to ensure it's up to date
            val fetchedNameResult = authRepository.fetchUserFullName(currentUser.id)
            val fetchedName = fetchedNameResult.getOrNull()
            val updatedUser = currentUser.copy(fullName = fetchedName ?: currentUser.fullName)

            // 2. Fetch User Profile
            profileRepository.getProfile(currentUser.id)
                .onSuccess { profile ->
                    uiState = ProfileUiState.Success(updatedUser, profile)
                }
                .onFailure { e ->
                    uiState = ProfileUiState.Error(e.message ?: "Failed to load profile")
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
