package com.edu.minlish.features.auth.data

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/v1/auth/google-login")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): AuthResponse

    @retrofit2.http.POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@retrofit2.http.Query("email") email: String): Map<String, String>
}

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class GoogleLoginRequest(
    val idToken: String
)

data class AuthResponse(
    val email: String,
    val fullName: String?,
    val token: String?,
    val message: String
)
