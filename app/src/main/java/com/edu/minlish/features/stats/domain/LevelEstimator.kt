package com.edu.minlish.features.stats.domain

data class LevelEstimate(
    val code: String,
    val label: String,
    val nextCode: String?,
    val nextLabel: String?,
    val score: Float,
    val progressToNext: Float
)

object LevelEstimator {
    fun estimate(
        totalWords: Int,
        masteredWords: Int,
        retentionRate: Float,
        activeDaysLast7: Int
    ): LevelEstimate {
        val masteryRate = if (totalWords > 0) masteredWords.toFloat() / totalWords else 0f
        val wordScore = (totalWords / 1000f * 100f).coerceIn(0f, 100f)
        val masteryScore = (masteryRate * 100f).coerceIn(0f, 100f)
        val consistencyScore = (activeDaysLast7 / 7f * 100f).coerceIn(0f, 100f)
        val boundedRetention = retentionRate.coerceIn(0f, 100f)

        val score = wordScore * 0.35f +
            masteryScore * 0.30f +
            boundedRetention * 0.25f +
            consistencyScore * 0.10f

        return mapScoreToLevel(score)
    }

    private fun mapScoreToLevel(score: Float): LevelEstimate {
        val levels = listOf(
            LevelRange("A1", "Beginner", 0f, 15f),
            LevelRange("A2", "Elementary", 15f, 30f),
            LevelRange("B1", "Intermediate", 30f, 50f),
            LevelRange("B2", "Upper-intermediate", 50f, 70f),
            LevelRange("C1", "Advanced", 70f, 88f),
            LevelRange("C2", "Mastery", 88f, 100f)
        )

        val currentIndex = levels.indexOfFirst { score <= it.max }.coerceAtLeast(0)
        val current = levels[currentIndex]
        val next = levels.getOrNull(currentIndex + 1)
        val progress = if (next == null) {
            1f
        } else {
            ((score - current.min) / (current.max - current.min)).coerceIn(0f, 1f)
        }

        return LevelEstimate(
            code = current.code,
            label = current.label,
            nextCode = next?.code,
            nextLabel = next?.label,
            score = score.coerceIn(0f, 100f),
            progressToNext = progress
        )
    }

    private data class LevelRange(
        val code: String,
        val label: String,
        val min: Float,
        val max: Float
    )
}
