package com.edu.minlish.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.edu.minlish.core.util.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        // 1. Hiển thị thông báo nhắc học hàng ngày
        NotificationHelper.showReminderNotification(
            context,
            "MinLish Study Time! 🔥",
            "Keep up your streak! Tap here to practice your vocabulary words now."
        )

        // 2. Tự động lên lịch lại báo thức cho ngày mai (tạo tính chất lặp lại hàng ngày)
        val reminderTime = com.edu.minlish.core.util.AppSettings.reminderTime
        val repository = AlarmReminderRepository(context)
        repository.scheduleDaily(reminderTime)
    }
}
