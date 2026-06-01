package com.edu.minlish.features.learning.domain.usecase

import com.edu.minlish.features.learning.domain.model.QuestionType
import com.edu.minlish.features.learning.domain.model.QuizQuestion
import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.edu.minlish.features.library.domain.model.VocabularyWord

/**
 * UseCase: Tạo danh sách câu hỏi quiz từ danh sách từ vựng.
 * Pure logic — không phụ thuộc platform hay framework.
 */
class BuildQuizQuestionsUseCase {

    private val fallbackDistractors = listOf(
        "Trì hoãn, chậm trễ",
        "Thực tế, thực dụng",
        "Phù du, chóng vánh",
        "Đồng tình, nhất trí",
        "Kiên nhẫn, bền bỉ",
        "Tự phát, tự nhiên",
        "Sáng sủa, thông minh",
        "Bao quát, toàn diện",
        "Nghi ngờ, hoài nghi",
        "Hào phóng, rộng rãi"
    )

    operator fun invoke(
        words: List<Pair<VocabularyWord, UserWordProgress?>>,
        modes: String = "MULTIPLE_CHOICE",
        questionCount: Int = 10
    ): List<QuizQuestion> {
        val wordsList = words.map { it.first }

        val allowedTypes = modes.split(",")
            .mapNotNull { token ->
                try { QuestionType.valueOf(token.trim()) } catch (e: Exception) { null }
            }
            .ifEmpty { listOf(QuestionType.MULTIPLE_CHOICE) }

        val questionsList = mutableListOf<QuizQuestion>()
        val remainingWords = wordsList.toMutableList()

        // 1. Generate Matching questions first (needs groups of 4)
        if (allowedTypes.contains(QuestionType.MATCHING)) {
            while (remainingWords.size >= 4) {
                val chunk = remainingWords.take(4)
                remainingWords.removeAll(chunk.toSet())

                val pairs = chunk.map { w ->
                    Pair(
                        w.word,
                        w.definitions.firstOrNull { it.meaningVietnamese.isNotBlank() }
                            ?.meaningVietnamese ?: "Nghĩa của từ"
                    )
                }

                questionsList.add(
                    QuizQuestion(
                        type = QuestionType.MATCHING,
                        word = chunk.first(),
                        matchingPairs = pairs
                    )
                )
            }
        }

        // 2. Generate MC / Spelling for remaining words
        val otherTypes = allowedTypes
            .filter { it != QuestionType.MATCHING }
            .ifEmpty { listOf(QuestionType.MULTIPLE_CHOICE) }

        remainingWords.forEach { word ->
            val questionType = otherTypes.random()
            val primaryMeaning = word.definitions
                .firstOrNull { it.meaningVietnamese.isNotBlank() }
                ?.meaningVietnamese ?: "Nghĩa của từ"

            when (questionType) {
                QuestionType.MULTIPLE_CHOICE -> {
                    val distractors = (
                        wordsList
                            .filter { it.id != word.id }
                            .mapNotNull { w ->
                                w.definitions.firstOrNull { it.meaningVietnamese.isNotBlank() }
                                    ?.meaningVietnamese
                            }
                            .distinct()
                            .shuffled() + fallbackDistractors
                    )
                        .filter { it != primaryMeaning }
                        .distinct()
                        .take(3)

                    val allOptions = (distractors + primaryMeaning).shuffled()
                    questionsList.add(
                        QuizQuestion(
                            type = QuestionType.MULTIPLE_CHOICE,
                            word = word,
                            options = allOptions,
                            correctIndex = allOptions.indexOf(primaryMeaning)
                        )
                    )
                }
                QuestionType.SPELLING -> {
                    questionsList.add(
                        QuizQuestion(type = QuestionType.SPELLING, word = word)
                    )
                }
                QuestionType.MATCHING -> { /* Already handled */ }
            }
        }

        return questionsList.shuffled().take(questionCount)
    }
}
