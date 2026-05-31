package com.edu.minlish.features.speaking.domain.model

data class SpeakingTopic(
    val id: String,
    val title: String,
    val prompt: String
)

data class SpeakingResult(
    val transcript: String = "",
    val score: String = "0.0",
    val grammarFeedback: String = "",
    val vocabularyFeedback: String = "",
    val fluencyFeedback: String = "",
    val overallComment: String = ""
)
