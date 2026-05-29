package com.edu.minlish.features.profilesetup.data.repository

import com.edu.minlish.features.profilesetup.domain.model.UserLearningStats
import com.edu.minlish.features.profilesetup.domain.model.UserProfile
import com.edu.minlish.features.profilesetup.domain.repository.ProfileRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class FirestoreProfileRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProfileRepository {

    override suspend fun completeProfileSetup(
        profile: UserProfile,
        stats: UserLearningStats
    ): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            // 1. Save Profile
            val profileRef = firestore.collection("profiles").document(profile.userId)
            batch.set(profileRef, profile)
            
            // 2. Initialize Stats
            val statsRef = firestore.collection("stats").document(stats.userId)
            batch.set(statsRef, stats)
            
            // 3. Update User meta
            val userRef = firestore.collection("users").document(profile.userId)
            batch.set(userRef, mapOf("hasCompletedSetup" to true), SetOptions.merge())
            
            withTimeout(10000) { // Tăng lên 10s cho chắc chắn
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProfile(userId: String): Result<UserProfile?> {
        return try {
            val snapshot = withTimeout(8000) {
                firestore.collection("profiles").document(userId).get().await()
            }
            if (snapshot.exists()) {
                Result.success(snapshot.toObject(UserProfile::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
