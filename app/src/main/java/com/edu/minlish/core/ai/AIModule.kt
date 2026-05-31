package com.edu.minlish.core.ai

import com.edu.minlish.BuildConfig

object AIModule {
    val geminiService: GeminiAIService by lazy {
        GeminiAIService(BuildConfig.GEMINI_API_KEY)
    }
}
