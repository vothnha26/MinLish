# MinLish - Giải thích chi tiết Features: Learning (Học ngắt quãng & Quiz), Notification (Thông báo), Settings (Cài đặt)

Tài liệu này giải thích chi tiết **từng dòng code phần logic** (Model, Repository, UseCase, ViewModel) và **mục đích sử dụng** của các file thuộc 3 module: `learning`, `notification` và `settings`.

---

## Phần 1: Feature `notification` (Hệ thống thông báo)

Chịu trách nhiệm nhận thông báo từ Admin gửi cho tất cả người dùng, và hiển thị danh sách thông báo.

### 1. `domain/model/Notification.kt`
- **Mục đích**: Khai báo cấu trúc dữ liệu của 1 thông báo.
```kotlin
package com.edu.minlish.features.notification.domain.model
import java.util.Date

data class Notification(
    val id: String = "", // ID thông báo trên Firestore
    val title: String = "", // Tiêu đề
    val message: String = "", // Nội dung
    val createdAt: Date = Date(), // Thời gian tạo
    val isSystem: Boolean = true // Đánh dấu là thông báo hệ thống (Admin gửi)
)
```

### 2. `domain/repository/NotificationRepository.kt`
- **Mục đích**: Định nghĩa Interface (hợp đồng) cho các thao tác với thông báo.
```kotlin
interface NotificationRepository {
    suspend fun getNotifications(): Result<List<Notification>> // Lấy danh sách thông báo
    suspend fun publishNotification(notification: Notification): Result<Unit> // Admin đăng thông báo mới
}
```

### 3. `data/repository/FirestoreNotificationRepositoryImpl.kt`
- **Mục đích**: Triển khai thực tế Interface trên bằng Firebase Firestore.
```kotlin
class FirestoreNotificationRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {

    override suspend fun getNotifications(): Result<List<Notification>> = try {
        withTimeout(10000) { // Giới hạn 10s tải, quá giờ báo lỗi mạng
            val snapshot = firestore.collection("notifications") // Trỏ vào bảng notifications
                .orderBy("createdAt", Query.Direction.DESCENDING) // Sắp xếp mới nhất lên đầu
                .get().await() // Tải 1 lần
            
            // Map từ Document Firestore sang Object Kotlin
            val list = snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(Notification::class.java)?.copy(id = doc.id) } 
                catch (e: Exception) { null }
            }
            Result.success(list)
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun publishNotification(notification: Notification): Result<Unit> = try {
        withTimeout(10000) {
            firestore.collection("notifications").add(notification).await() // Thêm mới document
        }
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
```

### 4. `presentation/viewmodel/NotificationViewModel.kt`
- **Mục đích**: Chứa logic màn hình xem danh sách thông báo của User.
```kotlin
sealed class NotificationUiState { // Trạng thái màn hình
    object Loading : NotificationUiState()
    data class Success(val notifications: List<Notification>) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

class NotificationViewModel(
    private val repository: NotificationRepository = FirestoreNotificationRepositoryImpl()
) : ViewModel() {
    var uiState by mutableStateOf<NotificationUiState>(NotificationUiState.Loading) private set

    init { loadNotifications() } // Vừa vào màn hình là tự gọi hàm tải data

    fun loadNotifications() {
        uiState = NotificationUiState.Loading
        viewModelScope.launch {
            repository.getNotifications()
                .onSuccess { list -> uiState = NotificationUiState.Success(list) }
                .onFailure { e -> uiState = NotificationUiState.Error(e.message ?: "Failed") }
        }
    }
}
```

### 5. `presentation/viewmodel/AdminNotificationViewModel.kt`
- **Mục đích**: Dành riêng cho màn hình Admin để bắn thông báo.
```kotlin
class AdminNotificationViewModel(
    private val repository: NotificationRepository = FirestoreNotificationRepositoryImpl()
) : ViewModel() {
    var publishState by mutableStateOf<PublishUiState>(PublishUiState.Idle) private set // Trạng thái đăng bài

    fun publishNotification(title: String, message: String) {
        if (title.isBlank() || message.isBlank()) { // Chặn rỗng
            publishState = PublishUiState.Error("Fields cannot be empty")
            return
        }
        publishState = PublishUiState.Loading
        viewModelScope.launch {
            val notification = Notification(title = title, message = message, createdAt = Date(), isSystem = true)
            repository.publishNotification(notification)
                .onSuccess { publishState = PublishUiState.Success }
                .onFailure { e -> publishState = PublishUiState.Error(e.message ?: "Failed") }
        }
    }
    fun resetState() { publishState = PublishUiState.Idle } // Reset sau khi đăng xong
}
```

### Các file UI của Notification
- `presentation/AdminNotificationScreen.kt`: Màn hình cho Admin có ô nhập Title, Message và nút "Publish".
- `presentation/NotificationListScreen.kt`: Màn hình cho User xem danh sách thông báo, hiển thị dưới dạng danh sách (LazyColumn).

