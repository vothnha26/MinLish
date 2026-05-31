package com.edu.minlish.features.learning.presentation.viewmodel

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
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.profilesetup.data.repository.FirestoreProfileRepositoryImpl
import com.edu.minlish.features.profilesetup.domain.repository.ProfileRepository
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

sealed class FlashcardUiState {
    object Loading : FlashcardUiState()
    data class Success(val words: List<Pair<VocabularyWord, UserWordProgress?>>) : FlashcardUiState()
    object Finished : FlashcardUiState()
    data class Error(val message: String) : FlashcardUiState()
}

class FlashcardViewModel(
    private val repository: LearningRepository = FirestoreLearningRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val profileRepository: ProfileRepository = FirestoreProfileRepositoryImpl()
) : ViewModel() {

    private companion object {
        const val TAG = "FlashcardViewModel"
    }

    var uiState by mutableStateOf<FlashcardUiState>(FlashcardUiState.Loading)
        private set

    var currentIndex by mutableStateOf(0)
    var isFlipped by mutableStateOf(false)
    var isSubmittingRating by mutableStateOf(false)
        private set

    fun loadWords(setId: String?, forceAll: Boolean = false) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = FlashcardUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            uiState = FlashcardUiState.Loading
            
            val result = if (forceAll) {
                repository.getDueWords(currentUser.id, setId, forceAll = true)
            } else {
                var targetNew = 10
                var targetReview = 20
                
                profileRepository.getProfile(currentUser.id).onSuccess { profile ->
                    if (profile != null) {
                        targetNew = profile.dailyNewWordsTarget
                        targetReview = profile.dailyReviewWordsTarget
                    }
                }
                
                repository.getDailySessionWords(currentUser.id, targetNew, targetReview, setId)
            }

            result
                .onSuccess { dueWords ->
                    if (dueWords.isEmpty()) {
                        uiState = FlashcardUiState.Finished
                    } else {
                        uiState = FlashcardUiState.Success(dueWords)
                        currentIndex = 0
                    }
                }
                .onFailure { e ->
                    uiState = FlashcardUiState.Error(e.message ?: "Failed to load words")
                }
        }
    }

    fun onFlip() {
        isFlipped = !isFlipped
    }

    fun submitRating(rating: Int) {
        if (isSubmittingRating) return

        val currentWords = (uiState as? FlashcardUiState.Success)?.words ?: return
        val (word, progress) = currentWords[currentIndex]
        val currentUser = authRepository.getCurrentUser() ?: return

        viewModelScope.launch {
            isSubmittingRating = true
            try {
                val updatedProgress = calculateSM2(progress ?: UserWordProgress(userId = currentUser.id, wordId = word.id, setId = word.vocabularySetId), rating)
                val progressResult = repository.updateProgress(updatedProgress)
                if (progressResult.isFailure) {
                    val message = progressResult.exceptionOrNull()?.message ?: "Failed to update progress"
                    Log.e(TAG, "Failed to update progress for wordId=${word.id}: $message")
                    uiState = FlashcardUiState.Error(message)
                    return@launch
                }

                val ratingStr = when (rating) {
                    0 -> "AGAIN"
                    1 -> "HARD"
                    2 -> "GOOD"
                    3 -> "EASY"
                    else -> "GOOD"
                }

                val log = UserReviewLog(
                    userId = currentUser.id,
                    wordId = word.id,
                    reviewedAt = Date(),
                    rating = ratingStr,
                    intervalBefore = progress?.interval ?: 0,
                    intervalAfter = updatedProgress.interval
                )
                val logResult = repository.logReview(log)
                if (logResult.isFailure) {
                    val message = logResult.exceptionOrNull()?.message ?: "Failed to save review log"
                    Log.e(TAG, "Failed to save review log for wordId=${word.id}: $message")
                    uiState = FlashcardUiState.Error(message)
                    return@launch
                }
                Log.d(TAG, "Saved review log to user_review_logs for wordId=${word.id}, rating=$ratingStr")

                if (rating == 0) {
                    isFlipped = false
                    Log.d(TAG, "Keeping wordId=${word.id} on current card after Again rating")
                    return@launch
                }

                if (currentIndex < currentWords.size - 1) {
                    currentIndex++
                    isFlipped = false
                } else {
                    uiState = FlashcardUiState.Finished
                }
            } finally {
                isSubmittingRating = false
            }
        }
    }

    /**
     * SM-2 Algorithm Implementation
     * rating: 0 (Again), 1 (Hard), 2 (Good), 3 (Easy)
     * Maps to quality: 0-5 in original SM-2. Here we simplify to 4 levels.
     */
    private fun calculateSM2(current: UserWordProgress, rating: Int): UserWordProgress {
        val q = when(rating) {
            0 -> 0 // Again
            1 -> 3 // Hard
            2 -> 4 // Good
            3 -> 5 // Easy
            else -> 3
        }

        var nextEaseFactor = current.easeFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        if (nextEaseFactor < 1.3f) nextEaseFactor = 1.3f

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

        val unit = com.edu.minlish.core.util.AppSettings.intervalUnit
        val calendar = Calendar.getInstance()
        when (unit) {
            "MINUTES" -> calendar.add(Calendar.MINUTE, nextInterval)
            "HOURS" -> calendar.add(Calendar.HOUR_OF_DAY, nextInterval)
            else -> calendar.add(Calendar.DAY_OF_YEAR, nextInterval)
        }

        val masteredThreshold = com.edu.minlish.core.util.AppSettings.masteredThreshold

        return current.copy(
            easeFactor = nextEaseFactor,
            interval = nextInterval,
            repetitions = nextRepetitions,
            nextReviewDate = calendar.time,
            lastReviewedAt = Date(),
            status = if (nextInterval > masteredThreshold) "mastered" else "reviewing"
        )
    }
}
