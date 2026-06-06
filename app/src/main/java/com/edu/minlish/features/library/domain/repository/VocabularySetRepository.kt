package com.edu.minlish.features.library.domain.repository

import com.edu.minlish.features.library.domain.model.VocabularySet

interface VocabularySetRepository {
    suspend fun createSet(set: VocabularySet): Result<Unit>
    suspend fun createSetAndGetId(set: VocabularySet): Result<String>
    suspend fun getUserSets(userId: String): Result<List<VocabularySet>>
    suspend fun getSetById(setId: String): Result<VocabularySet>
    suspend fun updateSet(set: VocabularySet): Result<Unit>
    suspend fun deleteSet(setId: String): Result<Unit>
}
