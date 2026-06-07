package com.edu.minlish.features.library.presentation.viewmodel

import com.edu.minlish.features.library.data.DictionaryEntry
import com.edu.minlish.features.library.domain.model.Category
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.repository.LookupStrategy
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslateAndLookupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeRepository: FakeVocabularyRepository
    private lateinit var fakeLookupStrategy: FakeLookupStrategy
    private lateinit var viewModel: TranslateAndLookupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeRepository = FakeVocabularyRepository()
        fakeLookupStrategy = FakeLookupStrategy()

        // Khởi tạo một số bộ từ giả lập
        fakeRepository.sets.add(
            VocabularySet(
                id = "set_123",
                title = "IELTS Vocabulary",
                creatorId = "test_user_id",
                wordCount = 5
            )
        )

        viewModel = TranslateAndLookupViewModel(
            repository = fakeRepository,
            lookupStrategy = fakeLookupStrategy,
            getUserId = { "test_user_id" }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadUserSets_LoadsFromRepository() = runTest(testDispatcher) {
        // GIVEN: Fake repository has sets

        // WHEN: View model initializes and we wait for sets load (done in init)
        advanceUntilIdle()

        // THEN: VM userSets should load the correct sets
        assertEquals(1, viewModel.userSets.value.size)
        assertEquals("IELTS Vocabulary", viewModel.userSets.value[0].title)
    }

    @Test
    fun testLookupWord_Success() = runTest(testDispatcher) {
        // GIVEN: User inputs word to search
        viewModel.updateInputText("outstanding")

        // WHEN: Calling lookupWord
        viewModel.lookupWord()
        
        // THEN: UI state is Success and lookupResult is populated
        assertTrue(viewModel.uiState.value is TranslateAndLookupUiState.Success)
        assertNotNull(viewModel.lookupResult.value)
        assertEquals("outstanding", viewModel.lookupResult.value?.word)
        assertEquals("/outstanding/", viewModel.lookupResult.value?.pronunciation)
        assertEquals("Nghĩa giả lập", viewModel.lookupResult.value?.definitions?.firstOrNull()?.meaningVietnamese)
    }

    @Test
    fun testQuickAddWord_ToSpecificSet_Success() = runTest(testDispatcher) {
        // GIVEN: Word to save and target specific set ID
        val word = VocabularyWord(
            word = "magnificent",
            pronunciation = "/mæɡˈnɪf.ɪ.sənt/",
            definitions = listOf(WordDefinition(pos = "Adj", meaningVietnamese = "Tuyệt vời"))
        )
        val targetSetId = "set_123"

        // WHEN: Adding word
        var callbackSuccess = false
        viewModel.quickAddWord(word, targetSetId) { result ->
            if (result.isSuccess) {
                callbackSuccess = true
            }
        }
        advanceUntilIdle()

        // THEN: Word added to repository under the specified set ID
        assertTrue(callbackSuccess)
        assertEquals(1, fakeRepository.words.size)
        assertEquals("magnificent", fakeRepository.words[0].word)
        assertEquals("set_123", fakeRepository.words[0].vocabularySetId)
        assertTrue(viewModel.wordSavedStatus.value[word.word] == true)
    }

    @Test
    fun testQuickAddWord_ToDefaultSet_CreatesSetAndSaves() = runTest(testDispatcher) {
        // GIVEN: Target set ID is null, repository does not have "Quick Notes" set yet
        val word = VocabularyWord(
            word = "remarkable",
            pronunciation = "/rɪˈmɑː.kə.bəl/",
            definitions = listOf(WordDefinition(pos = "Adj", meaningVietnamese = "Đáng chú ý"))
        )

        // WHEN: Adding word to default
        var callbackSuccess = false
        viewModel.quickAddWord(word, null) { result ->
            if (result.isSuccess) {
                callbackSuccess = true
            }
        }
        advanceUntilIdle()

        // THEN: Default set "Quick Notes" should be created, and word saved in it
        assertTrue(callbackSuccess)
        
        // Assert set created
        val createdDefaultSet = fakeRepository.sets.find { it.title.equals("Quick Notes", ignoreCase = true) }
        assertNotNull(createdDefaultSet)
        
        // Assert word saved to that created set ID
        assertEquals(1, fakeRepository.words.size)
        assertEquals("remarkable", fakeRepository.words[0].word)
        assertEquals(createdDefaultSet?.id, fakeRepository.words[0].vocabularySetId)
        assertTrue(viewModel.wordSavedStatus.value[word.word] == true)
    }

    @Test
    fun testSwapLanguages_SwapsSuccessfully() = runTest(testDispatcher) {
        // GIVEN: Set input texts
        viewModel.updateInputText("Hello")
        viewModel.updateTranslatedText("Xin chào")

        // WHEN: Swapping
        viewModel.swapLanguages()

        // THEN: Languages, codes, and texts should be swapped
        assertEquals("Tiếng Việt", viewModel.sourceLang.value)
        assertEquals("vi", viewModel.sourceLangCode.value)
        assertEquals("Tiếng Anh", viewModel.targetLang.value)
        assertEquals("en", viewModel.targetLangCode.value)
        assertEquals("Xin chào", viewModel.inputText.value)
        assertEquals("Hello", viewModel.translatedText.value)
    }

    @Test
    fun testAddWordToHistory_LimitsAndRemovesDuplicates() = runTest(testDispatcher) {
        // GIVEN: Clear history first
        viewModel.clearRecentHistory()

        // WHEN: Adding multiple words, including duplicates and exceeding 5 items
        viewModel.addWordToHistory("hello")
        viewModel.addWordToHistory("world")
        viewModel.addWordToHistory("hello") // Duplicate - should move to index 0
        viewModel.addWordToHistory("apple")
        viewModel.addWordToHistory("banana")
        viewModel.addWordToHistory("orange")
        viewModel.addWordToHistory("grapes") // Exceeds 5 items (hello will be dropped if it falls to index 5+)

        // THEN: Limit should be 5, no duplicates, most recent at index 0
        assertEquals(5, viewModel.recentHistory.value.size)
        assertEquals("grapes", viewModel.recentHistory.value[0])
        assertEquals("orange", viewModel.recentHistory.value[1])
        assertEquals("banana", viewModel.recentHistory.value[2])
        assertEquals("apple", viewModel.recentHistory.value[3])
        assertEquals("hello", viewModel.recentHistory.value[4]) // "world" got pushed out
        assertFalse(viewModel.recentHistory.value.contains("world"))
    }

    @Test
    fun testClearRecentHistory_ClearsSuccessfully() = runTest(testDispatcher) {
        // GIVEN: History with items
        viewModel.addWordToHistory("hello")
        assertEquals(1, viewModel.recentHistory.value.size)

        // WHEN: Clearing
        viewModel.clearRecentHistory()

        // THEN: History is empty
        assertTrue(viewModel.recentHistory.value.isEmpty())
    }
}

