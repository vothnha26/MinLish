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
                var targetNew = com.edu.minlish.core.util.AppSettings.dailyNewWordsTarget
                var targetReview = 20
                
                profileRepository.getProfile(currentUser.id).onSuccess { profile ->
                    if (profile != null) {
                        com.edu.minlish.core.util.AppSettings.dailyNewWordsTarget = profile.dailyNewWordsTarget
                        targetNew = com.edu.minlish.core.util.AppSettings.dailyNewWordsTarget
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
                val updatedProgress = com.edu.minlish.core.util.SpacedRepetitionUtil.calculateSM2ForRating(progress ?: UserWordProgress(userId = currentUser.id, wordId = word.id, setId = word.vocabularySetId), rating)
                val progressResult = repository.updateProgress(updatedProgress)
                if (progressResult.isFailure) {
                    val message = progressResult.exceptionOrNull()?.message ?: "Failed to update progress"
//                    Log.e(TAG, "Failed to update progress for wordId=${word.id}: $message")
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

                // ✅ Cập nhật local cache ngay lập tức — không chờ Firestore listener round-trip
                // HomeViewModel đọc từ SessionDataManager, nên Home sẽ hiển thị đúng ngay khi quay lại.
                val sessionCache = com.edu.minlish.core.util.SessionDataManager
                val currentLogs = sessionCache.userReviewLogs?.toMutableList() ?: mutableListOf()
                currentLogs.add(log)
                sessionCache.userReviewLogs = currentLogs

                val currentProgresses = sessionCache.userWordProgresses?.toMutableList() ?: mutableListOf()
                val existingIndex = currentProgresses.indexOfFirst { it.wordId == updatedProgress.wordId }
                if (existingIndex >= 0) {
                    currentProgresses[existingIndex] = updatedProgress
                } else {
                    currentProgresses.add(updatedProgress)
                }
                sessionCache.userWordProgresses = currentProgresses

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


}
