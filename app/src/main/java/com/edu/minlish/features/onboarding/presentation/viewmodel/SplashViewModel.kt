package com.edu.minlish.features.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.profilesetup.data.repository.FirestoreProfileRepositoryImpl
import com.edu.minlish.features.profilesetup.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashDestination {
    object Loading : SplashDestination()
    object Home : SplashDestination()
    object ProfileSetup : SplashDestination()
    object Onboarding : SplashDestination()
}

class SplashViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val profileRepository: ProfileRepository = FirestoreProfileRepositoryImpl()
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    fun checkDestination() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                profileRepository.getProfile(currentUser.id)
                    .onSuccess { profile ->
                        val isSetupComplete = profile != null && profile.learningGoal.isNotEmpty()
                        if (isSetupComplete) {
                            _destination.value = SplashDestination.Home
                        } else {
                            _destination.value = SplashDestination.ProfileSetup
                        }
                    }
                    .onFailure {
                        _destination.value = SplashDestination.Home // Mặc định vào Home khi có lỗi kết nối nhưng phiên Auth vẫn khả dụng
                    }
            } else {
                _destination.value = SplashDestination.Onboarding
            }
        }
    }
}