// --- Fake Implementation Classes for Test ---

class FakeVocabularyRepository : VocabularyRepository {
    val sets = mutableListOf<VocabularySet>()
    val words = mutableListOf<VocabularyWord>()
    var createSetCount = 0
    var addWordCount = 0

    override suspend fun createSet(set: VocabularySet): Result<Unit> {
        sets.add(set)
        createSetCount++
        return Result.success(Unit)
    }

    override suspend fun createSetAndGetId(set: VocabularySet): Result<String> {
        val id = "generated_set_${sets.size + 1}"
        sets.add(set.copy(id = id))
        createSetCount++
        return Result.success(id)
    }

    override suspend fun getUserSets(userId: String): Result<List<VocabularySet>> {
        return Result.success(sets)
    }

    override suspend fun getSetById(setId: String): Result<VocabularySet> {
        val s = sets.find { it.id == setId }
        return if (s != null) Result.success(s) else Result.failure(Exception("Not found"))
    }

    override suspend fun updateSet(set: VocabularySet): Result<Unit> = Result.success(Unit)
    override suspend fun deleteSet(setId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getCategories(userId: String): Result<List<Category>> = Result.success(emptyList())
    override suspend fun addCategory(category: Category): Result<Unit> = Result.success(Unit)
    override suspend fun updateCategory(category: Category): Result<Unit> = Result.success(Unit)
    override suspend fun deleteCategory(categoryId: String): Result<Unit> = Result.success(Unit)

    override suspend fun fetchWordDetails(word: String): Result<List<DictionaryEntry>> = Result.success(emptyList())

    override suspend fun addWord(word: VocabularyWord): Result<Unit> {
        words.add(word)
        addWordCount++
        return Result.success(Unit)
    }

    override suspend fun importWords(set: VocabularySet, words: List<VocabularyWord>): Result<Unit> = Result.success(Unit)
    override suspend fun updateWord(word: VocabularyWord): Result<Unit> = Result.success(Unit)
    override suspend fun deleteWord(wordId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getWordsBySet(setId: String): Result<List<VocabularyWord>> {
        return Result.success(words.filter { it.vocabularySetId == setId })
    }
    override suspend fun getWordById(wordId: String): Result<VocabularyWord> {
        val w = words.find { it.id == wordId }
        return if (w != null) Result.success(w) else Result.failure(Exception("Not found"))
    }
}

class FakeLookupStrategy : LookupStrategy {
    var lastWord = ""
    override suspend fun lookupWord(word: String): Result<VocabularyWord> {
        lastWord = word
        return Result.success(
            VocabularyWord(
                word = word,
                pronunciation = "/$word/",
                definitions = listOf(
                    WordDefinition(
                        pos = "Noun",
                        meaningVietnamese = "Nghĩa giả lập",
                        definitionEnglish = "English definition",
                        exampleSentence = "Example sentence"
                    )
                )
            )
        )
    }
}