---

## Phần 2: Feature `learning` (Flashcard SM-2 & Quiz Game)

Đây là module phức tạp nhất, chứa thuật toán ôn tập ngắt quãng (Spaced Repetition SM-2) và sinh câu hỏi trắc nghiệm/ghép từ.

### 1. Các Model Dữ liệu (`domain/model`)
- **`QuizQuestion.kt`**: Cấu trúc 1 câu hỏi Game.
```kotlin
enum class QuestionType { MULTIPLE_CHOICE, SPELLING, MATCHING } // 3 chế độ chơi
data class QuizQuestion(
    val type: QuestionType, // Loại câu hỏi
    val word: VocabularyWord, // Từ vựng gốc
    val options: List<String> = emptyList(), // Các đáp án (Trắc nghiệm)
    val correctIndex: Int = -1, // Vị trí đáp án đúng
    val matchingPairs: List<Pair<String, String>> = emptyList() // Các cặp Anh-Việt (Cho game Nối từ)
)
```
- **`UserWordProgress.kt`**: Lưu tiến độ học SM-2 của 1 từ.
```kotlin
data class UserWordProgress(
    val id: String = "", val userId: String = "", val wordId: String = "", val setId: String = "",
    val easeFactor: Float = 2.5f, // Hệ số dễ/khó (Mặc định 2.5 của SM-2)
    val interval: Int = 0, // Số ngày/giờ/phút chờ cho lần ôn tiếp theo
    val repetitions: Int = 0, // Chuỗi trả lời đúng liên tiếp
    val nextReviewDate: Date = Date(), // Ngày giờ đến hạn ôn
    val lastReviewedAt: Date = Date(), // Lần cuối học
    val status: String = "learning" // learning (mới), reviewing (đang ôn), mastered (thuộc lòng)
)
```
- **`UserReviewLog.kt`**: Lịch sử ôn tập (dành cho biểu đồ thống kê sau này). Lưu lại user đã bấm Nút Hard/Good/Easy.

### 2. `data/repository/FirestoreLearningRepositoryImpl.kt`
- **Mục đích**: Giao tiếp với Firestore để tải và lưu tiến trình học.
- **Giải thích Logic**:
  - `getDueWords`: Trộn danh sách từ gốc `VocabularyWord` và tiến độ `UserWordProgress` của User. Chỉ lọc ra các từ chưa học (`progress == null`) hoặc đã đến hạn ôn (`nextReviewDate <= now`).
  - `getDailySessionWords`: Tách biệt lấy ra đúng X từ mới (`targetNew`) và Y từ cũ (`targetReview`) theo cấu hình AppSettings.
  - `updateProgress`: Lưu/Cập nhật object `UserWordProgress` lên collection `user_word_progress`.
  - `logReview`: Lưu object `UserReviewLog` lên bảng `user_review_logs`.

### 3. `domain/usecase/UpdateWordProgressUseCase.kt` (Lõi Thuật toán Quiz Game)
- **Mục đích**: Tính toán thuật toán cho Quiz (Đúng/Sai). Chú ý FlashcardViewModel có hàm tự tính riêng cho 4 nút (Again/Hard/Good/Easy).
```kotlin
class UpdateWordProgressUseCase(private val repository: LearningRepository) {
    suspend operator fun invoke(
        existing: UserWordProgress?, userId: String, wordId: String, setId: String,
        correct: Boolean, // User trả lời Đúng hay Sai
        intervalUnitMs: Long, masteredThreshold: Int
    ): Result<Unit> {
        val now = Date()
        val updated = if (existing == null) {
            // Từ mới hoàn toàn -> Trả lời phát tạo mới luôn interval = 1
            UserWordProgress(..., interval = 1, repetitions = if (correct) 1 else 0, easeFactor = 2.5f, status = "learning")
        } else {
            // Từ cũ
            var rep = existing.repetitions; var intv = existing.interval; var fac = existing.easeFactor
            if (correct) { // Nếu ĐÚNG trong Quiz
                rep += 1 // Tăng chuỗi
                intv = when (rep) { 1 -> 1; 2 -> 6; else -> (intv * fac).toInt().coerceAtLeast(1) } // Tính ngày ôn tiếp
            } else { // Nếu SAI trong Quiz
                rep = 0 // Đứt chuỗi
                intv = 1 // Học lại ngay
                fac = (fac - 0.2f).coerceAtLeast(1.3f) // Phạt trừ điểm Ease Factor
            }
            val status = when {
                intv > masteredThreshold -> "mastered" // Nếu interval (ví dụ 30 ngày) -> Thuộc lòng
                rep >= 1 -> "reviewing"
                else -> "learning"
            }
            existing.copy(repetitions = rep, interval = intv, easeFactor = fac, nextReviewDate = Date(now.time + intv * intervalUnitMs), status = status)
        }
        return repository.updateProgress(updated) // Ghi database
    }
}
```

