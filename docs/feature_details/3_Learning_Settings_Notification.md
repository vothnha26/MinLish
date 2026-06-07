# MinLish - Giải thích chi tiết Features: Learning (Học ngắt quãng & Quiz), Notification (Thông báo), Settings (Cài đặt)

Tài liệu này giải thích chi tiết **từng dòng code phần logic** (Model, Repository, UseCase, ViewModel) và **Luồng hoạt động (Activity Flow)** của 3 tính năng: Learning (Quiz & Spaced Repetition), Notification, và App Settings.

---

## 1. Feature: Learning (Học ngắt quãng SM-2 & Quiz)

Tính năng này quản lý tiến độ học từ vựng của người dùng dựa trên thuật toán Spaced Repetition (SM-2) và cung cấp các bài tập (Quiz) để ôn tập.

### 1.1. Luồng hoạt động (Activity Flow)
1. **Lấy câu hỏi**: `QuizViewModel` gọi `LearningRepository` lấy các từ đến hạn ôn tập (hoặc từ mới). Sau đó đưa qua `BuildQuizQuestionsUseCase` để xáo trộn và tạo thành các dạng câu hỏi (Multiple Choice, Spelling, Matching).
2. **Người dùng làm bài**: Trả lời từng câu (trắc nghiệm, điền từ, nối thẻ). `QuizViewModel` ghi nhận đáp án.
3. **Cập nhật tiến độ**: Mỗi khi trả lời xong một câu, `QuizViewModel` gọi `UpdateWordProgressUseCase`.
4. **Tính toán SM-2**: `UpdateWordProgressUseCase` kiểm tra kết quả (đúng/sai) để tính toán lại `interval`, `easeFactor`, `repetitions` và ngày ôn tập tiếp theo (`nextReviewDate`) dựa vào thuật toán SM-2.
5. **Lưu trữ DB**: Dữ liệu tiến độ học (`UserWordProgress`) được lưu/cập nhật vào Firestore thông qua `FirestoreLearningRepositoryImpl`.

### 1.2. Chi tiết từng file Logic

#### `domain/model/UserWordProgress.kt`
File định nghĩa mô hình dữ liệu lưu trữ trạng thái học của một từ vựng.
```kotlin
package com.edu.minlish.features.learning.domain.model

import java.util.Date

data class UserWordProgress(
    val id: String = "",                 // ID duy nhất của tiến độ
    val userId: String = "",             // ID của người dùng học từ này
    val wordId: String = "",             // ID của từ vựng
    val setId: String = "",              // ID của bộ từ vựng chứa từ này
    
    // SM-2 Parameters
    val easeFactor: Float = 2.5f,        // Hệ số độ khó (mặc định 2.5)
    val interval: Int = 0,               // Khoảng cách ôn tập tiếp theo (số ngày/giờ/phút)
    val repetitions: Int = 0,            // Số lần trả lời đúng liên tiếp
    
    val nextReviewDate: Date = Date(),   // Thời điểm cần ôn tập lại
    val lastReviewedAt: Date = Date(),   // Lần cuối cùng ôn tập
    val status: String = "learning"      // Trạng thái: learning, reviewing, mastered
)
```

#### `domain/usecase/UpdateWordProgressUseCase.kt`
Chứa logic thuật toán SM-2 để cập nhật tiến độ học tập.
```kotlin
class UpdateWordProgressUseCase(
    private val repository: LearningRepository
) {
    suspend operator fun invoke(
        existing: UserWordProgress?,     // Tiến độ hiện tại của từ (có thể null nếu mới học)
        userId: String,
        wordId: String,
        setId: String,
        correct: Boolean,                // Người dùng trả lời đúng hay sai
        intervalUnitMs: Long = ONE_DAY_MS, // Đơn vị thời gian (từ Setting)
        masteredThreshold: Int = 30      // Ngưỡng xem như đã thuộc (từ Setting)
    ): Result<Unit> {
        // Nếu từ này chưa học bao giờ
        val updated = if (existing == null) {
            com.edu.minlish.core.util.SpacedRepetitionUtil.createInitialProgress(
                userId = userId, wordId = wordId, setId = setId, correct = correct, intervalUnitMs = intervalUnitMs
            )
        } else {
            // Nếu đã học, tính toán lại dựa trên SM-2
            com.edu.minlish.core.util.SpacedRepetitionUtil.calculateSM2ForBinary(
                current = existing, correct = correct, intervalUnitMs = intervalUnitMs, masteredThreshold = masteredThreshold
            )
        }
        // Lưu tiến độ mới tính vào Repository
        return repository.updateProgress(updated)
    }
}
```

