package com.edu.minlish.features.library.data.repository

import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.repository.LookupStrategy
import com.edu.minlish.features.library.domain.repository.DictionaryStrategy
import com.edu.minlish.features.library.domain.repository.TranslationStrategy
import com.edu.minlish.features.library.domain.repository.CollocationStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Fallback implementation of LookupStrategy using Free Dictionary API and translation.
 */
class DictionaryApiLookupStrategy(
    private val dictionaryStrategy: DictionaryStrategy = FreeDictionaryStrategy(),
    private val translationStrategy: TranslationStrategy = GoogleTranslationStrategy(),
    private val collocationStrategy: CollocationStrategy = DatamuseCollocationStrategy()
) : LookupStrategy {
    override suspend fun lookupWord(word: String): Result<VocabularyWord> = withContext(Dispatchers.IO) {
        val cleanWord = word.trim().lowercase()
        if (cleanWord.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Word cannot be empty"))
        }

        // Fetch translation and collocations in parallel with dictionary details
        val translationDeferred = async {
            translationStrategy.translate(cleanWord, "en", "vi").getOrDefault("")
        }
        val collocationsDeferred = async {
            collocationStrategy.fetchCollocations(cleanWord).getOrDefault(emptyList())
        }

        dictionaryStrategy.getWordDetails(cleanWord).mapCatching { entries ->
            if (entries.isEmpty()) {
                throw Exception("Word details not found in dictionary")
            }

            val firstEntry = entries.first()
            val phonetic = firstEntry.phonetic 
                ?: firstEntry.phonetics.firstOrNull { !it.text.isNullOrBlank() }?.text 
                ?: ""
            
            val audio = firstEntry.phonetics.firstOrNull { !it.audio.isNullOrBlank() }?.audio ?: ""
            val audioUrl = if (audio.startsWith("//")) "https:$audio" else audio

            val translationResult = translationDeferred.await()
            val collocationsResult = collocationsDeferred.await()

            // Map definitions
            val mappedDefinitions = mutableListOf<WordDefinition>()
            
            // Try to use AI to get better Vietnamese meanings for each definition
            val aiDefinitionsResult = try {
                val aiResponse = com.edu.minlish.core.ai.AIModule.geminiService.lookupWordDetail(cleanWord)
                if (aiResponse.isSuccess) {
                    val jsonStr = aiResponse.getOrThrow()
                    val cleanJson = if (jsonStr.contains("```json")) {
                        jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
                    } else if (jsonStr.contains("```")) {
                        jsonStr.substringAfter("```").substringBeforeLast("```").trim()
                    } else {
                        jsonStr
                    }
                    com.google.gson.Gson().fromJson(cleanJson, com.edu.minlish.core.ai.model.AIAutoFillResult::class.java).definitions
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            entries.forEach { entry ->
                entry.meanings.forEach { meaning ->
                    meaning.definitions.take(2).forEach { def ->
                        val meaningSyns = meaning.synonyms?.take(3) ?: emptyList()
                        val meaningAnts = meaning.antonyms?.take(3) ?: emptyList()
                        val allSyns = (def.synonyms + meaningSyns).distinct().take(5)
                        val allAnts = (def.antonyms + meaningAnts).distinct().take(5)

                        // Try to find a matching AI definition for this POS
                        val matchingAiDef = aiDefinitionsResult.find { 
                            it.pos.equals(meaning.partOfSpeech, ignoreCase = true) 
                        } ?: aiDefinitionsResult.firstOrNull()

                        mappedDefinitions.add(
                            WordDefinition(
                                pos = meaning.partOfSpeech,
                                definitionEnglish = def.definition,
                                meaningVietnamese = matchingAiDef?.meaningVietnamese ?: translationResult,
                                exampleSentence = def.example ?: "",
                                synonyms = allSyns,
                                antonyms = allAnts
                            )
                        )
                    }
                }
            }

            VocabularyWord(
                word = cleanWord,
                pronunciation = phonetic,
                audioUrl = audioUrl,
                definitions = if (mappedDefinitions.isNotEmpty()) mappedDefinitions.take(5) else aiDefinitionsResult.take(3),
                collocations = collocationsResult.joinToString(", "),
                imageUrl = "https://loremflickr.com/600/400/$cleanWord?lock=${Math.abs(cleanWord.hashCode())}"
            )
        }
    }
}
