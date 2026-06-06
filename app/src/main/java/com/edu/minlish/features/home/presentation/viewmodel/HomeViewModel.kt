package com.edu.minlish.features.home.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.learning.data.repository.FirestoreLearningRepositoryImpl
import com.edu.minlish.features.learning.domain.model.UserReviewLog
import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.edu.minlish.features.learning.domain.repository.LearningRepository
import com.edu.minlish.features.profilesetup.data.repository.FirestoreProfileRepositoryImpl
import com.edu.minlish.features.profilesetup.domain.repository.ProfileRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class RecentWordItem(val id: String, val word: String, val meaning: String)

data class HomeUiState(
    val dateString: String = "",
    val userName: String = "",
    val greeting: String = "Hello 👋",
    val streakDays: Int = 0,
    val learnedCount: Int = 0,
    val dueTodayCount: Int = 0,
    val accuracy: String = "100%",
    val todayPlanDone: Int = 0,
    val todayPlanTotal: Int = 20,
    val recentWords: List<RecentWordItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val learningRepository: LearningRepository = FirestoreLearningRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val profileRepository: ProfileRepository = FirestoreProfileRepositoryImpl(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private companion object {
        const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeSessionData()
    }

    private fun observeSessionData() {
        val currentUser = authRepository.getCurrentUser() ?: return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            combine(
                com.edu.minlish.core.util.SessionDataManager.userProfileFlow,
                com.edu.minlish.core.util.SessionDataManager.userWordProgressesFlow,
                com.edu.minlish.core.util.SessionDataManager.userReviewLogsFlow
            ) { profile, progresses, logs ->
                Triple(profile, progresses, logs)
            }.collectLatest { (profile, progresses, logs) ->
                if (profile != null && progresses != null && logs != null) {
                    triggerDashboardUpdate(profile, progresses, logs)
                }
            }
        }
    }

    fun loadHomeData(showLoading: Boolean = false) {
        val currentUser = authRepository.getCurrentUser() ?: return
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true) }
        }

        // Cập nhật UI ngay lập tức từ cache hiện tại
        val cache = com.edu.minlish.core.util.SessionDataManager
        val profile = cache.userProfile
        val progresses = cache.userWordProgresses
        val logs = cache.userReviewLogs
        if (profile != null && progresses != null && logs != null) {
            triggerDashboardUpdate(profile, progresses, logs)
        }

        // Refresh ở background thông qua SessionDataManager để giữ tính nhất quán
        viewModelScope.launch {
            try {
                com.edu.minlish.core.util.SessionDataManager.preFetchUserData(currentUser.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh session data", e)
            }
        }
    }

    private fun triggerDashboardUpdate(
        profile: com.edu.minlish.features.profilesetup.domain.model.UserProfile,
        progresses: List<UserWordProgress>,
        logs: List<UserReviewLog>
    ) {
        val currentUser = authRepository.getCurrentUser() ?: return
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH)
        val dateString = dateFormat.format(Date())

        // Đồng bộ mục tiêu từ Firestore về local
        com.edu.minlish.core.util.AppSettings.dailyNewWordsTarget = profile.dailyNewWordsTarget

        val targetNew = com.edu.minlish.core.util.AppSettings.dailyNewWordsTarget
        val targetReview = profile.dailyReviewWordsTarget

        val todayLogs = logsForDay(logs, Date())
        val todayPlanDone = todayLogs.map { it.wordId }.distinct().size
        val learnedCount = progresses.size

        val avgEaseFactor = if (progresses.isNotEmpty()) progresses.map { it.easeFactor }.average() else 0.0
        val retentionRate = if (progresses.isEmpty()) 100 else (avgEaseFactor / 2.5f * 100).coerceAtMost(100.0).toInt()

        val streakDays = calculateCurrentStreak(logs)
        val now = Date()
        val dueTodayCount = progresses.filter { it.nextReviewDate.before(now) || it.nextReviewDate == now }.size

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingPrefix = when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..21 -> "Good evening"
            else -> "Good night"
        }
        val nameToDisplay = currentUser.fullName ?: "User"
        val greeting = "$greetingPrefix, $nameToDisplay 👋"

        _uiState.update {
            HomeUiState(
                dateString = dateString,
                userName = nameToDisplay,
                greeting = greeting,
                streakDays = streakDays,
                learnedCount = learnedCount,
                dueTodayCount = dueTodayCount,
                accuracy = "$retentionRate%",
                todayPlanDone = todayPlanDone,
                todayPlanTotal = targetNew + targetReview,
                recentWords = it.recentWords,
                isLoading = false
            )
        }

        loadRecentWordsAsync(currentUser.id, progresses)
    }

    private fun loadRecentWordsAsync(userId: String, progresses: List<UserWordProgress>) {
        viewModelScope.launch {
            try {
                val recentWords = progresses.sortedByDescending { it.lastReviewedAt }
                    .take(3)
                    .mapNotNull { progress ->
                        try {
                            val wordDoc = firestore.collection("vocabulary_words").document(progress.wordId).get().await()
                            val word = wordDoc.getString("word") ?: ""
                            val definitionsData = (wordDoc.get("definitions") as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
                            val meaning = definitionsData.firstOrNull()?.get("meaningVietnamese") as? String ?: ""
                            RecentWordItem(id = progress.wordId, word = word, meaning = meaning)
                        } catch (e: Exception) {
                            null
                        }
                    }
                _uiState.update { it.copy(recentWords = recentWords) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load recent words asynchronously", e)
            }
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
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    }
}
