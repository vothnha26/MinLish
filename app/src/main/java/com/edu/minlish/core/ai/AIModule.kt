package com.edu.minlish.core.ai

import com.edu.minlish.BuildConfig

object AIModule {
    val geminiService: GeminiAIService by lazy {
        val model = BuildConfig.GEMINI_MODEL
        if (model.isBlank()) {
            throw IllegalStateException("Gemini Model is not configured in local.properties. Please add 'gemini.model=...'")
        }
        GeminiAIService(modelName = model)
    }
}
