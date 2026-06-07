package com.edu.minlish.features.library.data.repository

import com.edu.minlish.core.ai.GeminiAIService
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiLookupStrategyTest {

    private lateinit var fakeCacheMap: MutableMap<String, String>
    private lateinit var fakeGeminiAIService: FakeGeminiAIService
    private lateinit var lookupStrategy: GeminiLookupStrategy

    @Before
    fun setUp() {
        fakeCacheMap = mutableMapOf()
        fakeGeminiAIService = FakeGeminiAIService()
        lookupStrategy = GeminiLookupStrategy(
            geminiService = fakeGeminiAIService,
            getCachedJson = { fakeCacheMap[it] },
            cacheWordJson = { w, j -> fakeCacheMap[w] = j }
        )
    }

    @Test
    fun testLookupWord_WhenNotCached_CallsAIAndCachesResult() = runTest {
        val word = "hello"
        val aiResponseJson = """
            {
              "word": "hello",
              "pronunciation": "/həˈloʊ/",
              "definitions": [
                {
                  "pos": "exclamation",
                  "meaningVietnamese": "xin chào",
                  "definitionEnglish": "used as a greeting",
                  "exampleSentence": "Hello, how are you?",
                  "synonyms": ["hi"],
                  "antonyms": []
                }
              ],
              "collocations": "",
              "personalNote": ""
            }
        """.trimIndent()
        fakeGeminiAIService.responseResult = Result.success(aiResponseJson)

        val result = lookupStrategy.lookupWord(word)

        assertTrue(result.isSuccess)
        val vocabularyWord = result.getOrNull()
        assertNotNull(vocabularyWord)
        assertEquals("hello", vocabularyWord?.word)
        assertEquals("/həˈloʊ/", vocabularyWord?.pronunciation)
        assertEquals(1, fakeGeminiAIService.callCount)

        // Verify it is cached
        val cachedJson = fakeCacheMap[word]
        assertNotNull(cachedJson)
        val cachedWord = Gson().fromJson(cachedJson, VocabularyWord::class.java)
        assertEquals("hello", cachedWord.word)
        assertEquals("/həˈloʊ/", cachedWord.pronunciation)
    }

    @Test
    fun testLookupWord_WhenCached_ReturnsCachedWordAndDoesNotCallAI() = runTest {
        val word = "world"
        val cachedVocabularyWord = VocabularyWord(
            word = "world",
            pronunciation = "/wɜːld/",
            definitions = listOf(
                WordDefinition(
                    pos = "noun",
                    meaningVietnamese = "thế giới",
                    definitionEnglish = "the earth, together with all of its countries and peoples",
                    exampleSentence = "around the world"
                )
            )
        )
        val jsonStr = Gson().toJson(cachedVocabularyWord)
        fakeCacheMap[word] = jsonStr

        val result = lookupStrategy.lookupWord(word)

        assertTrue(result.isSuccess)
        val vocabularyWord = result.getOrNull()
        assertNotNull(vocabularyWord)
        assertEquals("world", vocabularyWord?.word)
        assertEquals("/wɜːld/", vocabularyWord?.pronunciation)
        assertEquals(0, fakeGeminiAIService.callCount) // AI should NOT be called
    }

    // --- Fake Classes for testing ---

    class FakeGeminiAIService : GeminiAIService("fake-model") {
        var callCount = 0
        var responseResult: Result<String> = Result.success("")

        override suspend fun lookupWordDetail(word: String): Result<String> {
            callCount++
            return responseResult
        }
    }
}
