package com.edu.minlish.core.ai.model

import com.edu.minlish.features.library.domain.model.WordDefinition

data class AIAutoFillResult(
    val word: String = "",
    val pronunciation: String = "",
    val definitions: List<WordDefinition> = emptyList(),
    val collocations: String = "",
    val personalNote: String = ""
)