### 4. `domain/usecase/BuildQuizQuestionsUseCase.kt`
- **Mục đích**: Trộn từ vựng thành các câu hỏi Game.
- **Logic**:
  - Tùy chọn `modes` (Ví dụ: "MULTIPLE_CHOICE,SPELLING,MATCHING").
  - `shuffledWords`: Xáo trộn list từ đầu vào.
  - Vòng lặp chia từ vào 3 giỏ (Trắc nghiệm, Gõ chữ, Ghép cặp) ngẫu nhiên.
  - Xử lý dư thừa: Ghép cặp bắt buộc phải là bội số của 4. Các từ thừa bị ném ngược lại giỏ Trắc nghiệm/Gõ chữ.
  - Trắc nghiệm: Lấy các "Nghĩa tiếng Việt" của các từ KHÁC trong danh sách làm phương án nhiễu (distractors). Nếu thiếu thì lấy từ `fallbackDistractors`. Trộn phương án đúng và 3 nhiễu.

### 5. `presentation/viewmodel/FlashcardViewModel.kt`
- **Mục đích**: Xử lý lật thẻ và thuật toán SM-2 đầy đủ 4 nút (Again, Hard, Good, Easy).
- **Logic `calculateSM2`**:
  - Again (0 điểm), Hard (3 điểm), Good (4 điểm), Easy (5 điểm).
  - Công thức chuẩn: `nextEaseFactor = current.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))`
  - Cập nhật local cache ngay lập tức vào `SessionDataManager` để quay ra trang Chủ (`Home`) thấy vòng tròn tiến độ được làm đầy ngay lập tức không bị delay tải mạnng.
  - Nút Again -> Cập nhật nhưng giữ nguyên thẻ hiện tại ở trước mắt để học lại luôn.

### 6. `presentation/viewmodel/QuizViewModel.kt`
- **Mục đích**: Quản lý State cho 3 Mini game trong Quiz.
- **Logic**:
  - Có các biến State: `currentIndex` (Câu đang làm), `score` (Điểm), `selectedOptionIndex` (Chặn chọn 2 lần).
  - Trắc nghiệm: `selectOption(index)` -> So sánh `correctIndex` -> Gọi `UpdateWordProgressUseCase`.
  - Spelling: So sánh `spellingInput.trim().lowercase()` với từ gốc.
  - Matching (Ghép cặp): Duy trì list thẻ Anh đang chọn `selectedEnglishCard` và thẻ Việt `selectedVietnameseCard`. Bấm 2 thẻ -> Ktra nếu khớp cặp -> Xóa khỏi UI -> Cộng điểm.

### Các file UI của Learning
- `presentation/FlashcardScreen.kt`: Màn hình vuốt thẻ 3D (Animatable). Có 4 nút cường độ ở dưới.
- `presentation/QuizGameScreen.kt`: Màn hình chứa 3 giao diện đổi liên tục tùy loại câu hỏi.
- `presentation/GameHubScreen.kt`: Màn hình chọn chế độ chơi (Cho phép User tick chọn chơi Trắc nghiệm, hay Gõ chữ, hay Nối từ).
- `presentation/WordDetailScreen.kt`: Màn hình chi tiết một từ, có loa phát âm, ví dụ.

---

## Phần 3: Feature `settings` (Cài đặt)

### 1. `presentation/SettingsScreen.kt`
- **Mục đích**: UI Cài đặt và logic gọi Android System để kích hoạt thông báo ngầm định kỳ.
- **Giải thích Logic**:
  - Nó đọc/ghi biến vào object `AppSettings` (Sử dụng `SharedPreferences` ở module `core.util`).
  - **Daily Reminders Switch**: Công tắc bật tắt. Nếu Bật -> Yêu cầu quyền `Manifest.permission.POST_NOTIFICATIONS` (Cho Android 13+).
  - **Reminder Time**: Sử dụng `android.app.TimePickerDialog` của hệ điều hành để cho user chọn giờ. Parse string giờ sang Milliseconds.
  - **Spaced Repetition Unit**: Chọn đơn vị thời gian (DAYS/HOURS/MINUTES). Bình thường học ngắt quãng tính bằng ngày, nhưng có MINUTES để *Giảng viên chấm điểm Demo nhanh*.
  - **Save Settings**: Nút lưu -> Vừa lưu SharedPreferences, Vừa bắn lên bảng `profiles` trên Firestore, Vừa gọi `ScheduleReminderUseCase` để tạo `WorkManager` hẹn giờ cho hệ điều hành.

---
*Báo cáo kết thúc việc giải thích các module Thông Báo, Cài Đặt và Học Ngắt Quãng/Game.*
