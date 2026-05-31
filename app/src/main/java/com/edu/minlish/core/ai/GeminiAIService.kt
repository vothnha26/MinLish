package com.edu.minlish.core.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service to interact with Gemini AI through Firebase AI SDK (Vertex AI in Firebase).
 * Authentication is handled internally by Firebase.
 */
class GeminiAIService(
    private val modelName: String
) {
    
    // For text generation and Auto-Fill
    private val textModel by lazy {
        Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                temperature = 0.1f // Low temperature for deterministic output
                responseMimeType = "application/json"
            }
        )
    }

    // For processing Audio/Multimodal (Speaking Practice)
    private val multimodalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                temperature = 0.0f // Set to 0.0 for maximum transcription accuracy
                responseMimeType = "application/json"
            }
        )
    }

    suspend fun generateAutoFillContent(word: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Bạn là một chuyên gia ngôn ngữ học. Hãy tạo từ vựng chuẩn cho từ tiếng Anh: "$word".
                Trả về kết quả DƯỚI DẠNG JSON hợp lệ theo cấu trúc sau, KHÔNG ĐƯỢC có các thẻ markdown bao quanh:
                {
                  "word": "$word",
                  "pronunciation": "...",
                  "definitions": [
                    {
                      "pos": "...",
                      "meaningVietnamese": "...",
                      "definitionEnglish": "...",
                      "exampleSentence": "...",
                      "synonyms": [],
                      "antonyms": []
                    }
                  ],
                  "collocations": "...",
                  "personalNote": "..."
                }
            """.trimIndent()
            
            val response = textModel.generateContent(prompt)
            val text = response.text ?: throw Exception("Empty response from AI")
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateVocabularySet(
        prompt: String,
        category: String,
        wordCount: Int,
        includeCollocations: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fullPrompt = """
                Bạn là một chuyên gia soạn thảo giáo trình tiếng Anh. Hãy tạo một bộ từ vựng cho chủ đề: "$prompt".
                Yêu cầu:
                - Số lượng từ: $wordCount từ.
                - Thể loại: $category.
                - Bao gồm cụm từ đi kèm (collocations): ${if (includeCollocations) "Có" else "Không"}.
                
                Trả về kết quả DƯỚI DẠNG JSON hợp lệ theo cấu trúc sau, KHÔNG ĐƯỢC có các thẻ markdown bao quanh:
                {
                  "title": "Tên bộ từ vựng hấp dẫn",
                  "description": "Mô tả ngắn gọn về bộ từ vựng",
                  "words": [
                    {
                      "word": "từ_vựng",
                      "pronunciation": "phiên_âm_IPA",
                      "definitions": [
                        {
                          "pos": "Noun/Verb/...",
                          "meaningVietnamese": "nghĩa tiếng Việt",
                          "definitionEnglish": "English definition",
                          "exampleSentence": "Câu ví dụ",
                          "synonyms": ["đồng nghĩa"],
                          "antonyms": ["trái nghĩa"]
                        }
                      ],
                      "collocations": "cụm từ đi kèm",
                      "personalNote": "ghi chú"
                    }
                  ]
                }
            """.trimIndent()

            val response = textModel.generateContent(fullPrompt)
            val text = response.text ?: throw Exception("Empty response from AI")
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun evaluateSpeaking(topic: String, audioBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputContent = content {
                inlineData(audioBytes, "audio/mp4") 
                text("""
                    TASK: Transcribe and evaluate the provided audio.
                    TOPIC: "$topic"
                    
                    STRICT INSTRUCTIONS:
                    1. FOR THE "transcript" FIELD: You MUST write down exactly word-for-word what the user said in the audio. DO NOT use template answers, DO NOT clean up the speech, and DO NOT hallucinate. If the user said "Uhm, I... I like apple", the transcript must be "Uhm, I... I like apple".
                    2. Evaluate the performance based on the ACTUAL transcript.
                    
                    Return the result in strictly valid JSON format without markdown formatting:
                    {
                        "transcript": "The exact verbatim transcription of the audio",
                        "score": "A score from 0.0 to 10.0",
                        "grammarFeedback": "Feedback based ONLY on what was actually said",
                        "vocabularyFeedback": "Suggestions based ONLY on what was actually said",
                        "fluencyFeedback": "Feedback on pronunciation and hesitations in the audio",
                        "overallComment": "Encouraging summary in Vietnamese"
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
