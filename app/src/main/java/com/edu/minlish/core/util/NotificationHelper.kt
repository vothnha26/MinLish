package com.edu.minlish.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.edu.minlish.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "study_reminder_channel"
    private const val CHANNEL_NAME = "Study Reminders"
    private const val CHANNEL_DESC = "Daily study reminder notifications"

    fun showReminderNotification(context: Context, title: String, message: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard system icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(1001, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(context: Context, timeString: String) {
        try {
            // Parse timeString like "09:00 PM"
            val parts = timeString.split(" ")
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val amPm = parts.getOrNull(1) ?: "PM"
            
            if (amPm == "PM" && hour < 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0

            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            val now = java.util.Calendar.getInstance()
            if (calendar.before(now)) {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = calendar.timeInMillis - now.timeInMillis

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<ReminderWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            )
                .setInitialDelay(initialDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "MinLishDailyReminder",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelDailyReminder(context: Context) {
        try {
            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("MinLishDailyReminder")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
