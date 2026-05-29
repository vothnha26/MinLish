package com.edu.minlish.features.auth.data.repository

import com.edu.minlish.features.auth.domain.model.User
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseAuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("User is null"))
            Result.success(mapFirebaseUser(firebaseUser))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String, fullName: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("User is null"))
            
            // Update profile with full name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Initialize user in Firestore
            val user = mapFirebaseUser(firebaseUser, fullName)
            syncUserToFirestore(user)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("User is null"))
            
            val user = mapFirebaseUser(firebaseUser)
            syncUserToFirestore(user)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncUserToFirestore(user: User) {
        try {
            withTimeoutOrNull(5000) {
                val userMap = mapOf(
                    "email" to user.email,
                    "username" to (user.fullName ?: ""),
                    "authProvider" to if (firebaseAuth.currentUser?.providerData?.any { it.providerId == "google.com" } == true) "google" else "email",
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "photoUrl" to user.photoUrl
                )
                firestore.collection("users").document(user.id)
                    .set(userMap, SetOptions.merge())
                    .await()
            }
        } catch (e: Exception) {
            // Log error but don't fail auth
            println("DEBUG: Firestore sync failed: ${e.message}")
        }
    }

    private fun mapFirebaseUser(firebaseUser: com.google.firebase.auth.FirebaseUser, fullNameOverride: String? = null): User {
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            fullName = fullNameOverride ?: firebaseUser.displayName,
            photoUrl = firebaseUser.photoUrl?.toString()
        )
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return mapFirebaseUser(firebaseUser)
    }
}
