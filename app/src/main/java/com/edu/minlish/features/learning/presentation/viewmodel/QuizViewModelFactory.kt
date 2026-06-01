package com.edu.minlish.features.learning.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edu.minlish.features.auth.data.repository.FirebaseAuthRepositoryImpl
import com.edu.minlish.features.learning.data.repository.FirestoreLearningRepositoryImpl
import com.edu.minlish.features.learning.domain.usecase.BuildQuizQuestionsUseCase
import com.edu.minlish.features.learning.domain.usecase.UpdateWordProgressUseCase

/**
 * Factory để inject dependencies vào QuizViewModel.
 * ViewModel không tự new repository nữa — factory đảm nhiệm việc này.
 */
class QuizViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = FirestoreLearningRepositoryImpl()
        val authRepository = FirebaseAuthRepositoryImpl()

        val updateProgressUseCase = UpdateWordProgressUseCase(repository)
        val buildQuestionsUseCase = BuildQuizQuestionsUseCase()

        return QuizViewModel(
            application = application,
            repository = repository,
            authRepository = authRepository,
            updateProgressUseCase = updateProgressUseCase,
            buildQuestionsUseCase = buildQuestionsUseCase
        ) as T
    }
}
