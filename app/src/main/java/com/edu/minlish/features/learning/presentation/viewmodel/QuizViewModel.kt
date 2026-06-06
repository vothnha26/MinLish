package com.edu.minlish.features.learning.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.core.util.AppSettings
import com.edu.minlish.features.auth.domain.repository.AuthRepository
import com.edu.minlish.features.learning.domain.model.QuestionType
import com.edu.minlish.features.learning.domain.model.QuizQuestion
import com.edu.minlish.features.learning.domain.repository.LearningRepository
import com.edu.minlish.features.learning.domain.usecase.BuildQuizQuestionsUseCase
import com.edu.minlish.features.learning.domain.usecase.UpdateWordProgressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch



class QuizViewModel(
    application: Application,
    private val repository: LearningRepository,
    private val authRepository: AuthRepository,
    private val updateProgressUseCase: UpdateWordProgressUseCase,
    private val buildQuestionsUseCase: BuildQuizQuestionsUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _selectedOptionIndex = MutableStateFlow<Int?>(null)
    val selectedOptionIndex: StateFlow<Int?> = _selectedOptionIndex.asStateFlow()

    private val _spellingInput = MutableStateFlow("")
    val spellingInput: StateFlow<String> = _spellingInput.asStateFlow()

    private val _isSpellingChecked = MutableStateFlow(false)
    val isSpellingChecked: StateFlow<Boolean> = _isSpellingChecked.asStateFlow()

    private val _isSpellingCorrect = MutableStateFlow(false)
    val isSpellingCorrect: StateFlow<Boolean> = _isSpellingCorrect.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _maxScore = MutableStateFlow(0)
    val maxScore: StateFlow<Int> = _maxScore.asStateFlow()

    // Matching Game state
    private val _selectedEnglishCard = MutableStateFlow<String?>(null)
    val selectedEnglishCard: StateFlow<String?> = _selectedEnglishCard.asStateFlow()

    private val _selectedVietnameseCard = MutableStateFlow<String?>(null)
    val selectedVietnameseCard: StateFlow<String?> = _selectedVietnameseCard.asStateFlow()

    private val _matchedEnglishCards = MutableStateFlow<List<String>>(emptyList())
    val matchedEnglishCards: StateFlow<List<String>> = _matchedEnglishCards.asStateFlow()

    private val _matchedVietnameseCards = MutableStateFlow<List<String>>(emptyList())
    val matchedVietnameseCards: StateFlow<List<String>> = _matchedVietnameseCards.asStateFlow()

    private val _matchingErrorPair = MutableStateFlow<Pair<String, String>?>(null)
    val matchingErrorPair: StateFlow<Pair<String, String>?> = _matchingErrorPair.asStateFlow()

    // Cache word progress for SM-2 updates after quiz
    private var wordProgressCache:
        Map<String, com.edu.minlish.features.learning.domain.model.UserWordProgress?> = emptyMap()

    init {
        AppSettings.init(application)
    }

    fun updateSpellingInput(value: String) {
        _spellingInput.update { value }
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
            _uiState.update { QuizUiState.Error("User not logged in") }
            return
        }

        viewModelScope.launch {
            _uiState.update { QuizUiState.Loading }
            resetSession()

            repository.getDueWords(currentUser.id, setId, forceAll = true)
                .onSuccess { dueWords ->
                    if (dueWords.isEmpty()) {
                        _uiState.update { QuizUiState.Error(
                            "Chưa có từ vựng nào để luyện tập. Hãy thêm từ vào bộ từ của bạn trước nhé!"
                        ) }
                        return@launch
                    }

                    // Cache progress map để dùng khi update SM-2
                    wordProgressCache = dueWords.associate { (word, progress) ->
                        word.id to progress
                    }

                    val finalQuestions = buildQuestionsUseCase(dueWords, modes, questionCount)

                    _maxScore.update { finalQuestions.sumOf { q ->
                        when (q.type) {
                            QuestionType.MATCHING -> q.matchingPairs.size
                            else -> 1
                        }
                    } }

                    _uiState.update { QuizUiState.Success(finalQuestions) }
                }
                .onFailure { e ->
                    _uiState.update { QuizUiState.Error(e.message ?: "Không thể tải câu hỏi.") }
                }
        }
    }

    fun selectOption(index: Int) {
        if (_selectedOptionIndex.value != null) return
        _selectedOptionIndex.update { index }

        val state = _uiState.value as? QuizUiState.Success ?: return
        val question = state.questions[_currentIndex.value]
        val isCorrect = index == question.correctIndex

        if (isCorrect) _score.update { it + 1 }
        recordAnswer(question, isCorrect)
    }

    fun checkSpellingAnswer() {
        if (_isSpellingChecked.value) return
        val state = _uiState.value as? QuizUiState.Success ?: return
        val question = state.questions[_currentIndex.value]

        val input = _spellingInput.value.trim().lowercase()
        val correct = question.word.word.trim().lowercase()
        _isSpellingCorrect.update { input == correct }
        _isSpellingChecked.update { true }

        if (_isSpellingCorrect.value) _score.update { it + 1 }
        recordAnswer(question, _isSpellingCorrect.value)
    }

    fun onCardClick(cardText: String, isEnglish: Boolean) {
        val state = _uiState.value as? QuizUiState.Success ?: return
        val question = state.questions[_currentIndex.value]
        _matchingErrorPair.update { null }

        if (isEnglish) {
            if (_matchedEnglishCards.value.contains(cardText)) return
            _selectedEnglishCard.update { if (it == cardText) null else cardText }
        } else {
            if (_matchedVietnameseCards.value.contains(cardText)) return
            _selectedVietnameseCard.update { if (it == cardText) null else cardText }
        }

        val eng = _selectedEnglishCard.value
        val viet = _selectedVietnameseCard.value
        if (eng != null && viet != null) {
            val matched = question.matchingPairs.any { it.first == eng && it.second == viet }
            if (matched) {
                _matchedEnglishCards.update { it + eng }
                _matchedVietnameseCards.update { it + viet }
                _selectedEnglishCard.update { null }
                _selectedVietnameseCard.update { null }
                _score.update { it + 1 }
            } else {
                _matchingErrorPair.update { Pair(eng, viet) }
                _selectedEnglishCard.update { null }
                _selectedVietnameseCard.update { null }
            }
        }
    }

    fun nextQuestion() {
        val state = _uiState.value as? QuizUiState.Success ?: return
        if (_currentIndex.value < state.questions.size - 1) {
            _currentIndex.update { it + 1 }
            _selectedOptionIndex.update { null }
            _spellingInput.update { "" }
            _isSpellingChecked.update { false }
            _isSpellingCorrect.update { false }
            resetMatchingState()
        } else {
            _uiState.update { QuizUiState.Finished(score = _score.value, maxScore = _maxScore.value) }
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
        _selectedEnglishCard.update { null }
        _selectedVietnameseCard.update { null }
        _matchedEnglishCards.update { emptyList() }
        _matchedVietnameseCards.update { emptyList() }
        _matchingErrorPair.update { null }
    }

    private fun resetSession() {
        _currentIndex.update { 0 }
        _selectedOptionIndex.update { null }
        _spellingInput.update { "" }
        _isSpellingChecked.update { false }
        _isSpellingCorrect.update { false }
        _score.update { 0 }
        _maxScore.update { 0 }
        wordProgressCache = emptyMap()
        resetMatchingState()
    }
}
