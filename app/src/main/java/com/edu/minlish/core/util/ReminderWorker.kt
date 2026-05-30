package com.edu.minlish.core.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        NotificationHelper.showReminderNotification(
            applicationContext,
            "MinLish Study Time! 🔥",
            "Keep up your streak! Tap here to practice your vocabulary words now."
        )
        return Result.success()
    }
}
