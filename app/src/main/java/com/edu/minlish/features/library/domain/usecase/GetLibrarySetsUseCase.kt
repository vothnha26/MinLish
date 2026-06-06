package com.edu.minlish.features.library.domain.usecase

import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.repository.VocabularyProgressRepository
import com.edu.minlish.features.library.domain.repository.VocabularySetRepository

data class LibrarySetItem(
    val set: VocabularySet,
    val progress: Float
)

class GetLibrarySetsUseCase(
    private val setRepository: VocabularySetRepository,
    private val progressRepository: VocabularyProgressRepository
) {
    suspend operator fun invoke(userId: String): Result<List<LibrarySetItem>> {
        return try {
            val setsResult = setRepository.getUserSets(userId)
            val masteredCountResult = progressRepository.getMasteredCountBySet(userId)

            if (setsResult.isSuccess && masteredCountResult.isSuccess) {
                val sets = setsResult.getOrThrow()
                val masteredCounts = masteredCountResult.getOrThrow()

                val items = sets.map { set ->
                    val total = set.wordCount.toFloat()
                    val mastered = (masteredCounts[set.id] ?: 0).toFloat()
                    val pct = if (total > 0) mastered / total else 0.0f
                    LibrarySetItem(set, pct)
                }
                Result.success(items)
            } else {
                Result.failure(setsResult.exceptionOrNull() ?: masteredCountResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
