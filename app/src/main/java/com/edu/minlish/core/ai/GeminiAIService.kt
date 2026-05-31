package com.edu.minlish.core.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAIService(private val apiKey: String) {
    
    // For text generation and Auto-Fill
    private val textModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
            }
        )
    }

    // For processing Audio/Multimodal (Speaking Practice)
    private val multimodalModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-pro", // Pro is better for complex audio analysis
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.4f
                responseMimeType = "application/json"
            }
        )
    }

    suspend fun generateAutoFillContent(word: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API Key is missing. Please add it to local.properties."))
        }
        try {
            val prompt = """
                Bạn là một chuyên gia ngôn ngữ học. Hãy tạo từ vựng chuẩn cho từ tiếng Anh: "$word".
                Trả về kết quả DƯỚI DẠNG JSON hợp lệ theo cấu trúc sau, KHÔNG ĐƯỢC có các thẻ markdown bao quanh (như ```json):
                {
                  "word": "từ_gốc",
                  "pronunciation": "phiên_âm_IPA",
                  "definitions": [
                    {
                      "pos": "Noun/Verb/Adjective/...",
                      "meaningVietnamese": "Nghĩa tiếng Việt chuẩn",
                      "definitionEnglish": "English definition",
                      "exampleSentence": "Một câu ví dụ hay, tự nhiên",
                      "synonyms": ["đồng nghĩa 1", "đồng nghĩa 2"],
                      "antonyms": ["trái nghĩa 1", "trái nghĩa 2"]
                    }
                  ],
                  "collocations": "cụm_từ_1, cụm_từ_2",
                  "personalNote": "Ghi chú thêm về cách dùng (bằng tiếng Việt)"
                }
                Lưu ý: "definitions" là một mảng, có thể có từ 1 đến 3 nghĩa phổ biến nhất.
            """.trimIndent()
            
            val response = textModel.generateContent(prompt)
            val text = response.text ?: throw Exception("Empty response from AI")
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun evaluateSpeaking(topic: String, audioBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
         if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API Key is missing."))
        }
        try {
            val inputContent = content {
                blob("audio/mp4", audioBytes) // Adjust mime type if needed (e.g., audio/wav)
                text("""
                    You are an expert IELTS examiner. Listen to the audio of the user answering the topic: "$topic".
                    Please provide an evaluation of their speaking.
                    Return the result in strictly valid JSON format exactly matching this structure without markdown formatting:
                    {
                        "transcript": "Write down exactly what the user said",
                        "score": "A score from 0.0 to 10.0",
                        "grammarFeedback": "Detailed feedback on grammatical errors and how to fix them",
                        "vocabularyFeedback": "Feedback on vocabulary usage and suggestions for better words",
                        "fluencyFeedback": "Feedback on pronunciation, fluency, and coherence",
                        "overallComment": "A brief overall encouraging comment in Vietnamese"
                    }
                """.trimIndent())
            }
            
            val response = multimodalModel.generateContent(inputContent)
            val text = response.text ?: throw Exception("Empty response from AI")
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
