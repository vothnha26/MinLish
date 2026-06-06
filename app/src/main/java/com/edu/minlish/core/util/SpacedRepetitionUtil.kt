package com.edu.minlish.core.util

import com.edu.minlish.features.learning.domain.model.UserWordProgress
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

/**
 * Pure Kotlin Spaced Repetition Utility implementing the SM-2 algorithm.
 * Used project-wide for updating word progress.
 */
object SpacedRepetitionUtil {

    private const val MIN_EASE_FACTOR = 1.3f

    /**
     * Creates an initial UserWordProgress for a newly learned word.
     */
    fun createInitialProgress(
        userId: String,
        wordId: String,
        setId: String,
        correct: Boolean,
        intervalUnitMs: Long = 24L * 60 * 60 * 1000
    ): UserWordProgress {
        val now = Date()
        val interval = 1
        val repetitions = if (correct) 1 else 0
        return UserWordProgress(
            userId = userId,
            wordId = wordId,
            setId = setId,
            easeFactor = 2.5f,
            interval = interval,
            repetitions = repetitions,
            lastReviewedAt = now,
            nextReviewDate = Date(now.time + interval * intervalUnitMs),
            status = "learning"
        )
    }

    /**
     * SM-2 Algorithm Implementation for Multi-level Rating (Flashcards)
     * rating: 0 (Again), 1 (Hard), 2 (Good), 3 (Easy)
     * Maps to quality: 0-5 in original SM-2. Here we simplify to 4 levels.
     */
    fun calculateSM2ForRating(
        current: UserWordProgress,
        rating: Int,
        intervalUnit: String = AppSettings.intervalUnit,
        masteredThreshold: Int = AppSettings.masteredThreshold
    ): UserWordProgress {
        val q = when (rating) {
            0 -> 0 // Again
            1 -> 3 // Hard
            2 -> 4 // Good
            3 -> 5 // Easy
            else -> 3
        }

        var nextEaseFactor = current.easeFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        if (nextEaseFactor < MIN_EASE_FACTOR) nextEaseFactor = MIN_EASE_FACTOR

        val nextRepetitions: Int
        val nextInterval: Int

        if (q < 3) {
            nextRepetitions = 0
            nextInterval = 1
        } else {
            nextRepetitions = current.repetitions + 1
            nextInterval = when (nextRepetitions) {
                1 -> 1
                2 -> 6
                else -> (current.interval * nextEaseFactor).roundToInt()
            }
        }

        val calendar = Calendar.getInstance()
        when (intervalUnit) {
            "MINUTES" -> calendar.add(Calendar.MINUTE, nextInterval)
            "HOURS" -> calendar.add(Calendar.HOUR_OF_DAY, nextInterval)
            else -> calendar.add(Calendar.DAY_OF_YEAR, nextInterval)
        }

        val status = if (nextInterval > masteredThreshold) "mastered" else "reviewing"

        return current.copy(
            easeFactor = nextEaseFactor,
            interval = nextInterval,
            repetitions = nextRepetitions,
            nextReviewDate = calendar.time,
            lastReviewedAt = Date(),
            status = status
        )
    }

    /**
     * SM-2 Algorithm Implementation for Binary Rating (Correct/Incorrect in Quizzes/Games)
     */
    fun calculateSM2ForBinary(
        current: UserWordProgress,
        correct: Boolean,
        intervalUnitMs: Long = 24L * 60 * 60 * 1000,
        masteredThreshold: Int = AppSettings.masteredThreshold
    ): UserWordProgress {
        val now = Date()
        var rep = current.repetitions
        var intv = current.interval
        var fac = current.easeFactor

        if (correct) {
            rep += 1
            intv = when (rep) {
                1 -> 1
                2 -> 6
                else -> (intv * fac).toInt().coerceAtLeast(1)
            }
        } else {
            rep = 0
            intv = 1
            fac = (fac - 0.2f).coerceAtLeast(MIN_EASE_FACTOR)
        }

        val status = when {
            intv > masteredThreshold -> "mastered"
            rep >= 1 -> "reviewing"
            else -> "learning"
        }

        return current.copy(
            repetitions = rep,
            interval = intv,
            easeFactor = fac,
            lastReviewedAt = now,
            nextReviewDate = Date(now.time + intv * intervalUnitMs),
            status = status
        )
    }
}
