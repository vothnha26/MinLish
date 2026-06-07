package com.edu.minlish.features.library.data.importer

import com.edu.minlish.features.library.domain.importer.VocabularyImportParser
import com.edu.minlish.features.library.domain.model.ImportVocabularyPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class CsvVocabularyImportParser : VocabularyImportParser {
    override suspend fun parse(
        fileName: String,
        inputStream: InputStream
    ): Result<ImportVocabularyPreview> = withContext(Dispatchers.IO) {
        try {
            val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val records = parseCsv(text)
            Result.success(ImportVocabularyRowParser.parse(fileName, records))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCsv(text: String): List<List<String>> {
        val firstLine = text.lineSequence().firstOrNull() ?: ""
        val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','

        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && inQuotes && index + 1 < text.length && text[index + 1] == '"' -> {
                    cell.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    row.add(cell.toString())
                    cell.clear()
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') {
                        index++
                    }
                    row.add(cell.toString())
                    cell.clear()
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(char)
            }
            index++
        }

        row.add(cell.toString())
        if (row.any { it.isNotBlank() }) rows.add(row)

        return rows
    }
}
