package com.edu.minlish.core.notification

/**
 * UseCase: Lên lịch hoặc hủy thông báo nhắc học.
 * Presentation layer gọi UseCase này — không biết gì về WorkManager.
 */
class ScheduleReminderUseCase(
    private val reminderRepository: ReminderRepository
) {
    /** Bật nhắc nhở hàng ngày lúc [timeString] (vd: "08:00 AM"). */
    fun schedule(timeString: String) = reminderRepository.scheduleDaily(timeString)

    /** Tắt tất cả nhắc nhở. */
    fun cancel() = reminderRepository.cancelAll()
}