#### `presentation/viewmodel/QuizViewModel.kt`
Quản lý trạng thái màn hình Quiz.
```kotlin
class QuizViewModel(
    application: Application,
    private val repository: LearningRepository,
    private val updateProgressUseCase: UpdateWordProgressUseCase,
    private val buildQuestionsUseCase: BuildQuizQuestionsUseCase
) : AndroidViewModel(application) {
    // ... Khai báo các biến State: uiState, currentIndex, score...
    
    // Tải danh sách bài tập
    fun loadQuiz(setId: String?, modes: String = "MULTIPLE_CHOICE", questionCount: Int = 10) {
        viewModelScope.launch {
            // Lấy từ vựng cần ôn tập từ Firestore
            repository.getDueWords(currentUser.id, setId, forceAll = true).onSuccess { dueWords ->
                // Gọi usecase sinh câu hỏi
                val finalQuestions = buildQuestionsUseCase(dueWords, modes, questionCount)
                uiState = QuizUiState.Success(finalQuestions)
            }
        }
    }
    
    // Khi người dùng chọn một đáp án
    fun selectOption(index: Int) {
        val question = state.questions[currentIndex]
        val isCorrect = index == question.correctIndex // Kiểm tra đáp án
        if (isCorrect) score++                         // Cộng điểm
        recordAnswer(question, isCorrect)              // Ghi nhận trả lời
    }
    
    // Gọi UseCase cập nhật tiến trình
    private fun recordAnswer(question: QuizQuestion, correct: Boolean) {
        viewModelScope.launch {
            updateProgressUseCase(
                existing = existingProgress,
                userId = user.id, wordId = question.word.id, setId = question.word.vocabularySetId,
                correct = correct, intervalUnitMs = intervalUnitMs(), masteredThreshold = AppSettings.masteredThreshold
            )
        }
    }
}
```

---

## 2. Feature: Notification (Thông báo Push & In-app)

Tính năng này cho phép ứng dụng gửi thông báo nhắc nhở học tập (Local Push Notification qua AlarmManager) và thông báo hệ thống (In-app Notification từ Admin).

### 2.1. Luồng hoạt động (Activity Flow)
1. **Push Notification (Nhắc nhở học tập)**:
   - User cài đặt giờ nhắc nhở trong màn hình Settings.
   - `SettingsScreen` ghi giá trị giờ vào `AppSettings`.
   - `SettingsScreen` gọi `ScheduleReminderUseCase` để lên lịch với hệ thống Android bằng `AlarmManager`.
   - Đến thời gian hẹn, `AlarmManager` đánh thức `ReminderReceiver`.
   - `ReminderReceiver` gọi `NotificationHelper` hiển thị thông báo Push trên màn hình điện thoại, nhắc người dùng vào học.
2. **In-app Notification (Thông báo hệ thống)**:
   - Admin tạo thông báo bằng `AdminNotificationViewModel`. Dữ liệu lưu vào Collection `notifications` trên Firestore qua `FirestoreNotificationRepositoryImpl`.
   - Client mở ứng dụng, `NotificationViewModel` lắng nghe realtime các thay đổi và lấy danh sách hiển thị trên màn hình `NotificationListScreen`.

### 2.2. Chi tiết từng file Logic

#### `core/notification/ScheduleReminderUseCase.kt`
Lên lịch nhắc nhở hàng ngày.
```kotlin
class ScheduleReminderUseCase(
    private val reminderRepository: ReminderRepository
) {
    fun invoke(hour: Int, minute: Int) {
        // Hủy nhắc nhở cũ (nếu có)
        reminderRepository.cancelReminder()
        // Đặt nhắc nhở mới qua Repository (AlarmManager)
        reminderRepository.scheduleReminder(hour, minute)
    }
}
```

#### `core/notification/AlarmReminderRepository.kt`
Cài đặt `AlarmManager` của Android để hẹn giờ.
```kotlin
class AlarmReminderRepository(private val context: Context) : ReminderRepository {
    override fun scheduleReminder(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Intent dẫn tới ReminderReceiver
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        // Cài đặt thời gian kích hoạt bằng Calendar
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        
        // Hẹn giờ lặp lại hàng ngày vào giờ đã chỉ định
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
```

