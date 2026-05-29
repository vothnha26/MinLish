package com.edu.minlish.features.auth.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.model.User
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

import com.edu.minlish.features.profilesetup.data.repository.FirestoreProfileRepositoryImpl
import com.edu.minlish.features.profilesetup.domain.repository.ProfileRepository

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User, val isSetupComplete: Boolean) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val profileRepository: ProfileRepository = FirestoreProfileRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    fun login(email: String, password: String, onNavigate: (Boolean) -> Unit) {
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            repository.login(email, password).onSuccess { user ->
                checkSetupAndNavigate(user, onNavigate)
            }.onFailure { e ->
                uiState = AuthUiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, fullName: String, onNavigate: (Boolean) -> Unit) {
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            repository.register(email, password, fullName).onSuccess { user ->
                uiState = AuthUiState.Success(user, false)
                onNavigate(false)
            }.onFailure { e ->
                uiState = AuthUiState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun googleLogin(idToken: String, onNavigate: (Boolean) -> Unit) {
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            repository.loginWithGoogle(idToken).onSuccess { user ->
                checkSetupAndNavigate(user, onNavigate)
            }.onFailure { e ->
                uiState = AuthUiState.Error(e.message ?: "Google login failed")
            }
        }
    }

    private suspend fun checkSetupAndNavigate(user: User, onNavigate: (Boolean) -> Unit) {
        try {
            val result = withTimeoutOrNull(8000) {
                profileRepository.getProfile(user.id)
            }
            
            if (result == null) {
                uiState = AuthUiState.Success(user, false)
                onNavigate(false)
                return
            }

            result.onSuccess { profile ->
                val isSetupComplete = profile != null && profile.learningGoal.isNotEmpty()
                uiState = AuthUiState.Success(user, isSetupComplete)
                onNavigate(isSetupComplete)
            }.onFailure {
                uiState = AuthUiState.Success(user, false)
                onNavigate(false)
            }
        } catch (e: Exception) {
            uiState = AuthUiState.Success(user, false)
            onNavigate(false)
        }
    }

    fun forgotPassword(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            repository.forgotPassword(email).onSuccess {
                uiState = AuthUiState.Idle
                onSuccess()
            }.onFailure { e ->
                uiState = AuthUiState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }
}
