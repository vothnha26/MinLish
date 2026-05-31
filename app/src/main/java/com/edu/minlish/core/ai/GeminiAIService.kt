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
    private val modelName: String // No default value here to ensure it's passed from BuildConfig
) {
    
    // For text generation and Auto-Fill
    private val textModel by lazy {
        Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
            }
        )
    }

    // For processing Audio/Multimodal (Speaking Practice)
    private val multimodalModel by lazy {
        // Use the specified modelName for everything to respect local.properties
        Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                temperature = 0.4f
                responseMimeType = "application/json"
            }
        )
    }

    suspend fun generateAutoFillContent(word: String): Result<String> = withContext(Dispatchers.IO) {
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
        try {
            val inputContent = content {
                inlineData(audioBytes, "audio/m4a") 
                text("""
                    You are an expert IELTS examiner. Listen to the audio of the user answering the topic: "$topic".
                    Please provide an evaluation of their speaking.
                    
                    CRITICAL RULES FOR TRANSCRIPTION:
                    1. For "transcript": Write down EXACTLY what the user said in the audio.
                       - Do NOT correct their grammar or vocabulary.
                       - Do NOT generate a sample script, generic template, or standard IELTS answer.
                       - Include all grammatical errors, hesitations (e.g., "uh", "um", "ah"), pauses, and repetitions.
                       - If the audio is silent, containing only noise, or contains no speech, you MUST set "transcript" to "(No speech detected)" and "score" to "0.0".
                    2. For "score": Give an IELTS Speaking band score from 0.0 to 10.0 based on their actual spoken words. If no speech is detected, the score must be 0.0.
                    3. For "grammarFeedback", "vocabularyFeedback", and "fluencyFeedback": Give detailed, constructive feedback based ONLY on their actual spoken words. Highlight specific mistakes from the transcript and suggest how to correct them.
                    4. For "overallComment": Provide a brief overall encouraging comment in Vietnamese.
                    
                    Return the result in strictly valid JSON format exactly matching this structure without markdown formatting:
                    {
                        "transcript": "Exact transcription of the user's spoken words",
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
