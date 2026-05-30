package com.edu.minlish.features.learning.domain.repository

import com.edu.minlish.features.learning.domain.model.UserReviewLog
import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.edu.minlish.features.library.domain.model.VocabularyWord
import java.util.Date

interface LearningRepository {
    suspend fun getDueWords(userId: String, setId: String? = null, forceAll: Boolean = false): Result<List<Pair<VocabularyWord, UserWordProgress?>>>
    suspend fun updateProgress(progress: UserWordProgress): Result<Unit>
    suspend fun initializeProgress(userId: String, wordId: String, setId: String): Result<UserWordProgress>
    
    suspend fun getDailySessionWords(
        userId: String,
        targetNew: Int,
        targetReview: Int,
        setId: String? = null
    ): Result<List<Pair<VocabularyWord, UserWordProgress?>>>

    suspend fun logReview(log: UserReviewLog): Result<Unit>

    suspend fun getReviewLogsForDate(userId: String, date: Date): Result<List<UserReviewLog>>
}
