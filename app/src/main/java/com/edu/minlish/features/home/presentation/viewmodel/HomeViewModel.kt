package com.edu.minlish.features.home.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class RecentWordItem(val id: String, val word: String, val meaning: String)

data class HomeUiState(
    val dateString: String = "",
    val userName: String = "",
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

    var uiState by mutableStateOf(HomeUiState())
        private set

    private var hasLoadedData = false
    private var isLoadInProgress = false

    init {
        loadHomeData(showLoading = true)
    }

    fun loadHomeData(showLoading: Boolean = !hasLoadedData) {
        if (isLoadInProgress) return

        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = uiState.copy(isLoading = false, errorMessage = "User not logged in")
            return
        }

        val shouldShowLoading = showLoading && !hasLoadedData
        if (shouldShowLoading) {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
        } else {
            uiState = uiState.copy(isLoading = false, errorMessage = null)
        }

        isLoadInProgress = true
        viewModelScope.launch {
            try {
                // 1. Get current date string
                val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH)
                val dateString = dateFormat.format(Date())

                // 2. Get user's profile and target limits
                var targetNew = 10
                var targetReview = 20
                profileRepository.getProfile(currentUser.id).onSuccess { profile ->
                    if (profile != null) {
                        targetNew = profile.dailyNewWordsTarget
                        targetReview = profile.dailyReviewWordsTarget
                    }
                }

                // 3. Get review logs once and derive today's progress/streak from real data
                val reviewLogs = try {
                    loadReviewLogs(currentUser.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load review logs for userId=${currentUser.id}: ${e.message}")
                    emptyList()
                }
                val todayLogs = logsForDay(reviewLogs, Date())
                val todayPlanDone = todayLogs
                    .map { it.wordId }
                    .distinct()
                    .size
                Log.d(
                    TAG,
                    "Today's plan logs userId=${currentUser.id}, totalLogs=${reviewLogs.size}, todayLogs=${todayLogs.size}, todayUniqueWords=$todayPlanDone"
                )

                // 4. Get due words count today
                var dueTodayCount = 0
                learningRepository.getDueWords(currentUser.id, setId = null, forceAll = false)
                    .onSuccess { due ->
                        dueTodayCount = due.size
                    }
                    .onFailure { e ->
                        Log.e(TAG, "Failed to load due words: ${e.message}")
                    }

                // 5. Get total learned count and accuracy/retention
                val progresses = try {
                    firestore.collection("user_word_progress")
                        .whereEqualTo("userId", currentUser.id)
                        .get()
                        .await()
                        .toObjects(UserWordProgress::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load user progress: ${e.message}")
                    emptyList()
                }
                val learnedCount = progresses.size

                val avgEaseFactor = if (progresses.isNotEmpty()) progresses.map { it.easeFactor }.average() else 0.0
                val retentionRate = if (progresses.isEmpty()) 100 else (avgEaseFactor / 2.5f * 100).coerceAtMost(100.0).toInt()

                // 6. Get recently studied words
                val recentWords = try {
                    val recentSnapshot = firestore.collection("user_word_progress")
                        .whereEqualTo("userId", currentUser.id)
                        .orderBy("lastReviewedAt", Query.Direction.DESCENDING)
                        .limit(3)
                        .get()
                        .await()

                    recentSnapshot.documents.mapNotNull { doc ->
                        try {
                            val progress = doc.toObject(UserWordProgress::class.java) ?: return@mapNotNull null

                            // Fetch the word details
                            val wordDoc = firestore.collection("vocabulary_words").document(progress.wordId).get().await()
                            val word = wordDoc.getString("word") ?: ""

                            // Parse Vietnamese meaning
                            val definitionsData = wordDoc.get("definitions") as? List<Map<String, Any>> ?: emptyList()
                            val meaning = definitionsData.firstOrNull()?.get("meaningVietnamese") as? String ?: ""

                            RecentWordItem(id = progress.wordId, word = word, meaning = meaning)
                        } catch (e: Exception) {
                            null
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load recently studied words: ${e.message}")
                    emptyList()
                }

                // 7. Calculate current streak from real review logs
                val streakDays = try {
                    calculateCurrentStreak(reviewLogs)
                } catch (e: Exception) {
                    0
                }

                uiState = HomeUiState(
                    dateString = dateString,
                    userName = currentUser.fullName ?: "User",
                    streakDays = streakDays,
                    learnedCount = learnedCount,
                    dueTodayCount = dueTodayCount,
                    accuracy = "$retentionRate%",
                    todayPlanDone = todayPlanDone,
                    todayPlanTotal = targetNew + targetReview,
                    recentWords = recentWords,
                    isLoading = false
                )
                hasLoadedData = true
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, errorMessage = e.message ?: "Failed to load dashboard data")
            } finally {
                isLoadInProgress = false
            }
        }
    }

    private suspend fun loadReviewLogs(userId: String): List<UserReviewLog> {
        val snapshot = firestore.collection("user_review_logs")
            .whereEqualTo("userId", userId)
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
