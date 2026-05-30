package com.edu.minlish.features.notification.domain.model

import java.util.Date

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Date = Date(),
    val isSystem: Boolean = true
)
