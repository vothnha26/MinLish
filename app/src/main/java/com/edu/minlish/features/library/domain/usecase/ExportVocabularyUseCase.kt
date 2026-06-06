package com.edu.minlish.features.library.domain.usecase

import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.repository.VocabularySetRepository
import com.edu.minlish.features.library.domain.repository.VocabularyWordRepository

class ExportVocabularyUseCase(
    private val setRepository: VocabularySetRepository,
    private val wordRepository: VocabularyWordRepository
) {
    suspend operator fun invoke(userId: String): Result<List<VocabularyWord>> {
        return try {
            val setsResult = setRepository.getUserSets(userId)
            if (setsResult.isFailure) return Result.failure(setsResult.exceptionOrNull()!!)

            val allWords = mutableListOf<VocabularyWord>()
            val sets = setsResult.getOrThrow()
            
            for (set in sets) {
                val wordsResult = wordRepository.getWordsBySet(set.id)
                if (wordsResult.isSuccess) {
                    allWords.addAll(wordsResult.getOrThrow())
                }
                // Nếu một set fail, chúng ta vẫn tiếp tục các set khác hoặc throw?
                // Logic hiện tại trong ViewModel là hasError = true
            }
            
            if (allWords.isEmpty()) {
                Result.failure(Exception("No words found to export"))
            } else {
                Result.success(allWords)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
