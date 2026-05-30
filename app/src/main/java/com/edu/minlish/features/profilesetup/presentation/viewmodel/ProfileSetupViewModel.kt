package com.edu.minlish.features.profilesetup.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.profilesetup.data.repository.FirestoreProfileRepositoryImpl
import com.edu.minlish.features.profilesetup.domain.model.UserLearningStats
import com.edu.minlish.features.profilesetup.domain.model.UserProfile
import com.edu.minlish.features.profilesetup.domain.repository.ProfileRepository
import kotlinx.coroutines.launch

sealed class ProfileSetupUiState {
    object Idle : ProfileSetupUiState()
    object Loading : ProfileSetupUiState()
    object Success : ProfileSetupUiState()
    data class Error(val message: String) : ProfileSetupUiState()
}

class ProfileSetupViewModel(
    private val profileRepository: ProfileRepository = FirestoreProfileRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf<ProfileSetupUiState>(ProfileSetupUiState.Idle)
        private set

    var name by mutableStateOf("")
    var selectedGoal by mutableStateOf("ielts")
    var selectedLevel by mutableStateOf("B1")

    fun loadExistingProfile() {
        val currentUser = authRepository.getCurrentUser() ?: return
        name = currentUser.fullName ?: ""
        
        viewModelScope.launch {
            uiState = ProfileSetupUiState.Loading
            profileRepository.getProfile(currentUser.id)
                .onSuccess { profile ->
                    if (profile != null) {
                        selectedGoal = profile.learningGoal
                        selectedLevel = profile.currentLevel
                    }
                    uiState = ProfileSetupUiState.Idle
                }
                .onFailure { e ->
                    uiState = ProfileSetupUiState.Error(e.message ?: "Failed to load profile for editing")
                }
        }
    }

    fun saveProfile(name: String, goal: String, level: String) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = ProfileSetupUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            uiState = ProfileSetupUiState.Loading
            
            // 1. Update full name in Auth and users collection
            authRepository.updateProfile(name)
                .onFailure { e ->
                    uiState = ProfileSetupUiState.Error(e.message ?: "Failed to update profile name")
                    return@launch
                }

            // 2. Save profile settings and stats
            val profile = UserProfile(
                userId = currentUser.id,
                learningGoal = goal,
                currentLevel = level
            )
            
            val stats = UserLearningStats(
                userId = currentUser.id
            )

            profileRepository.completeProfileSetup(profile, stats)
                .onSuccess {
                    uiState = ProfileSetupUiState.Success
                }
                .onFailure { e ->
                    uiState = ProfileSetupUiState.Error(e.message ?: "Failed to save profile")
                }
        }
    }
}
