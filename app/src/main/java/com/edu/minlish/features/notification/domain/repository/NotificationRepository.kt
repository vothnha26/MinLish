package com.edu.minlish.features.notification.domain.repository

import com.edu.minlish.features.notification.domain.model.Notification

interface NotificationRepository {
    suspend fun getNotifications(): Result<List<Notification>>
    suspend fun publishNotification(notification: Notification): Result<Unit>
}
