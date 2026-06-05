package com.edu.minlish.core.ai.model

data class AIGeneratedSetWord(
    val word: String = "",
    val pronunciation: String = "",
    val pos: String = "",
    val meaningVietnamese: String = ""
)

data class AIGeneratedSet(
    val title: String = "",
    val description: String = "",
    val words: List<AIGeneratedSetWord> = emptyList()
)
