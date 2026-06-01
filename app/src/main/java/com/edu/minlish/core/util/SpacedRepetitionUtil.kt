package com.edu.minlish.core.util

import android.content.Context
import org.json.JSONObject

/**
 * SM-2 spaced repetition utility using SharedPreferences + org.json (built-in Android).
 * No extra dependencies needed.
 */
data class SpacedData(
    val interval: Int = 1,       // days until next review
    val repetition: Int = 0,     // number of consecutive correct answers
    val factor: Double = 2.5,    // ease factor
    val nextReview: Long = System.currentTimeMillis()
)

object SpacedRepetitionUtil {

    private const val PREFS_NAME = "spaced_repetition"
    private const val ONE_DAY_MS = 24L * 60 * 60 * 1000

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun keyFor(wordId: String) = "spaced_$wordId"

    fun getData(context: Context, wordId: String): SpacedData? {
        val str = prefs(context).getString(keyFor(wordId), null) ?: return null
        return try {
            val obj = JSONObject(str)
            SpacedData(
                interval = obj.getInt("interval"),
                repetition = obj.getInt("repetition"),
                factor = obj.getDouble("factor"),
                nextReview = obj.getLong("nextReview")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun saveData(context: Context, wordId: String, data: SpacedData) {
        val obj = JSONObject().apply {
            put("interval", data.interval)
            put("repetition", data.repetition)
            put("factor", data.factor)
            put("nextReview", data.nextReview)
        }
        prefs(context).edit().putString(keyFor(wordId), obj.toString()).apply()
    }

    /**
     * Call this after each quiz answer to update the SM-2 state for a word.
     * [correct] = true if user answered correctly, false otherwise.
     */
    fun updateData(context: Context, wordId: String, correct: Boolean) {
        val existing = getData(context, wordId)
        val now = System.currentTimeMillis()

        val updated = if (existing == null) {
            if (correct)
                SpacedData(interval = 1, repetition = 1, factor = 2.5, nextReview = now + ONE_DAY_MS)
            else
                SpacedData(interval = 1, repetition = 0, factor = 2.5, nextReview = now + ONE_DAY_MS)
        } else {
            var rep = existing.repetition
            var intv = existing.interval
            var fac = existing.factor

            if (correct) {
                rep += 1
                intv = when (rep) {
                    1 -> 1
                    2 -> 6
                    else -> (intv * fac).toInt()
                }
            } else {
                rep = 0
                intv = 1
                fac = (fac - 0.2).coerceAtLeast(1.3)
            }

            SpacedData(
                interval = intv,
                repetition = rep,
                factor = fac,
                nextReview = now + intv * ONE_DAY_MS
            )
        }

        saveData(context, wordId, updated)
    }

    /**
     * Returns true if this word is due for review now.
     */
    fun isDue(context: Context, wordId: String): Boolean {
        val data = getData(context, wordId) ?: return true
        return System.currentTimeMillis() >= data.nextReview
    }
}
