package com.edu.minlish.features.learning.domain.model

import com.edu.minlish.features.library.domain.model.VocabularyWord

enum class QuestionType {
    MULTIPLE_CHOICE,
    SPELLING,
    MATCHING
}

data class QuizQuestion(
    val type: QuestionType,
    val word: VocabularyWord,
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val matchingPairs: List<Pair<String, String>> = emptyList()
)
