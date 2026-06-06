package com.edu.minlish.features.learning.domain.usecase

import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.edu.minlish.features.learning.domain.repository.LearningRepository
import java.util.Date

/**
 * UseCase: Tính toán SM-2 và lưu progress lên Firestore.
 *
 * SM-2 Algorithm:
 *  - Trả lời đúng: tăng repetition, tính interval mới theo ease factor.
 *  - Trả lời sai: reset repetition về 0, interval = 1 ngày, giảm ease factor.
 */
class UpdateWordProgressUseCase(
    private val repository: LearningRepository
) {
    companion object {
        private const val ONE_DAY_MS = 24L * 60 * 60 * 1000
    }

    suspend operator fun invoke(
        existing: UserWordProgress?,
        userId: String,
        wordId: String,
        setId: String,
        correct: Boolean,
        intervalUnitMs: Long = ONE_DAY_MS,      // đọc từ AppSettings ở ngoài
        masteredThreshold: Int = 30             // đọc từ AppSettings ở ngoài
    ): Result<Unit> {
        val updated = if (existing == null) {
            com.edu.minlish.core.util.SpacedRepetitionUtil.createInitialProgress(
                userId = userId,
                wordId = wordId,
                setId = setId,
                correct = correct,
                intervalUnitMs = intervalUnitMs
            )
        } else {
            com.edu.minlish.core.util.SpacedRepetitionUtil.calculateSM2ForBinary(
                current = existing,
                correct = correct,
                intervalUnitMs = intervalUnitMs,
                masteredThreshold = masteredThreshold
            )
        }

        return repository.updateProgress(updated)
    }
}
