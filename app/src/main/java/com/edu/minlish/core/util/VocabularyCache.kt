package com.edu.minlish.core.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility to cache looked up vocabulary words in SharedPreferences to avoid redundant AI queries.
 */
object VocabularyCache {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("minlish_vocabulary_cache", Context.MODE_PRIVATE)
    }

    fun getCachedWord(word: String): String? {
        return prefs?.getString("cached_word_${word.trim().lowercase()}", null)
    }

    fun cacheWord(word: String, jsonWord: String) {
        prefs?.edit()?.putString("cached_word_${word.trim().lowercase()}", jsonWord)?.apply()
    }
}
