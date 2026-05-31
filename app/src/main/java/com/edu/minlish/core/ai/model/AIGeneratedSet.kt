package com.edu.minlish.core.ai.model

data class AIGeneratedSet(
    val title: String = "",
    val description: String = "",
    val words: List<AIAutoFillResult> = emptyList()
)
