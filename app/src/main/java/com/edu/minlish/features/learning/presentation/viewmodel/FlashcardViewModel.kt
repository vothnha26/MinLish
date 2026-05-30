package com.edu.minlish.features.learning.presentation.viewmodel

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
import com.edu.minlish.features.library.domain.model.VocabularyWord
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
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf<FlashcardUiState>(FlashcardUiState.Loading)
        private set

    var currentIndex by mutableStateOf(0)
    var isFlipped by mutableStateOf(false)

    fun loadWords(setId: String?, forceAll: Boolean = false) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = FlashcardUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            uiState = FlashcardUiState.Loading
            repository.getDueWords(currentUser.id, setId, forceAll)
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
        val currentWords = (uiState as? FlashcardUiState.Success)?.words ?: return
        val (word, progress) = currentWords[currentIndex]
        val currentUser = authRepository.getCurrentUser() ?: return

        viewModelScope.launch {
            val updatedProgress = calculateSM2(progress ?: UserWordProgress(userId = currentUser.id, wordId = word.id, setId = word.vocabularySetId), rating)
            repository.updateProgress(updatedProgress)

            if (currentIndex < currentWords.size - 1) {
                currentIndex++
                isFlipped = false
            } else {
                uiState = FlashcardUiState.Finished
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
