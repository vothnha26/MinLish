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
        private const val MIN_EASE_FACTOR = 1.3f
    }

    suspend operator fun invoke(
        existing: UserWordProgress?,
        userId: String,
        wordId: String,
        setId: String,
        correct: Boolean
    ): Result<Unit> {
        val now = Date()
        val updated = if (existing == null) {
            // Từ mới — khởi tạo progress
            val interval = if (correct) 1 else 1
            val repetitions = if (correct) 1 else 0
            UserWordProgress(
                userId = userId,
                wordId = wordId,
                setId = setId,
                easeFactor = 2.5f,
                interval = interval,
                repetitions = repetitions,
                lastReviewedAt = now,
                nextReviewDate = Date(now.time + interval * ONE_DAY_MS),
                status = if (correct) "learning" else "learning"
            )
        } else {
            // Từ đã có progress — áp dụng SM-2
            var rep = existing.repetitions
            var intv = existing.interval
            var fac = existing.easeFactor

            if (correct) {
                rep += 1
                intv = when (rep) {
                    1 -> 1
                    2 -> 6
                    else -> (intv * fac).toInt().coerceAtLeast(1)
                }
                // Ease factor không thay đổi khi chỉ đúng (SM-2 gốc dùng quality rating)
                // Giữ nguyên factor để đơn giản hóa cho binary correct/incorrect
            } else {
                rep = 0
                intv = 1
                fac = (fac - 0.2f).coerceAtLeast(MIN_EASE_FACTOR)
            }

            val status = when {
                rep >= 5 -> "mastered"
                rep >= 1 -> "reviewing"
                else -> "learning"
            }

            existing.copy(
                repetitions = rep,
                interval = intv,
                easeFactor = fac,
                lastReviewedAt = now,
                nextReviewDate = Date(now.time + intv * ONE_DAY_MS),
                status = status
            )
        }

        return repository.updateProgress(updated)
    }
}
