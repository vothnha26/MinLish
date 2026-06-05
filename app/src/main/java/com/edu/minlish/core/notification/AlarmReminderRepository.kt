package com.edu.minlish.core.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class AlarmReminderRepository(
    private val context: Context
) : ReminderRepository {

    @SuppressLint("ScheduleExactAlarm")
    override fun scheduleDaily(timeString: String) {
        try {
            val (hour, minute) = parseTime(timeString)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // Nếu giờ hẹn đã qua trong ngày hôm nay, đặt cho ngày mai
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun cancelAll() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
