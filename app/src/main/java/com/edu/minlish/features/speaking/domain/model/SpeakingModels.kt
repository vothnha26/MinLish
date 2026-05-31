package com.edu.minlish.features.speaking.domain.model

enum class MessageSender {
    AI, USER
}

data class SpeakingChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val transcript: String? = null,
    val turnFeedback: String? = null
)

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
