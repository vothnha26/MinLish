package com.edu.minlish.core.util

import android.content.Context
import android.content.SharedPreferences

object AppSettings {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("minlish_settings", Context.MODE_PRIVATE)
    }

    var intervalUnit: String
        get() = prefs?.getString("interval_unit", "DAYS") ?: "DAYS"
        set(value) {
            prefs?.edit()?.putString("interval_unit", value)?.apply()
        }

    var masteredThreshold: Int
        get() = prefs?.getInt("mastered_threshold", 30) ?: 30
        set(value) {
            prefs?.edit()?.putInt("mastered_threshold", value)?.apply()
        }
}
