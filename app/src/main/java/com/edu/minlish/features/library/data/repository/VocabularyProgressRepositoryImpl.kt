package com.edu.minlish.features.library.data.repository

import com.edu.minlish.core.util.AppSettings
import com.edu.minlish.features.library.domain.repository.VocabularyProgressRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class VocabularyProgressRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : VocabularyProgressRepository {

    override suspend fun getMasteredCountBySet(userId: String): Result<Map<String, Int>> {
        return try {
            val progressSnapshot = firestore.collection("user_word_progress")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val masteredThreshold = AppSettings.masteredThreshold
            
            val masteredBySet = progressSnapshot.documents.mapNotNull { doc ->
                val setId = doc.getString("setId") ?: return@mapNotNull null
                val status = doc.getString("status") ?: ""
                val interval = doc.getLong("interval") ?: 0L
                val isMastered = status == "mastered" || interval > masteredThreshold
                
                if (isMastered) setId else null
            }.groupBy { it }.mapValues { it.value.size }

            Result.success(masteredBySet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
