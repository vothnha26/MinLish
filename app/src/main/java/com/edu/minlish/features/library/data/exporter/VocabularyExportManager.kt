package com.edu.minlish.features.library.data.exporter

import android.content.Context
import android.net.Uri
import com.edu.minlish.features.library.domain.model.VocabularyWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

class VocabularyExportManager {

    suspend fun exportToCsv(context: Context, uri: Uri, words: List<VocabularyWord>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        // Header
                        writer.write("Word,Pronunciation,POS,Meaning (VN),Definition (EN),Example,Synonyms,Antonyms,Collocations,Note\n")
                        
                        words.forEach { word ->
                            val definition = word.definitions.firstOrNull()
                            val line = StringBuilder()
                            line.append(escapeCsv(word.word)).append(",")
                            line.append(escapeCsv(word.pronunciation)).append(",")
                            line.append(escapeCsv(definition?.pos ?: "")).append(",")
                            line.append(escapeCsv(definition?.meaningVietnamese ?: "")).append(",")
                            line.append(escapeCsv(definition?.definitionEnglish ?: "")).append(",")
                            line.append(escapeCsv(definition?.exampleSentence ?: "")).append(",")
                            line.append(escapeCsv(definition?.synonyms?.joinToString("; ") ?: "")).append(",")
                            line.append(escapeCsv(definition?.antonyms?.joinToString("; ") ?: "")).append(",")
                            line.append(escapeCsv(word.collocations)).append(",")
                            line.append(escapeCsv(word.personalNote))
                            line.append("\n")
                            writer.write(line.toString())
                        }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
