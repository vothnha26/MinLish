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

    var recentLookupHistory: String
        get() = prefs?.getString("recent_lookup_history", "") ?: ""
        set(value) {
            prefs?.edit()?.putString("recent_lookup_history", value)?.apply()
        }

    var streakFreezesLeft: Int
        get() = prefs?.getInt("streak_freezes_left", 2) ?: 2
        set(value) {
            prefs?.edit()?.putInt("streak_freezes_left", value)?.apply()
        }

    var isStreakFreezeEquipped: Boolean
        get() = prefs?.getBoolean("is_streak_freeze_equipped", false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean("is_streak_freeze_equipped", value)?.apply()
        }

    var dailyNewWordsTarget: Int
        get() = prefs?.getInt("daily_new_words_target", 10) ?: 10
        set(value) {
            prefs?.edit()?.putInt("daily_new_words_target", value)?.apply()
        }

    var reminderTime: String
        get() = prefs?.getString("reminder_time", "09:00 PM") ?: "09:00 PM"
        set(value) {
            prefs?.edit()?.putString("reminder_time", value)?.apply()
        }

    var isNotificationsEnabled: Boolean
        get() = prefs?.getBoolean("is_notifications_enabled", true) ?: true
        set(value) {
            prefs?.edit()?.putBoolean("is_notifications_enabled", value)?.apply()
        }
}
