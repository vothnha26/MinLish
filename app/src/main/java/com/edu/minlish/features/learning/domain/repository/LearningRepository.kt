package com.edu.minlish.features.learning.domain.repository

import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.edu.minlish.features.library.domain.model.VocabularyWord

interface LearningRepository {
    suspend fun getDueWords(userId: String, setId: String? = null, forceAll: Boolean = false): Result<List<Pair<VocabularyWord, UserWordProgress?>>>
    suspend fun updateProgress(progress: UserWordProgress): Result<Unit>
    suspend fun initializeProgress(userId: String, wordId: String, setId: String): Result<UserWordProgress>
}
