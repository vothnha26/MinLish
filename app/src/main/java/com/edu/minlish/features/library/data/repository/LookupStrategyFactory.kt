package com.edu.minlish.features.library.data.repository

import com.edu.minlish.features.library.domain.repository.LookupStrategy

/**
 * Factory class to create LookupStrategy instances.
 */
object LookupStrategyFactory {
    fun create(useAi: Boolean = true): LookupStrategy {
        return GeminiLookupStrategy()
    }
}
