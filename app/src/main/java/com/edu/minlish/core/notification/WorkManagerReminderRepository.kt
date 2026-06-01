package com.edu.minlish.core.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.edu.minlish.core.util.ReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * WorkManager implementation của ReminderRepository.
 * Toàn bộ WorkManager logic tập trung ở đây — không còn trong NotificationHelper.
 */
class WorkManagerReminderRepository(
    private val context: Context
) : ReminderRepository {

    companion object {
        private const val WORK_NAME = "MinLishDailyReminder"
    }

    override fun scheduleDaily(timeString: String) {
        try {
            val (hour, minute) = parseTime(timeString)

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // Nếu giờ đã qua hôm nay, lên lịch cho ngày mai
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = target.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun cancelAll() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Parse "09:00 PM" hoặc "21:00" thành (hour24, minute).
     */
    private fun parseTime(timeString: String): Pair<Int, Int> {
        val parts = timeString.trim().split(" ")
        val timeParts = parts[0].split(":")
        var hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val amPm = parts.getOrNull(1)?.uppercase()

        when {
            amPm == "PM" && hour < 12 -> hour += 12
            amPm == "AM" && hour == 12 -> hour = 0
        }

        return Pair(hour, minute)
    }
}
