package com.edu.minlish.features.auth.domain.repository

import com.edu.minlish.features.auth.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, fullName: String): Result<User>
    suspend fun loginWithGoogle(idToken: String): Result<User>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun logout()
    fun getCurrentUser(): User?
}
