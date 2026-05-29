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

    fun saveProfile(name: String, goal: String, level: String) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = ProfileSetupUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            uiState = ProfileSetupUiState.Loading
            
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
