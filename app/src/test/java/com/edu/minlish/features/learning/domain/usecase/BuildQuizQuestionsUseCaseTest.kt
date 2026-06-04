package com.edu.minlish.features.learning.domain.usecase

import com.edu.minlish.features.learning.domain.model.QuestionType
import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

class BuildQuizQuestionsUseCaseTest {

    private lateinit var useCase: BuildQuizQuestionsUseCase
    private lateinit var mockWords: List<Pair<VocabularyWord, UserWordProgress?>>

    @Before
    fun setUp() {
        useCase = BuildQuizQuestionsUseCase()
        
        // Tạo danh sách 12 từ vựng giả lập để test
        mockWords = (1..12).map { index ->
            val word = VocabularyWord(
                id = "word_$index",
                vocabularySetId = "set_1",
                word = "Word$index",
                pronunciation = "/wɜːd$index/",
                definitions = listOf(
                    WordDefinition(
                        pos = "Noun",
                        meaningVietnamese = "Nghĩa Tiếng Việt $index",
                        definitionEnglish = "English definition $index",
                        exampleSentence = "Example sentence $index"
                    )
                )
            )
            Pair(word, null)
        }
    }

    @Test
    fun testMultipleChoiceOnly() {
        // GIVEN: Chỉ cho phép chế độ MULTIPLE_CHOICE
        val modes = "MULTIPLE_CHOICE"
        
        // WHEN: Gọi UseCase
        val questions = useCase(mockWords, modes, questionCount = 10)
        
        // THEN:
        assertFalse(questions.isEmpty())
        assertTrue(questions.size <= 10)
        questions.forEach { question ->
            assertEquals(QuestionType.MULTIPLE_CHOICE, question.type)
            assertNotNull(question.options)
            assertEquals(4, question.options!!.size)
            assertTrue(question.correctIndex in 0..3)
            assertEquals(
                question.word.definitions.first().meaningVietnamese,
                question.options!![question.correctIndex]
            )
        }
    }

    @Test
    fun testSpellingOnly() {
        // GIVEN: Chỉ cho phép chế độ SPELLING
        val modes = "SPELLING"
        
        // WHEN: Gọi UseCase
        val questions = useCase(mockWords, modes, questionCount = 10)
        
        // THEN:
        assertFalse(questions.isEmpty())
        assertTrue(questions.size <= 10)
        questions.forEach { question ->
            assertEquals(QuestionType.SPELLING, question.type)
        }
    }

    @Test
    fun testMatchingOnly_EnoughWords() {
        // GIVEN: Chỉ cho phép MATCHING và truyền vào đúng 8 từ (đủ chia hết cho 4)
        val modes = "MATCHING"
        val eightWords = mockWords.take(8)
        
        // WHEN: Gọi UseCase
        val questions = useCase(eightWords, modes, questionCount = 10)
        
        // THEN:
        // Cứ 4 từ sẽ gom lại thành 1 câu hỏi Matching. 8 từ = 2 câu hỏi Matching.
        assertEquals(2, questions.size)
        questions.forEach { question ->
            assertEquals(QuestionType.MATCHING, question.type)
            assertEquals(4, question.matchingPairs.size)
        }
    }

    @Test
    fun testMatchingOnly_NotEnoughWords() {
        // GIVEN: Chỉ cho phép MATCHING nhưng chỉ có 3 từ (không đủ 4 từ để ghép cặp)
        val modes = "MATCHING"
        val threeWords = mockWords.take(3)
        
        // WHEN: Gọi UseCase
        val questions = useCase(threeWords, modes, questionCount = 10)
        
        // THEN:
        // Vì không đủ 4 từ nên Matching không thể gom nhóm. Các từ dư sẽ được chuyển sang giỏ Multiple Choice.
        assertEquals(3, questions.size)
        questions.forEach { question ->
            assertEquals(QuestionType.MULTIPLE_CHOICE, question.type)
        }
    }

    @Test
    fun testMatchingOnly_WithLeftovers() {
        // GIVEN: Chỉ cho phép MATCHING và truyền 6 từ (dư 2 từ sau khi chia cho 4)
        val modes = "MATCHING"
        val sixWords = mockWords.take(6)
        
        // WHEN: Gọi UseCase
        val questions = useCase(sixWords, modes, questionCount = 10)
        
        // THEN:
        // 4 từ đầu tạo thành 1 câu hỏi Matching. 2 từ thừa tạo thành 2 câu hỏi Multiple Choice.
        // Tổng số câu hỏi = 1 (Matching) + 2 (MC) = 3 câu hỏi.
        assertEquals(3, questions.size)
        
        val matchingCount = questions.count { it.type == QuestionType.MATCHING }
        val mcCount = questions.count { it.type == QuestionType.MULTIPLE_CHOICE }
        
        assertEquals(1, matchingCount)
        assertEquals(2, mcCount)
    }

    @Test
    fun testMixModes_BalancedDistribution() {
        // GIVEN: Cho phép cả 3 chế độ (MC, Spelling, Matching) với 12 từ
        val modes = "MULTIPLE_CHOICE,SPELLING,MATCHING"
        
        // WHEN: Gọi UseCase
        val questions = useCase(mockWords, modes, questionCount = 12)
        
        // THEN:
        // Thuật toán mix ngẫu nhiên phân bổ đều từ vựng vào 3 giỏ.
        // Đảm bảo không xảy ra tình trạng Matching nuốt chửng toàn bộ từ vựng như trước.
        assertFalse(questions.isEmpty())
        
        val hasMatching = questions.any { it.type == QuestionType.MATCHING }
        val hasMC = questions.any { it.type == QuestionType.MULTIPLE_CHOICE }
        val hasSpelling = questions.any { it.type == QuestionType.SPELLING }
        
        // Kiểm tra xem có sự pha trộn giữa các loại (ít nhất có 2 trong 3 loại xuất hiện)
        val activeTypesCount = listOf(hasMatching, hasMC, hasSpelling).count { it }
        assertTrue("Các dạng câu hỏi chưa được mix đều", activeTypesCount >= 2)
    }

    @Test
    fun testQuestionCountLimit() {
        // GIVEN: Có 12 từ vựng, yêu cầu chỉ lấy 5 câu hỏi
        val modes = "MULTIPLE_CHOICE,SPELLING"
        
        // WHEN: Gọi UseCase
        val questions = useCase(mockWords, modes, questionCount = 5)
        
        // THEN:
        // Số câu hỏi trả về phải bằng đúng questionCount giới hạn
        assertEquals(5, questions.size)
    }
}
