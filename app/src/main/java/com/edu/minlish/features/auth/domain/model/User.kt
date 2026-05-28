package com.edu.minlish.features.auth.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String?,
    val photoUrl: String? = null
)
