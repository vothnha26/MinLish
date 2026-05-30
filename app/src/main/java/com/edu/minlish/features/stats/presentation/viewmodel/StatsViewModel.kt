package com.edu.minlish.features.stats.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(
        val totalWords: Int,
        val masteredWords: Int,
        val learningWords: Int,
        val retentionRate: Float,
        val easyCount: Int,
        val goodCount: Int,
        val hardCount: Int,
        val againCount: Int,
        val currentStreak: Int
    ) : StatsUiState()
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
                // Fetch user's word progress
                val snapshot = firestore.collection("user_word_progress")
                    .whereEqualTo("userId", currentUser.id)
                    .get()
                    .await()

                val progresses = snapshot.toObjects(UserWordProgress::class.java)

                val masteredThreshold = com.edu.minlish.core.util.AppSettings.masteredThreshold
                val totalWords = progresses.size
                val masteredWords = progresses.count { it.status == "mastered" || it.interval > masteredThreshold }
                val learningWords = totalWords - masteredWords

                // Calculate retention rate based on Ease Factor (EF)
                // Assuming starting EF is 2.5. If average EF is 2.5 or higher, retention is good.
                val avgEaseFactor = if (progresses.isNotEmpty()) progresses.map { it.easeFactor }.average() else 0.0
                val retentionRate = if (progresses.isEmpty()) 0f else (avgEaseFactor / 2.5f * 100).coerceAtMost(100.0).toFloat()

                // Simulating rating breakdown based on EF ranges (Since SM-2 stores EF, not exact past ratings easily)
                var easy = 0
                var good = 0
                var hard = 0
                var again = 0

                progresses.forEach {
                    when {
                        it.easeFactor >= 2.6f -> easy++
                        it.easeFactor >= 2.4f -> good++
                        it.easeFactor >= 2.0f -> hard++
                        else -> again++
                    }
                }
                
                // For demo: Mocking streak calculation
                val streak = 7

                uiState = StatsUiState.Success(
                    totalWords = totalWords,
                    masteredWords = masteredWords,
                    learningWords = learningWords,
                    retentionRate = retentionRate,
                    easyCount = easy,
                    goodCount = good,
                    hardCount = hard,
                    againCount = again,
                    currentStreak = streak
                )
            } catch (e: Exception) {
                uiState = StatsUiState.Error(e.message ?: "Failed to load stats")
            }
        }
    }
}
