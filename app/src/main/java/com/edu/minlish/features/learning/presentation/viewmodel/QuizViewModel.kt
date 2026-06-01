package com.edu.minlish.features.learning.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.core.util.AppSettings
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.learning.domain.model.QuestionType
import com.edu.minlish.features.learning.domain.model.QuizQuestion
import com.edu.minlish.features.learning.domain.repository.LearningRepository
import com.edu.minlish.features.learning.domain.usecase.BuildQuizQuestionsUseCase
import com.edu.minlish.features.learning.domain.usecase.UpdateWordProgressUseCase
import kotlinx.coroutines.launch

class QuizViewModel(
    application: Application,
    private val repository: LearningRepository,
    private val authRepository: AuthRepository,
    private val updateProgressUseCase: UpdateWordProgressUseCase,
    private val buildQuestionsUseCase: BuildQuizQuestionsUseCase
) : AndroidViewModel(application) {

    var uiState by mutableStateOf<QuizUiState>(QuizUiState.Loading)
        private set

    var currentIndex by mutableStateOf(0)
        private set

    var selectedOptionIndex by mutableStateOf<Int?>(null)
        private set

    var spellingInput by mutableStateOf("")

    var isSpellingChecked by mutableStateOf(false)
        private set

    var isSpellingCorrect by mutableStateOf(false)
        private set

    var score by mutableStateOf(0)
        private set

    var maxScore by mutableStateOf(0)
        private set

    // Matching Game state
    var selectedEnglishCard by mutableStateOf<String?>(null)
        private set

    var selectedVietnameseCard by mutableStateOf<String?>(null)
        private set

    val matchedEnglishCards = mutableStateListOf<String>()
    val matchedVietnameseCards = mutableStateListOf<String>()

    var matchingErrorPair by mutableStateOf<Pair<String, String>?>(null)
        private set

    // Cache word progress for SM-2 updates after quiz
    private var wordProgressCache:
        Map<String, com.edu.minlish.features.learning.domain.model.UserWordProgress?> = emptyMap()

    init {
        AppSettings.init(application)
    }

    /** Đổi intervalUnit (DAYS/HOURS/MINUTES) sang milliseconds. */
    private fun intervalUnitMs(): Long = when (AppSettings.intervalUnit) {
        "MINUTES" -> 60L * 1000
        "HOURS"   -> 60L * 60 * 1000
        else      -> 24L * 60 * 60 * 1000  // DAYS (mặc định)
    }

    fun loadQuiz(setId: String?, modes: String = "MULTIPLE_CHOICE", questionCount: Int = 10) {

        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            uiState = QuizUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            uiState = QuizUiState.Loading
            resetSession()

            repository.getDueWords(currentUser.id, setId, forceAll = true)
                .onSuccess { dueWords ->
                    if (dueWords.isEmpty()) {
                        uiState = QuizUiState.Error(
                            "Chưa có từ vựng nào để luyện tập. Hãy thêm từ vào bộ từ của bạn trước nhé!"
                        )
                        return@launch
                    }

                    // Cache progress map để dùng khi update SM-2
                    wordProgressCache = dueWords.associate { (word, progress) ->
                        word.id to progress
                    }

                    val finalQuestions = buildQuestionsUseCase(dueWords, modes, questionCount)

                    maxScore = finalQuestions.sumOf { q ->
                        when (q.type) {
                            QuestionType.MATCHING -> q.matchingPairs.size
                            else -> 1
                        }
                    }

                    uiState = QuizUiState.Success(finalQuestions)
                }
                .onFailure { e ->
                    uiState = QuizUiState.Error(e.message ?: "Không thể tải câu hỏi.")
                }
        }
    }

    fun selectOption(index: Int) {
        if (selectedOptionIndex != null) return
        selectedOptionIndex = index

        val state = uiState as? QuizUiState.Success ?: return
        val question = state.questions[currentIndex]
        val isCorrect = index == question.correctIndex

        if (isCorrect) score++
        recordAnswer(question, isCorrect)
    }

    fun checkSpellingAnswer() {
        if (isSpellingChecked) return
        val state = uiState as? QuizUiState.Success ?: return
        val question = state.questions[currentIndex]

        val input = spellingInput.trim().lowercase()
        val correct = question.word.word.trim().lowercase()
        isSpellingCorrect = (input == correct)
        isSpellingChecked = true

        if (isSpellingCorrect) score++
        recordAnswer(question, isSpellingCorrect)
    }

    fun onCardClick(cardText: String, isEnglish: Boolean) {
        val state = uiState as? QuizUiState.Success ?: return
        val question = state.questions[currentIndex]
        matchingErrorPair = null

        if (isEnglish) {
            if (matchedEnglishCards.contains(cardText)) return
            selectedEnglishCard = if (selectedEnglishCard == cardText) null else cardText
        } else {
            if (matchedVietnameseCards.contains(cardText)) return
            selectedVietnameseCard = if (selectedVietnameseCard == cardText) null else cardText
        }

        val eng = selectedEnglishCard
        val viet = selectedVietnameseCard
        if (eng != null && viet != null) {
            val matched = question.matchingPairs.any { it.first == eng && it.second == viet }
            if (matched) {
                matchedEnglishCards.add(eng)
                matchedVietnameseCards.add(viet)
                selectedEnglishCard = null
                selectedVietnameseCard = null
                score++
            } else {
                matchingErrorPair = Pair(eng, viet)
                selectedEnglishCard = null
                selectedVietnameseCard = null
            }
        }
    }

    fun nextQuestion() {
        val state = uiState as? QuizUiState.Success ?: return
        if (currentIndex < state.questions.size - 1) {
            currentIndex++
            selectedOptionIndex = null
            spellingInput = ""
            isSpellingChecked = false
            isSpellingCorrect = false
            resetMatchingState()
        } else {
            uiState = QuizUiState.Finished(score = score, maxScore = maxScore)
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private fun recordAnswer(question: QuizQuestion, correct: Boolean) {
        val user = authRepository.getCurrentUser() ?: return
        val existingProgress = wordProgressCache[question.word.id]
        viewModelScope.launch {
            updateProgressUseCase(
                existing = existingProgress,
                userId = user.id,
                wordId = question.word.id,
                setId = question.word.vocabularySetId,
                correct = correct,
                intervalUnitMs = intervalUnitMs(),
                masteredThreshold = AppSettings.masteredThreshold
            )
        }
    }

    private fun resetMatchingState() {
        selectedEnglishCard = null
        selectedVietnameseCard = null
        matchedEnglishCards.clear()
        matchedVietnameseCards.clear()
        matchingErrorPair = null
    }

    private fun resetSession() {
        currentIndex = 0
        selectedOptionIndex = null
        spellingInput = ""
        isSpellingChecked = false
        isSpellingCorrect = false
        score = 0
        maxScore = 0
        wordProgressCache = emptyMap()
        resetMatchingState()
    }
}
