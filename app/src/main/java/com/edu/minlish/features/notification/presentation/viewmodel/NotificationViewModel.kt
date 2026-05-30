package com.edu.minlish.features.notification.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.notification.data.repository.FirestoreNotificationRepositoryImpl
import com.edu.minlish.features.notification.domain.model.Notification
import com.edu.minlish.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.launch

sealed class NotificationUiState {
    object Loading : NotificationUiState()
    data class Success(val notifications: List<Notification>) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

class NotificationViewModel(
    private val repository: NotificationRepository = FirestoreNotificationRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf<NotificationUiState>(NotificationUiState.Loading)
        private set

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        uiState = NotificationUiState.Loading
        viewModelScope.launch {
            repository.getNotifications()
                .onSuccess { list ->
                    uiState = NotificationUiState.Success(list)
                }
                .onFailure { e ->
                    uiState = NotificationUiState.Error(e.message ?: "Failed to load notifications")
                }
        }
    }
}
