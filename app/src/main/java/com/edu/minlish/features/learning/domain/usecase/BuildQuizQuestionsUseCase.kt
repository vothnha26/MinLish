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
        
        // Trộn ngẫu nhiên danh sách từ đầu vào
        val shuffledWords = wordsList.shuffled()
        
        // Các giỏ chứa từ vựng cho từng loại câu hỏi
        val matchingWords = mutableListOf<VocabularyWord>()
        val mcWords = mutableListOf<VocabularyWord>()
        val spellingWords = mutableListOf<VocabularyWord>()
        
        // Phân phối ngẫu nhiên từ vựng vào các giỏ dựa trên allowedTypes
        shuffledWords.forEach { word ->
            val selectedType = allowedTypes.random()
            when (selectedType) {
                QuestionType.MATCHING -> matchingWords.add(word)
                QuestionType.MULTIPLE_CHOICE -> mcWords.add(word)
                QuestionType.SPELLING -> spellingWords.add(word)
            }
        }
        
        // Xử lý từ thừa của Matching: Matching yêu cầu từng nhóm 4 từ
        val matchingChunksCount = matchingWords.size / 4
        val matchedCount = matchingChunksCount * 4
        val leftovers = matchingWords.subList(matchedCount, matchingWords.size).toList()
        
        // Giữ lại số lượng từ chẵn chia hết cho 4 cho Matching
        val finalMatchingWords = matchingWords.subList(0, matchedCount).toMutableList()
        
        // Phân phối số từ thừa sang các giỏ khác
        val otherTypes = allowedTypes.filter { it != QuestionType.MATCHING }
        leftovers.forEach { word ->
            if (otherTypes.isNotEmpty()) {
                val nextType = otherTypes.random()
                if (nextType == QuestionType.SPELLING) {
                    spellingWords.add(word)
                } else {
                    mcWords.add(word)
                }
            } else {
                // Fallback mặc định
                mcWords.add(word)
            }
        }
        
        // 1. Tạo câu hỏi Matching (nối cặp)
        finalMatchingWords.chunked(4).forEach { chunk ->
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
        
        // Lấy tất cả nghĩa tiếng Việt để làm distractor (phương án nhiễu) cho trắc nghiệm
        val allMeanings = wordsList.mapNotNull { w ->
            w.definitions.firstOrNull { it.meaningVietnamese.isNotBlank() }?.meaningVietnamese
        }.distinct()
        
        // 2. Tạo câu hỏi Multiple Choice (Trắc nghiệm)
        mcWords.forEach { word ->
            val primaryMeaning = word.definitions
                .firstOrNull { it.meaningVietnamese.isNotBlank() }
                ?.meaningVietnamese ?: "Nghĩa của từ"
            
            val distractors = (allMeanings.filter { it != primaryMeaning }.shuffled() + fallbackDistractors)
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
        
        // 3. Tạo câu hỏi Spelling (Gõ chữ cái)
        spellingWords.forEach { word ->
            questionsList.add(
                QuizQuestion(type = QuestionType.SPELLING, word = word)
            )
        }
        
        return questionsList.shuffled().take(questionCount)
    }
}
