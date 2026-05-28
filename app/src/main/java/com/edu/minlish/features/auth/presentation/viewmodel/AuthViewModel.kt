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

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: AuthRepository = FirebaseAuthRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    fun login(email: String, password: String, onSuccess: (User) -> Unit) {
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            repository.login(email, password).onSuccess { user ->
                uiState = AuthUiState.Success(user)
                onSuccess(user)
            }.onFailure { e ->
                uiState = AuthUiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, fullName: String, onSuccess: (User) -> Unit) {
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            repository.register(email, password, fullName).onSuccess { user ->
                uiState = AuthUiState.Success(user)
                onSuccess(user)
            }.onFailure { e ->
                uiState = AuthUiState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun googleLogin(idToken: String, onSuccess: (User) -> Unit) {
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            repository.loginWithGoogle(idToken).onSuccess { user ->
                uiState = AuthUiState.Success(user)
                onSuccess(user)
            }.onFailure { e ->
                uiState = AuthUiState.Error(e.message ?: "Google login failed")
            }
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
