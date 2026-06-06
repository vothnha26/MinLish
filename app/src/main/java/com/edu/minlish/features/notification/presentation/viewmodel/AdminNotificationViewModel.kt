package com.edu.minlish.features.notification.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.notification.data.repository.FirestoreNotificationRepositoryImpl
import com.edu.minlish.features.notification.domain.model.Notification
import com.edu.minlish.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

sealed class PublishUiState {
    object Idle : PublishUiState()
    object Loading : PublishUiState()
    object Success : PublishUiState()
    data class Error(val message: String) : PublishUiState()
}

class AdminNotificationViewModel(
    private val repository: NotificationRepository = FirestoreNotificationRepositoryImpl()
) : ViewModel() {

    private val _publishState = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val publishState: StateFlow<PublishUiState> = _publishState.asStateFlow()

    fun publishNotification(title: String, message: String) {
        if (title.isBlank() || message.isBlank()) {
            _publishState.value = PublishUiState.Error("Fields cannot be empty")
            return
        }

        _publishState.value = PublishUiState.Loading
        viewModelScope.launch {
            val notification = Notification(
                title = title,
                message = message,
                createdAt = Date(),
                isSystem = true
            )
            repository.publishNotification(notification)
                .onSuccess {
                    _publishState.value = PublishUiState.Success
                }
                .onFailure { e ->
                    _publishState.value = PublishUiState.Error(e.message ?: "Failed to publish notification")
                }
        }
    }

    fun resetState() {
        _publishState.value = PublishUiState.Idle
    }
}
