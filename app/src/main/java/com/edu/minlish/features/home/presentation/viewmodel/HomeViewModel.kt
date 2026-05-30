package com.edu.minlish.features.home.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.learning.data.repository.FirestoreLearningRepositoryImpl
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
    val streakDays: Int = 7,
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

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = uiState.copy(isLoading = false, errorMessage = "User not logged in")
            return
        }

        uiState = uiState.copy(isLoading = true)

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

                // 3. Get today's plan progress (from user_review_logs)
                var todayPlanDone = 0
                learningRepository.getReviewLogsForDate(currentUser.id, Date()).onSuccess { logs ->
                    // Count unique words reviewed today
                    todayPlanDone = logs.map { it.wordId }.distinct().size
                }

                // 4. Get due words count today
                var dueTodayCount = 0
                learningRepository.getDueWords(currentUser.id, setId = null, forceAll = false).onSuccess { due ->
                    dueTodayCount = due.size
                }

                // 5. Get total learned count and accuracy/retention
                val progressSnapshot = firestore.collection("user_word_progress")
                    .whereEqualTo("userId", currentUser.id)
                    .get()
                    .await()
                val progresses = progressSnapshot.toObjects(UserWordProgress::class.java)
                val learnedCount = progresses.size

                val avgEaseFactor = if (progresses.isNotEmpty()) progresses.map { it.easeFactor }.average() else 0.0
                val retentionRate = if (progresses.isEmpty()) 100 else (avgEaseFactor / 2.5f * 100).coerceAtMost(100.0).toInt()

                // 6. Get recently studied words
                val recentSnapshot = firestore.collection("user_word_progress")
                    .whereEqualTo("userId", currentUser.id)
                    .orderBy("lastReviewedAt", Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .await()
                
                val recentWords = recentSnapshot.documents.mapNotNull { doc ->
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

                // 7. Get streak (Fetch from stats if exists)
                var streakDays = 7
                try {
                    val statsDoc = firestore.collection("stats").document(currentUser.id).get().await()
                    if (statsDoc.exists()) {
                        streakDays = statsDoc.getLong("currentStreak")?.toInt() ?: 7
                    }
                } catch (e: Exception) {
                    // Fail silently, fallback to 7
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
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, errorMessage = e.message ?: "Failed to load dashboard data")
            }
        }
    }
}
