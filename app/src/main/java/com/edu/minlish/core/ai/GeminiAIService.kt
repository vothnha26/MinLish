package com.edu.minlish.core.ai

import com.edu.minlish.features.speaking.domain.model.SpeakingChatMessage
import com.edu.minlish.features.speaking.domain.model.MessageSender
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

    suspend fun generateFirstQuestion(topic: String, mode: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemContext = when (mode) {
                "IELTS Speaking" -> "You are an official IELTS Speaking Examiner. Conduct the session like a formal IELTS Speaking exam. Assess the user's band score criteria (Fluency and Coherence, Lexical Resource, Grammatical Range and Accuracy, Pronunciation)."
                "TOEIC Speaking" -> "You are a TOEIC Speaking Evaluator. Conduct the session focusing on business contexts, picture descriptions, and expressing opinions in a professional, concise manner."
                "Job Interview Prep" -> "You are an HR Manager interviewing the user for a professional job role. Conduct the session as a job interview, asking relevant situational and behavioral questions."
                else -> "You are a friendly, encouraging English speaking partner helping the user practice daily conversations."
            }

            val prompt = """
                $systemContext
                The user has chosen the speaking topic: "$topic".
                Generate a friendly opening greeting and ask the first question to begin the practice session.
                Keep it concise (1-2 sentences).
                
                Return the response in strictly valid JSON format without markdown wrapping:
                {
                  "question": "Friendly greeting and the first question"
                }
            """.trimIndent()
            val response = textModel.generateContent(prompt)
            val text = response.text ?: throw Exception("Empty response from AI")
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateNextTurn(
        topic: String,
        mode: String,
        history: List<SpeakingChatMessage>,
        audioBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val historyStr = history.joinToString(separator = "\n") { msg ->
                when (msg.sender) {
                    MessageSender.AI -> "AI Interviewer: ${msg.text}"
                    MessageSender.USER -> "User Candidate: ${msg.transcript ?: msg.text}"
                }
            }

            val systemContext = when (mode) {
                "IELTS Speaking" -> "You are an official IELTS Speaking Examiner conducting an exam about '$topic'. Keep your role formal and professional. The next question should follow IELTS Speaking formats."
                "TOEIC Speaking" -> "You are a TOEIC Speaking Evaluator conducting a test about '$topic'. Focus on professional and business contexts."
                "Job Interview Prep" -> "You are an HR Manager interviewing the user about '$topic'. Keep your questions realistic, professional, and job-oriented."
                else -> "You are a friendly and encouraging English conversation partner practicing daily dialog about '$topic'."
            }

            val inputContent = content {
                inlineData(audioBytes, "audio/mp4")
                text("""
                    $systemContext
                    Here is the conversation history:
                    $historyStr
                    
                    TASK:
                    1. Listen to the user's latest audio response (the attached audio).
                    2. Transcribe the audio word-for-word (`transcript`). Do not correct grammar or clean up stuttering in the transcript.
                    3. Generate a friendly, natural reply in English reacting to the user's answer (`reply`).
                    4. Generate the next logical question in English to keep the conversation flowing (`nextQuestion`).
                    5. Provide a short, constructive feedback in Vietnamese about their grammar, vocabulary, or pronunciation for this specific turn (`turnFeedback`).
                    
                    Return the result in strictly valid JSON format without markdown wrapping:
                    {
                      "transcript": "Exact transcription of user's audio",
                      "reply": "Friendly response in English",
                      "nextQuestion": "The next question to ask",
                      "turnFeedback": "Gợi ý/nhận xét nhanh bằng tiếng Việt"
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

    suspend fun evaluateSession(
        topic: String,
        mode: String,
        history: List<SpeakingChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val historyStr = history.joinToString(separator = "\n") { msg ->
                when (msg.sender) {
                    MessageSender.AI -> "AI: ${msg.text}"
                    MessageSender.USER -> "User: [Transcript: ${msg.transcript ?: ""}]"
                }
            }

            val modePrompt = when (mode) {
                "IELTS Speaking" -> """
                    Evaluate under the official IELTS Speaking test standards (Fluency & Coherence, Lexical Resource, Grammatical Range & Accuracy, Pronunciation). 
                    For the "score", provide an estimated IELTS Band Score from 1.0 to 9.0 (e.g. "6.5 Band").
                """.trimIndent()
                "TOEIC Speaking" -> """
                    Evaluate under the official TOEIC Speaking standards.
                    For the "score", provide a predicted TOEIC Speaking score range or scale out of 200 (e.g., "130/200").
                """.trimIndent()
                "Job Interview Prep" -> """
                    Evaluate their suitability, confidence, professional vocabulary, and clarity for a job interview.
                    For the "score", provide a score out of 100 (e.g., "85/100").
                """.trimIndent()
                else -> """
                    Evaluate their general conversation capability, confidence, and comprehension.
                    For the "score", provide a score out of 10 (e.g., "8.5 / 10").
                """.trimIndent()
            }

            val prompt = """
                You are an English speaking assessor. Evaluate the user's performance for this speaking practice session.
                
                Topic: "$topic"
                Mode / Exam Format: "$mode"
                
                Evaluation Context:
                $modePrompt
                
                Conversation History:
                $historyStr
                
                Tasks:
                1. Determine the "score" matching the format specified in the Evaluation Context.
                2. Provide detailed Grammar feedback, highlighting specific errors and how to correct them.
                3. Provide detailed Vocabulary feedback, suggesting better word choices and expressions.
                4. Provide detailed Fluency and Pronunciation feedback.
                5. Write an overall encouraging summary in Vietnamese.
                6. Compile all user transcripts into a single string for reference.
                
                Return the result in strictly valid JSON format without markdown:
                {
                  "transcript": "Full combined transcripts of user responses",
                  "score": "Score string (e.g. '6.5 Band' or '130/200' or '8.5 / 10')",
                  "grammarFeedback": "Detailed grammar feedback",
                  "vocabularyFeedback": "Detailed vocabulary feedback",
                  "fluencyFeedback": "Detailed fluency feedback",
                  "overallComment": "Nhận xét tổng quát bằng tiếng Việt"
                }
            """.trimIndent()

            val response = textModel.generateContent(prompt)
            val text = response.text ?: throw Exception("Empty response from AI")
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
