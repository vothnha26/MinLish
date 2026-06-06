package com.edu.minlish.features.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.model.User
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Login & Register Form State
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _showPassword = MutableStateFlow(false)
    val showPassword: StateFlow<Boolean> = _showPassword.asStateFlow()

    private val _showConfirmPassword = MutableStateFlow(false)
    val showConfirmPassword: StateFlow<Boolean> = _showConfirmPassword.asStateFlow()

    private val _agreedToTerms = MutableStateFlow(false)
    val agreedToTerms: StateFlow<Boolean> = _agreedToTerms.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private val _confirmPasswordError = MutableStateFlow<String?>(null)
    val confirmPasswordError: StateFlow<String?> = _confirmPasswordError.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _attempted = MutableStateFlow(false)
    val attempted: StateFlow<Boolean> = _attempted.asStateFlow()

    // Update methods
    fun updateEmail(value: String) {
        _email.value = value
        if (_attempted.value) validateLogin()
    }

    fun updatePassword(value: String) {
        _password.value = value
        if (_attempted.value) validateLogin()
    }

    fun updateFullName(value: String) {
        _fullName.value = value
    }

    fun updateConfirmPassword(value: String) {
        _confirmPassword.value = value
    }

    fun toggleShowPassword() {
        _showPassword.value = !_showPassword.value
    }

    fun toggleShowConfirmPassword() {
        _showConfirmPassword.value = !_showConfirmPassword.value
    }

    fun updateAgreedToTerms(value: Boolean) {
        _agreedToTerms.value = value
    }

    private fun validateLogin(): Boolean {
        var isValid = true
        if (!_email.value.contains("@")) {
            _emailError.value = "Please enter a valid email address."
            isValid = false
        } else {
            _emailError.value = null
        }

        if (_password.value.length < 6) {
            _passwordError.value = "Password must be at least 6 characters."
            isValid = false
        } else {
            _passwordError.value = null
        }
        return isValid
    }

    fun login(onNavigate: (Boolean) -> Unit) {
        _attempted.value = true
        if (!validateLogin()) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.login(_email.value, _password.value).onSuccess { user ->
                checkSetupAndNavigate(user, onNavigate)
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(onNavigate: (Boolean) -> Unit) {
        _attempted.value = true
        // Basic validation for Register
        if (_fullName.value.isBlank() || _email.value.isBlank() || _password.value.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields.")
            return
        }
        if (_password.value != _confirmPassword.value) {
            _uiState.value = AuthUiState.Error("Passwords do not match.")
            return
        }
        if (!_agreedToTerms.value) {
            _uiState.value = AuthUiState.Error("You must agree to the Terms and Privacy Policy.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.register(_email.value, _password.value, _fullName.value).onSuccess { user ->
                _uiState.value = AuthUiState.Success(user, false)
                onNavigate(false)
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun googleLogin(idToken: String, onNavigate: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.loginWithGoogle(idToken).onSuccess { user ->
                checkSetupAndNavigate(user, onNavigate)
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.message ?: "Google login failed")
            }
        }
    }

    private suspend fun checkSetupAndNavigate(user: User, onNavigate: (Boolean) -> Unit) {
        try {
            val result = withTimeoutOrNull(8000) {
                profileRepository.getProfile(user.id)
            }
            
            if (result == null) {
                _uiState.value = AuthUiState.Success(user, false)
                onNavigate(false)
                return
            }

            result.onSuccess { profile ->
                val isSetupComplete = profile != null && profile.learningGoal.isNotEmpty()
                if (isSetupComplete) {
                    viewModelScope.launch {
                        try {
                            com.edu.minlish.core.util.SessionDataManager.preFetchUserData(user.id)
                        } catch (e: Exception) {
                            // Bỏ qua lỗi pre-fetch để không gián đoạn chuyển hướng
                        }
                    }
                }
                _uiState.value = AuthUiState.Success(user, isSetupComplete)
                onNavigate(isSetupComplete)
            }.onFailure {
                _uiState.value = AuthUiState.Success(user, false)
                onNavigate(false)
            }
        } catch (e: Exception) {
            _uiState.value = AuthUiState.Success(user, false)
            onNavigate(false)
        }
    }

    fun forgotPassword(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.forgotPassword(email).onSuccess {
                _uiState.value = AuthUiState.Idle
                onSuccess()
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }
}
