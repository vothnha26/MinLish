package com.edu.minlish.features.learning.presentation.viewmodel

import com.edu.minlish.features.learning.domain.model.QuizQuestion

sealed class QuizUiState {
    object Loading : QuizUiState()
    data class Success(val questions: List<QuizQuestion>) : QuizUiState()
    data class Finished(val score: Int, val maxScore: Int) : QuizUiState()
    data class Error(val message: String) : QuizUiState()
}
