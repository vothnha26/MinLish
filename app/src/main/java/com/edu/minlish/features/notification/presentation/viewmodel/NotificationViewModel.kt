package com.edu.minlish.features.notification.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.notification.data.repository.FirestoreNotificationRepositoryImpl
import com.edu.minlish.features.notification.domain.model.Notification
import com.edu.minlish.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class NotificationUiState {
    object Loading : NotificationUiState()
    data class Success(val notifications: List<Notification>) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

class NotificationViewModel(
    private val repository: NotificationRepository = FirestoreNotificationRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        _uiState.update { NotificationUiState.Loading }
        viewModelScope.launch {
            repository.getNotifications()
                .onSuccess { list ->
                    _uiState.update { NotificationUiState.Success(list) }
                }
                .onFailure { e ->
                    _uiState.update { NotificationUiState.Error(e.message ?: "Failed to load notifications") }
                }
        }
    }
}
