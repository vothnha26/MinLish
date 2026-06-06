package com.edu.minlish.features.library.data.repository

import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.repository.VocabularySetRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class VocabularySetRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : VocabularySetRepository {

    override suspend fun createSet(set: VocabularySet): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("vocabulary_sets")
                    .add(set)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createSetAndGetId(set: VocabularySet): Result<String> {
        return try {
            val docRef = withTimeout(10000) {
                firestore.collection("vocabulary_sets")
                    .add(set)
                    .await()
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserSets(userId: String): Result<List<VocabularySet>> {
        return try {
            val snapshot = withTimeout(10000) {
                firestore.collection("vocabulary_sets")
                    .whereEqualTo("creatorId", userId)
                    .get()
                    .await()
            }
            val sets = snapshot.documents.mapNotNull { doc ->
                doc.toObject(VocabularySet::class.java)?.copy(id = doc.id)
            }
            Result.success(sets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSetById(setId: String): Result<VocabularySet> {
        return try {
            val doc = withTimeout(10000) {
                firestore.collection("vocabulary_sets")
                    .document(setId)
                    .get()
                    .await()
            }
            val set = doc.toObject(VocabularySet::class.java)?.copy(id = doc.id)
            if (set != null) {
                Result.success(set)
            } else {
                Result.failure(Exception("Set not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSet(set: VocabularySet): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("vocabulary_sets")
                    .document(set.id)
                    .set(set)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSet(setId: String): Result<Unit> {
        return try {
            withTimeout(15000) {
                firestore.collection("vocabulary_sets")
                    .document(setId)
                    .delete()
                    .await()

                val wordsSnapshot = firestore.collection("vocabulary_words")
                    .whereEqualTo("vocabularySetId", setId)
                    .get()
                    .await()
                
                for (doc in wordsSnapshot.documents) {
                    firestore.collection("vocabulary_words")
                        .document(doc.id)
                        .delete()
                        .await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
