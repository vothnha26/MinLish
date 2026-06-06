package com.edu.minlish.features.profilesetup.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.profilesetup.data.repository.FirestoreProfileRepositoryImpl
import com.edu.minlish.features.profilesetup.domain.model.UserLearningStats
import com.edu.minlish.features.profilesetup.domain.model.UserProfile
import com.edu.minlish.features.profilesetup.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _uiState = MutableStateFlow<ProfileSetupUiState>(ProfileSetupUiState.Idle)
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _selectedGoal = MutableStateFlow("ielts")
    val selectedGoal: StateFlow<String> = _selectedGoal.asStateFlow()

    private val _selectedLevel = MutableStateFlow("B1")
    val selectedLevel: StateFlow<String> = _selectedLevel.asStateFlow()

    private val _step = MutableStateFlow(1)
    val step: StateFlow<Int> = _step.asStateFlow()

    fun updateStep(value: Int) {
        _step.value = value
    }

    fun nextStep() {
        if (_step.value < 3) _step.value += 1
    }

    fun previousStep() {
        if (_step.value > 1) _step.value -= 1
    }

    fun updateName(value: String) {
        _name.value = value
    }

    fun updateSelectedGoal(value: String) {
        _selectedGoal.value = value
    }

    fun updateSelectedLevel(value: String) {
        _selectedLevel.value = value
    }

    fun loadExistingProfile() {
        val currentUser = authRepository.getCurrentUser() ?: return
        _name.value = currentUser.fullName ?: ""
        
        viewModelScope.launch {
            _uiState.value = ProfileSetupUiState.Loading
            profileRepository.getProfile(currentUser.id)
                .onSuccess { profile ->
                    if (profile != null) {
                        _selectedGoal.value = profile.learningGoal
                        _selectedLevel.value = profile.currentLevel
                    }
                    _uiState.value = ProfileSetupUiState.Idle
                }
                .onFailure { e ->
                    _uiState.value = ProfileSetupUiState.Error(e.message ?: "Failed to load profile for editing")
                }
        }
    }

    fun saveProfile(name: String, goal: String, level: String) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = ProfileSetupUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileSetupUiState.Loading
            
            // 1. Update full name in Auth and users collection
            authRepository.updateProfile(name)
                .onFailure { e ->
                    _uiState.value = ProfileSetupUiState.Error(e.message ?: "Failed to update profile name")
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
                    _uiState.value = ProfileSetupUiState.Success
                }
                .onFailure { e ->
                    _uiState.value = ProfileSetupUiState.Error(e.message ?: "Failed to save profile")
                }
        }
    }
}
