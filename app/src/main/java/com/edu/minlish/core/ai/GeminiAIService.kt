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
            Result.failure(mapGenerativeException(e))
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
                
                Trả về kết quả DƯỚI DẠNG JSON hợp lệ theo cấu trúc mẫu dưới đây, KHÔNG ĐƯỢC có các thẻ markdown bao quanh.
                Chú ý: Sinh nghĩa tiếng Việt chuẩn xác nhất theo đúng ngữ cảnh và chủ đề ôn tập.
                
                {
                  "title": "Tên bộ từ vựng hấp dẫn",
                  "description": "Mô tả ngắn gọn về bộ từ vựng",
                  "words": [
                    {
                      "word": "từ_tiếng_Anh",
                      "pronunciation": "phiên_âm_IPA",
                      "pos": "Noun/Verb/Adjective/...",
                      "meaningVietnamese": "nghĩa tiếng Việt chính xác theo chủ đề"
                    }
                  ]
                }
            """.trimIndent()

            val response = textModel.generateContent(fullPrompt)
            val text = response.text ?: throw Exception("Empty response from AI")
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(mapGenerativeException(e))
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
            Result.failure(mapGenerativeException(e))
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
                    1. SILENCE DETECTION (CRITICAL — do this FIRST):
                       Listen carefully to the attached audio. If ANY of these conditions apply:
                       - The audio is completely silent or nearly silent
                       - You can only hear background noise, hiss, or ambient sounds
                       - There is no recognizable human voice or spoken English words
                       - The speech is completely unintelligible
                       Then you MUST respond with this exact JSON and NOTHING else:
                       {"transcript": "SILENCE_DETECTED", "reply": "", "nextQuestion": "", "turnFeedback": ""}
                       DO NOT attempt to guess, invent, or hallucinate any spoken content.
                    
                    2. If and only if clear spoken English is detected:
                       - Transcribe the audio word-for-word into "transcript". Do not correct grammar or clean up stuttering.
                       - Generate a friendly, natural reply in English reacting to the user's answer ("reply").
                       - Generate the next logical question in English to keep the conversation flowing ("nextQuestion").
                       - Provide a short, constructive feedback in Vietnamese about their grammar, vocabulary, or pronunciation ("turnFeedback").
                    
                    Return the result in strictly valid JSON format without markdown wrapping:
                    {
                      "transcript": "Exact transcription OR SILENCE_DETECTED",
                      "reply": "Friendly response in English (empty if silence)",
                      "nextQuestion": "The next question to ask (empty if silence)",
                      "turnFeedback": "Gợi ý bằng tiếng Việt (empty if silence)"
                    }
                """.trimIndent())
            }

            val response = multimodalModel.generateContent(inputContent)
            val text = response.text ?: throw Exception("Empty response from AI")
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(mapGenerativeException(e))
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
            Result.failure(mapGenerativeException(e))
        }
    }

    suspend fun lookupWordDetail(word: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Bạn là một từ điển Anh-Việt thông minh. Hãy tra cứu và phân tích chi tiết từ tiếng Anh: "$word".
                Yêu cầu kết quả bao gồm: Phiên âm IPA chuẩn, từ loại, nghĩa tiếng Việt chính xác, định nghĩa bằng tiếng Anh rõ ràng, ví dụ cụ thể có dịch nghĩa, danh sách từ đồng nghĩa và trái nghĩa, các collocations phổ biến và một mẹo/ghi chú ghi nhớ từ vựng này bằng tiếng Việt.
                
                Yêu cầu bắt buộc: Trả về kết quả DƯỚI DẠNG JSON hợp lệ theo cấu trúc sau, KHÔNG ĐƯỢC có các thẻ markdown bao quanh:
                {
                  "word": "$word",
                  "pronunciation": "...",
                  "definitions": [
                    {
                      "pos": "Noun/Verb/Adjective/...",
                      "meaningVietnamese": "nghĩa tiếng Việt chính",
                      "definitionEnglish": "Định nghĩa tiếng Anh rõ ràng",
                      "exampleSentence": "Ví dụ tiếng Anh (Dịch nghĩa tiếng Việt)",
                      "synonyms": ["đồng nghĩa 1", "đồng nghĩa 2"],
                      "antonyms": ["trái nghĩa 1", "trái nghĩa 2"]
                    }
                  ],
                  "collocations": "Cụm từ đi kèm phổ biến",
                  "personalNote": "Ghi chú/Mẹo nhớ từ vựng này bằng tiếng Việt"
                }
            """.trimIndent()
            
            val response = textModel.generateContent(prompt)
            val textResponse = response.text ?: throw Exception("Empty response from AI")
            Result.success(textResponse.trim())
        } catch (e: Exception) {
            Result.failure(mapGenerativeException(e))
        }
    }

    private fun mapGenerativeException(e: Exception): Exception {
        val msg = e.message ?: ""
        val errorString = e.toString()
        return if (msg.contains("MissingFieldException") || 
            msg.contains("GRpcError") || 
            msg.contains("details") ||
            errorString.contains("MissingFieldException") ||
            errorString.contains("GRpcError")
        ) {
            Exception(
                "Lỗi kết nối Vertex AI: Máy chủ trả về phản hồi không hợp lệ hoặc mô hình không được hỗ trợ. " +
                "Vui lòng kiểm tra lại cấu hình 'gemini.model' trong local.properties, file google-services.json hoặc kết nối mạng.",
                e
            )
        } else {
            e
        }
    }
}
