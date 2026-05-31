package com.edu.minlish.features.stats.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.core.util.AppSettings
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.learning.domain.model.UserReviewLog
import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.edu.minlish.features.stats.domain.LevelEstimate
import com.edu.minlish.features.stats.domain.LevelEstimator
import com.edu.minlish.features.stats.presentation.components.BarChartData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RatingBreakdownCounts(
    val easy: Int = 0,
    val good: Int = 0,
    val hard: Int = 0,
    val again: Int = 0
) {
    val total: Int
        get() = easy + good + hard + again
}

sealed class StatsUiState {
    object Loading : StatsUiState()

    data class Success(
        val totalWords: Int,
        val masteredWords: Int,
        val learningWords: Int,
        val dueTodayWords: Int,
        val retentionRate: Float,
        val ratingBreakdown: RatingBreakdownCounts,
        val currentStreak: Int,
        val weeklyData: List<BarChartData>,
        val weeklyActiveIndex: Int,
        val weeklyCompletedDays: List<Boolean>,
        val monthlyData: List<BarChartData>,
        val levelEstimate: LevelEstimate
    ) : StatsUiState() {
        val easyCount: Int get() = ratingBreakdown.easy
        val goodCount: Int get() = ratingBreakdown.good
        val hardCount: Int get() = ratingBreakdown.hard
        val againCount: Int get() = ratingBreakdown.again
    }

    data class Error(val message: String) : StatsUiState()
}

class StatsViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf<StatsUiState>(StatsUiState.Loading)
        private set

    init {
        loadStats()
    }

    fun loadStats() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = StatsUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            uiState = StatsUiState.Loading
            try {
                val progressSnapshot = firestore.collection("user_word_progress")
                    .whereEqualTo("userId", currentUser.id)
                    .get()
                    .await()

                val progresses = progressSnapshot.toObjects(UserWordProgress::class.java)
                val logs = loadReviewLogs(currentUser.id)

                val masteredThreshold = AppSettings.masteredThreshold
                val totalWords = progresses.size
                val masteredWords = progresses.count {
                    it.status == "mastered" || it.interval > masteredThreshold
                }
                val learningWords = totalWords - masteredWords
                val now = Date()
                val dueTodayWords = progresses.count {
                    it.nextReviewDate.before(now) || it.nextReviewDate == now
                }

                val ratingBreakdown = buildRatingBreakdown(logs)
                val retentionRate = calculateRetentionRate(logs, progresses)
                val weeklyData = buildWeeklyData(logs)
                val weeklyCompletedDays = weeklyData.map { it.value > 0 }
                val monthlyData = buildMonthlyData(logs)
                val currentStreak = calculateCurrentStreak(logs)
                val activeDaysLast7 = weeklyCompletedDays.count { it }
                val levelEstimate = LevelEstimator.estimate(
                    totalWords = totalWords,
                    masteredWords = masteredWords,
                    retentionRate = retentionRate,
                    activeDaysLast7 = activeDaysLast7
                )

                uiState = StatsUiState.Success(
                    totalWords = totalWords,
                    masteredWords = masteredWords,
                    learningWords = learningWords,
                    dueTodayWords = dueTodayWords,
                    retentionRate = retentionRate,
                    ratingBreakdown = ratingBreakdown,
                    currentStreak = currentStreak,
                    weeklyData = weeklyData,
                    weeklyActiveIndex = weeklyData.lastIndex.coerceAtLeast(0),
                    weeklyCompletedDays = weeklyCompletedDays,
                    monthlyData = monthlyData,
                    levelEstimate = levelEstimate
                )
            } catch (e: Exception) {
                uiState = StatsUiState.Error(e.message ?: "Failed to load stats")
            }
        }
    }

    private suspend fun loadReviewLogs(userId: String): List<UserReviewLog> {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -370)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val snapshot = firestore.collection("user_review_logs")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("reviewedAt", calendar.time)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(UserReviewLog::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun buildRatingBreakdown(logs: List<UserReviewLog>): RatingBreakdownCounts {
        return RatingBreakdownCounts(
            easy = logs.count { it.rating.equals("EASY", ignoreCase = true) },
            good = logs.count { it.rating.equals("GOOD", ignoreCase = true) },
            hard = logs.count { it.rating.equals("HARD", ignoreCase = true) },
            again = logs.count { it.rating.equals("AGAIN", ignoreCase = true) }
        )
    }

    private fun calculateRetentionRate(
        logs: List<UserReviewLog>,
        progresses: List<UserWordProgress>
    ): Float {
        if (logs.isNotEmpty()) {
            val remembered = logs.count {
                it.rating.equals("GOOD", ignoreCase = true) || it.rating.equals("EASY", ignoreCase = true)
            }
            return (remembered.toFloat() / logs.size.toFloat() * 100f).coerceIn(0f, 100f)
        }

        if (progresses.isEmpty()) return 0f
        val avgEaseFactor = progresses.map { it.easeFactor }.average().toFloat()
        return (avgEaseFactor / 2.5f * 100f).coerceIn(0f, 100f)
    }

    private fun buildWeeklyData(logs: List<UserReviewLog>): List<BarChartData> {
        val formatter = SimpleDateFormat("EEE", Locale.ENGLISH)
        val today = startOfDay(Date())

        return (6 downTo 0).map { daysAgo ->
            val day = addDays(today, -daysAgo)
            val count = logsForDay(logs, day).map { it.wordId }.distinct().size
            BarChartData(formatter.format(day), count)
        }
    }

    private fun buildMonthlyData(logs: List<UserReviewLog>): List<BarChartData> {
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekRanges = listOf(1 to 7, 8 to 14, 15 to 21, 22 to monthStart.getActualMaximum(Calendar.DAY_OF_MONTH))

        return weekRanges.mapIndexed { index, (startDay, endDay) ->
            val start = monthStart.clone() as Calendar
            start.set(Calendar.DAY_OF_MONTH, startDay)
            val end = monthStart.clone() as Calendar
            end.set(Calendar.DAY_OF_MONTH, endDay)
            end.set(Calendar.HOUR_OF_DAY, 23)
            end.set(Calendar.MINUTE, 59)
            end.set(Calendar.SECOND, 59)
            end.set(Calendar.MILLISECOND, 999)

            val count = logs.filter { it.reviewedAt in start.time..end.time }
                .map { it.wordId }
                .distinct()
                .size
            BarChartData("W${index + 1}", count)
        }
    }

    private fun calculateCurrentStreak(logs: List<UserReviewLog>): Int {
        if (logs.isEmpty()) return 0

        val daysWithLogs = logs.map { dayKey(it.reviewedAt) }.toSet()
        val today = startOfDay(Date())
        val yesterday = addDays(today, -1)
        var cursor = when {
            daysWithLogs.contains(dayKey(today)) -> today
            daysWithLogs.contains(dayKey(yesterday)) -> yesterday
            else -> return 0
        }

        var streak = 0
        while (daysWithLogs.contains(dayKey(cursor))) {
            streak++
            cursor = addDays(cursor, -1)
        }
        return streak
    }

    private fun logsForDay(logs: List<UserReviewLog>, day: Date): List<UserReviewLog> {
        val start = startOfDay(day)
        val end = addDays(start, 1)
        return logs.filter { !it.reviewedAt.before(start) && it.reviewedAt.before(end) }
    }

    private fun startOfDay(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun addDays(date: Date, days: Int): Date {
        return Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_YEAR, days)
        }.time
    }

    private fun dayKey(date: Date): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(date)
    }
}
