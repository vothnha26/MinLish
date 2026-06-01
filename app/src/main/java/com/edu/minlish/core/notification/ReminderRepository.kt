package com.edu.minlish.core.notification

/**
 * Domain interface — định nghĩa contract cho việc lên lịch nhắc nhở.
 * Không biết gì về WorkManager hay Android platform.
 */
interface ReminderRepository {
    /** Lên lịch nhắc nhở hàng ngày vào giờ chỉ định (format "HH:mm AM/PM"). */
    fun scheduleDaily(timeString: String)

    /** Hủy tất cả lịch nhắc nhở đang chạy. */
    fun cancelAll()
}
