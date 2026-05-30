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

    var publishState by mutableStateOf<PublishUiState>(PublishUiState.Idle)
        private set

    fun publishNotification(title: String, message: String) {
        if (title.isBlank() || message.isBlank()) {
            publishState = PublishUiState.Error("Fields cannot be empty")
            return
        }

        publishState = PublishUiState.Loading
        viewModelScope.launch {
            val notification = Notification(
                title = title,
                message = message,
                createdAt = Date(),
                isSystem = true
            )
            repository.publishNotification(notification)
                .onSuccess {
                    publishState = PublishUiState.Success
                }
                .onFailure { e ->
                    publishState = PublishUiState.Error(e.message ?: "Failed to publish notification")
                }
        }
    }

    fun resetState() {
        publishState = PublishUiState.Idle
    }
}
