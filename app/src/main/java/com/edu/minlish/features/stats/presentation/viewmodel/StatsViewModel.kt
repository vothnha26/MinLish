package com.edu.minlish.features.stats.presentation.viewmodel

import android.util.Log
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

data class RatedWordItem(
    val wordId: String,
    val word: String,
    val meaning: String,
    val lastRatedAt: Date
)

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
        val levelEstimate: LevelEstimate,
        val wordsByRating: Map<String, List<RatedWordItem>> = emptyMap(),
        val freezesLeft: Int = AppSettings.streakFreezesLeft,
        val isFreezeEquipped: Boolean = AppSettings.isStreakFreezeEquipped
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

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        observeSessionData()
    }

    private fun observeSessionData() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.update { StatsUiState.Error("User not logged in") }
            return
        }

        viewModelScope.launch {
            combine(
                com.edu.minlish.core.util.SessionDataManager.userWordProgressesFlow,
                com.edu.minlish.core.util.SessionDataManager.userReviewLogsFlow
            ) { progresses, logs ->
                Pair(progresses, logs)
            }.collectLatest { (progresses, logs) ->
                if (progresses != null && logs != null) {
                    calculateStats(progresses, logs)
                }
            }
        }
    }

    fun loadStats() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.update { StatsUiState.Error("User not logged in") }
            return
        }

        // Tải lại ở background thông qua SessionDataManager để giữ tính nhất quán
        viewModelScope.launch {
            try {
                com.edu.minlish.core.util.SessionDataManager.preFetchUserData(currentUser.id)
            } catch (e: Exception) {
                Log.e("StatsViewModel", "Failed to refresh stats in background", e)
            }
        }
    }

    fun equipStreakFreeze() {
        val currentState = _uiState.value
        if (currentState is StatsUiState.Success && !currentState.isFreezeEquipped && currentState.freezesLeft > 0) {
            val newFreezesLeft = currentState.freezesLeft - 1
            AppSettings.streakFreezesLeft = newFreezesLeft
            AppSettings.isStreakFreezeEquipped = true
            
            _uiState.update { 
                currentState.copy(
                    freezesLeft = newFreezesLeft,
                    isFreezeEquipped = true
                )
            }
        }
    }

    private suspend fun calculateStats(
        progresses: List<UserWordProgress>,
        logs: List<UserReviewLog>
    ) {
        try {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -370)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val limitTime = calendar.time
            val filteredLogs = logs.filter { !it.reviewedAt.before(limitTime) }

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

            val ratingBreakdown = buildRatingBreakdown(filteredLogs)
            val retentionRate = calculateRetentionRate(filteredLogs, progresses)
            val weeklyData = buildWeeklyData(filteredLogs)
            val weeklyCompletedDays = weeklyData.map { it.value > 0 }
            val monthlyData = buildMonthlyData(filteredLogs)
            val currentStreak = calculateCurrentStreak(filteredLogs)
            val activeDaysLast7 = weeklyCompletedDays.count { it }
            val levelEstimate = LevelEstimator.estimate(
                totalWords = totalWords,
                masteredWords = masteredWords,
                retentionRate = retentionRate,
                activeDaysLast7 = activeDaysLast7
            )

            // Lấy danh sách từ theo rating từ logs
            val wordsByRating = buildWordsByRating(filteredLogs)

            _uiState.update { 
                StatsUiState.Success(
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
                    levelEstimate = levelEstimate,
                    wordsByRating = wordsByRating,
                    freezesLeft = AppSettings.streakFreezesLeft,
                    isFreezeEquipped = AppSettings.isStreakFreezeEquipped
                )
            }
        } catch (e: Exception) {
            _uiState.update { StatsUiState.Error(e.message ?: "Failed to calculate stats") }
        }
    }

    private suspend fun buildWordsByRating(
        logs: List<UserReviewLog>
    ): Map<String, List<RatedWordItem>> {
        val ratings = listOf("EASY", "GOOD", "HARD", "AGAIN")

        val grouped = ratings.associateWith { rating ->
            logs.filter { it.rating.equals(rating, ignoreCase = true) }
                .groupBy { it.wordId }
                .mapValues { (_, logList) -> logList.maxByOrNull { it.reviewedAt }!! }
                .values
                .sortedByDescending { it.reviewedAt }
                .take(50)
        }

        val allWordIds = grouped.values.flatten().map { it.wordId }.distinct()
        val wordInfoMap = mutableMapOf<String, Pair<String, String>>()

        allWordIds.map { wordId ->
            viewModelScope.async {
                try {
                    val doc = firestore.collection("vocabulary_words").document(wordId).get().await()
                    val word = doc.getString("word") ?: ""
                    val definitions = (doc.get("definitions") as? List<*>)
                        ?.filterIsInstance<Map<String, Any>>() ?: emptyList()
                    val meaning = definitions.firstOrNull()
                        ?.get("meaningVietnamese") as? String ?: ""
                    if (word.isNotBlank()) {
                        synchronized(wordInfoMap) {
                            wordInfoMap[wordId] = word to meaning
                        }
                    }
                } catch (_: Exception) {}
            }
        }.awaitAll()

        return grouped.mapValues { (_, logList) ->
            logList.mapNotNull { log ->
                val (word, meaning) = wordInfoMap[log.wordId] ?: return@mapNotNull null
                RatedWordItem(
                    wordId = log.wordId,
                    word = word,
                    meaning = meaning,
                    lastRatedAt = log.reviewedAt
                )
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
