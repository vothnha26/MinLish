package com.edu.minlish.features.learning.presentation.viewmodel

import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

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

    private val _uiState = MutableStateFlow<FlashcardUiState>(FlashcardUiState.Loading)
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    private val _isSubmittingRating = MutableStateFlow(false)
    val isSubmittingRating: StateFlow<Boolean> = _isSubmittingRating.asStateFlow()

    fun updateCurrentIndex(value: Int) { _currentIndex.update { value } }
    fun updateIsFlipped(value: Boolean) { _isFlipped.update { value } }
    fun updateIsSubmittingRating(value: Boolean) { _isSubmittingRating.update { value } }

    fun loadWords(setId: String?, forceAll: Boolean = false) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.update { FlashcardUiState.Error("User not logged in") }
            return
        }

        viewModelScope.launch {
            _uiState.update { FlashcardUiState.Loading }
            
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
                        _uiState.update { FlashcardUiState.Finished }
                    } else {
                        _uiState.update { FlashcardUiState.Success(dueWords) }
                        _currentIndex.update { 0 }
                    }
                }
                .onFailure { e ->
                    _uiState.update { FlashcardUiState.Error(e.message ?: "Failed to load words") }
                }
        }
    }

    fun onFlip() {
        _isFlipped.update { !it }
    }

    fun submitRating(rating: Int) {
        if (_isSubmittingRating.value) return

        val currentWords = (uiState.value as? FlashcardUiState.Success)?.words ?: return
        val (word, progress) = currentWords[_currentIndex.value]
        val currentUser = authRepository.getCurrentUser() ?: return

        viewModelScope.launch {
            _isSubmittingRating.update { true }
            try {
                val updatedProgress = com.edu.minlish.core.util.SpacedRepetitionUtil.calculateSM2ForRating(progress ?: UserWordProgress(userId = currentUser.id, wordId = word.id, setId = word.vocabularySetId), rating)
                val progressResult = repository.updateProgress(updatedProgress)
                if (progressResult.isFailure) {
                    val message = progressResult.exceptionOrNull()?.message ?: "Failed to update progress"
                    _uiState.update { FlashcardUiState.Error(message) }
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
                    _uiState.update { FlashcardUiState.Error(message) }
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
                    _isFlipped.update { false }
                    Log.d(TAG, "Keeping wordId=${word.id} on current card after Again rating")
                    return@launch
                }

                if (_currentIndex.value < currentWords.size - 1) {
                    _currentIndex.update { it + 1 }
                    _isFlipped.update { false }
                } else {
                    _uiState.update { FlashcardUiState.Finished }
                }
            } finally {
                _isSubmittingRating.update { false }
            }
        }
    }
}