#### `core/notification/ReminderReceiver.kt`
BroadcastReceiver nhận tín hiệu khi đến giờ hẹn và kích hoạt Notification.
```kotlin
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Khởi tạo Helper
        val notificationHelper = NotificationHelper(context)
        // Hiển thị thông báo lên màn hình người dùng
        notificationHelper.showNotification(
            "Đến giờ học rồi!",
            "Hãy dành 10 phút vào MinLish học từ vựng nào!"
        )
    }
}
```

#### `features/notification/data/repository/FirestoreNotificationRepositoryImpl.kt` (In-app notification)
Tương tác với Firestore để lấy thông báo.
```kotlin
class FirestoreNotificationRepositoryImpl(private val firestore: FirebaseFirestore) : NotificationRepository {
    // Lấy thông báo thời gian thực (Realtime updates)
    override fun getNotifications(): Flow<List<Notification>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Sắp xếp mới nhất lên đầu
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Notification::class.java) }
                    trySend(list) // Gửi data vào Flow
                }
            }
        awaitClose { listener.remove() } // Hủy lắng nghe khi không dùng
    }
}
```

---

## 3. Feature: App Settings (Cài đặt cấu hình)

Tính năng quản lý các thiết lập cá nhân hóa của người dùng (ví dụ: chế độ ban đêm, nhắc nhở học tập, đơn vị thời gian của thuật toán).

### 3.1. Luồng hoạt động (Activity Flow)
1. Khi app khởi động (trong `MinLishApp`), `AppSettings.init(context)` được gọi để load các cài đặt đã lưu.
2. Tại màn hình `SettingsScreen`, người dùng tương tác UI (gạt switch, đổi giờ).
3. Mỗi thay đổi lập tức được ghi thẳng vào `SharedPreferences` thông qua các biến setter trong `AppSettings`.
4. Nếu thay đổi liên quan đến giờ nhắc nhở, `SettingsScreen` sẽ gọi `ScheduleReminderUseCase` để Android cập nhật lại `AlarmManager`.
5. Các module khác (ví dụ `QuizViewModel`) truy cập trực tiếp biến tĩnh từ `AppSettings` để lấy cấu hình (như `AppSettings.intervalUnit`).

### 3.2. Chi tiết từng file Logic

#### `core/util/AppSettings.kt`
File Singleton đóng gói SharedPreferences của Android.
```kotlin
object AppSettings {
    private lateinit var prefs: SharedPreferences

    // Hàm khởi tạo, được gọi 1 lần khi mở app
    fun init(context: Context) {
        prefs = context.getSharedPreferences("minlish_settings", Context.MODE_PRIVATE)
    }

    // Biến lưu trạng thái Bật/Tắt nhắc nhở (Sử dụng Getter/Setter)
    var isReminderEnabled: Boolean
        get() = prefs.getBoolean("reminder_enabled", true) // Mặc định bật
        set(value) = prefs.edit().putBoolean("reminder_enabled", value).apply() // Ghi trực tiếp vào ổ đĩa

    // Biến lưu Giờ nhắc nhở (Mặc định 20h)
    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 20)
        set(value) = prefs.edit().putInt("reminder_hour", value).apply()

    // Biến lưu Đơn vị thời gian SM-2 (DAYS / HOURS / MINUTES)
    var intervalUnit: String
        get() = prefs.getString("interval_unit", "DAYS") ?: "DAYS"
        set(value) = prefs.edit().putString("interval_unit", value).apply()
}
```

#### Tích hợp `SettingsScreen` (UI thao tác logic)
Tại giao diện `SettingsScreen.kt`, khi người dùng đổi giờ học:
```kotlin
// ... Trong Compose UI ...
TimePickerDialog(
    onTimeSelected = { hour, minute ->
        // 1. Lưu cấu hình vào AppSettings
        AppSettings.reminderHour = hour
        AppSettings.reminderMinute = minute
        
        // 2. Lên lịch báo thức mới với hệ thống
        if (AppSettings.isReminderEnabled) {
            scheduleReminderUseCase(hour, minute)
        }
    }
)
```

---

*Tài liệu trên đã liệt kê tất cả các file logic đóng vai trò cốt lõi trong Notification, Spaced Repetition (Quiz), và Settings, đồng thời giải thích rõ từng dòng quan trọng cũng như luồng hoạt động tổng thể.*
