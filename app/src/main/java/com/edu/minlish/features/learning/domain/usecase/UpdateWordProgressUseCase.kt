package com.edu.minlish.features.learning.domain.usecase

import com.edu.minlish.features.learning.domain.model.UserReviewLog
import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.edu.minlish.features.learning.domain.repository.LearningRepository
import java.util.Date

/**
 * UseCase: Tính toán SM-2, lưu progress và log review lên Firestore, đồng thời cập nhật local cache.
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

        val progressResult = repository.updateProgress(updated)
        if (progressResult.isFailure) {
            return progressResult
        }

        // Tạo và lưu log review ôn tập để thống kê ở màn hình Stats
        val ratingStr = if (correct) "GOOD" else "AGAIN"
        val log = UserReviewLog(
            userId = userId,
            wordId = wordId,
            reviewedAt = Date(),
            rating = ratingStr,
            intervalBefore = existing?.interval ?: 0,
            intervalAfter = updated.interval
        )
        repository.logReview(log)

        // Cập nhật local cache ngay lập tức để đồng bộ màu chấm tròn (xanh dương/xanh lá) ở danh sách từ và Home
        val sessionCache = com.edu.minlish.core.util.SessionDataManager
        
        val currentLogs = sessionCache.userReviewLogs?.toMutableList() ?: mutableListOf()
        currentLogs.add(log)
        sessionCache.userReviewLogs = currentLogs

        val currentProgresses = sessionCache.userWordProgresses?.toMutableList() ?: mutableListOf()
        val existingIndex = currentProgresses.indexOfFirst { it.wordId == updated.wordId }
        if (existingIndex >= 0) {
            currentProgresses[existingIndex] = updated
        } else {
            currentProgresses.add(updated)
        }
        sessionCache.userWordProgresses = currentProgresses

        return Result.success(Unit)
    }
}
