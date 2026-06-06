package com.edu.minlish.features.library.domain.repository

interface VocabularyProgressRepository {
    /**
     * Trả về Map chứa tiến độ hoàn thành của từng bộ từ vựng (SetId -> MasteredCount)
     */
    suspend fun getMasteredCountBySet(userId: String): Result<Map<String, Int>>
}
