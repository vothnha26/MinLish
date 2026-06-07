# MinLish — Feature: Notification (Thông báo)

Tài liệu này mổ xẻ **toàn bộ 9 file** liên quan đến Notification, phân tích **từng dòng code**, **từng hàm**, và **luồng hoạt động** của hai hệ thống thông báo song song trong app.

---

## Tổng quan: 2 hệ thống thông báo trong MinLish

> MinLish có **hai hệ thống thông báo hoàn toàn độc lập**, phục vụ hai mục đích khác nhau:

| Hệ thống | Vị trí | Mục đích | Trạng thái |
|---|---|---|---|
| **System Reminder** | `core/notification/` + `core/util/` | Đặt báo thức hàng ngày nhắc người dùng học | ✅ Đang hoạt động |
| **In-App Notification List** | `features/notification/` | Hiển thị danh sách thông báo từ Firestore | ⚠️ Có màn hình UI nhưng **không có nơi nào ghi dữ liệu vào** |

---

## Sơ đồ kiến trúc tổng thể

```
┌─────────────────────────────────────────────────────────────────────┐
│                    HỆ THỐNG 1: SYSTEM REMINDER                      │
│                    (core/notification/ + core/util/)                 │
│                                                                      │
│  SettingsScreen ──► ScheduleReminderUseCase                         │
│                              │                                       │
│                              ▼                                       │
│                  AlarmReminderRepository                             │
│                  implements ReminderRepository                       │
│                              │                                       │
│                              ▼                                       │
│                     Android AlarmManager                             │
│                     (OS-level scheduler)                             │
│                              │ (đến giờ đã hẹn)                     │
│                              ▼                                       │
│                    ReminderReceiver                                  │
│                    (BroadcastReceiver)                               │
│                         │         │                                  │
│                         ▼         ▼                                  │
│              NotificationHelper   AlarmReminderRepository           │
│              (hiện thông báo)     (đặt lịch ngày mai)               │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                 HỆ THỐNG 2: IN-APP NOTIFICATION LIST                │
│                    (features/notification/)                          │
│                                                                      │
│  NotificationListScreen                                              │
│          │                                                           │
│          ▼                                                           │
│  NotificationViewModel ──► NotificationRepository (interface)       │
│                                      │                              │
│                                      ▼                              │
│              FirestoreNotificationRepositoryImpl                     │
│                      │                                              │
│                      ▼                                              │
│             Firestore: collection("notifications")                  │
│             ⚠️  Chỉ ĐỌC — không có code nào GHI vào collection này │
└─────────────────────────────────────────────────────────────────────┘
```

---

## PHẦN 1: HỆ THỐNG 1 — System Reminder

### File 1: `core/notification/ReminderRepository.kt`

```kotlin
// Khai báo package — nằm trong tầng core, không phụ thuộc vào feature nào
package com.edu.minlish.core.notification

// Domain interface — định nghĩa CONTRACT
// Ý nghĩa: "Tôi cần ai đó biết làm 2 việc: đặt lịch và hủy lịch"
// Bản thân interface KHÔNG quan tâm đến WorkManager, AlarmManager, hay bất kỳ thứ gì của Android
interface ReminderRepository {

    // Hàm 1: Đặt lịch nhắc nhở hàng ngày
    // timeString: chuỗi giờ dạng "HH:mm AM/PM", ví dụ "08:30 AM"
    fun scheduleDaily(timeString: String)

    // Hàm 2: Hủy tất cả lịch đang chạy
    fun cancelAll()
}
```

**Vai trò**: Đây là "hợp đồng" (contract) của tầng domain. Presentation layer (SettingsScreen) chỉ làm việc với interface này, không biết bên dưới dùng gì để thực thi.

---

### File 2: `core/notification/AlarmReminderRepository.kt`

```kotlin
package com.edu.minlish.core.notification

import android.annotation.SuppressLint
import android.app.AlarmManager       // API của Android để hẹn giờ chạy code
import android.app.PendingIntent       // "Giấy ủy quyền" cho Android OS thay mình thực thi Intent
import android.content.Context
import android.content.Intent
import android.os.Build                // Kiểm tra phiên bản Android
import java.util.Calendar             // Xử lý ngày giờ

// class này implements ReminderRepository — thực thi cụ thể bằng AlarmManager
class AlarmReminderRepository(
    private val context: Context    // Cần Context để gọi các Android service
) : ReminderRepository {

    // @SuppressLint("ScheduleExactAlarm"):
    //   Tắt cảnh báo lint vì từ Android 12, đặt exact alarm cần quyền SCHEDULE_EXACT_ALARM
    //   Code này chấp nhận rủi ro và giả định quyền đã được cấp trong AndroidManifest
    @SuppressLint("ScheduleExactAlarm")
    override fun scheduleDaily(timeString: String) {
        try {
            // Bước 1: Parse chuỗi "08:30 AM" → pair (8, 30)
            val (hour, minute) = parseTime(timeString)

            // Bước 2: Lấy service AlarmManager từ hệ thống Android
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Bước 3: Tạo Intent trỏ đến ReminderReceiver
            //   Khi báo thức reo, Android sẽ gửi broadcast đến ReminderReceiver
            val intent = Intent(context, ReminderReceiver::class.java)

            // Bước 4: Bọc Intent trong PendingIntent
            //   PendingIntent.getBroadcast: tạo loại PendingIntent để gửi broadcast
            //   requestCode = 0: dùng để identify, ở đây chỉ có 1 báo thức nên để 0
            //   FLAG_UPDATE_CURRENT: nếu PendingIntent đã tồn tại thì cập nhật, không tạo mới
            //   FLAG_IMMUTABLE: bắt buộc từ Android 12+ vì lý do bảo mật
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Bước 5: Tính thời điểm báo thức
            val now = Calendar.getInstance()           // Giờ hiện tại
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)        // Đặt giờ
                set(Calendar.MINUTE, minute)           // Đặt phút
                set(Calendar.SECOND, 0)                // Reset giây về 0
                set(Calendar.MILLISECOND, 0)           // Reset mili-giây về 0
                // Nếu giờ target đã qua trong hôm nay (ví dụ: đặt 08:00 nhưng giờ là 10:00)
                // → tự động cộng thêm 1 ngày → báo thức sẽ reo vào 08:00 NGÀY MAI
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }

            // Bước 6: Đặt báo thức với Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6+ (API 23+): dùng setExactAndAllowWhileIdle
                //   "Exact": báo thức chính xác, không bị delay
                //   "AllowWhileIdle": hoạt động kể cả khi máy đang trong Doze Mode
                //                    (chế độ tiết kiệm pin khi không dùng điện thoại)
                //   AlarmManager.RTC_WAKEUP: đánh thức máy dậy nếu đang ngủ
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,    // Thời điểm báo thức tính theo milli-giây
                    pendingIntent
                )
            } else {
                // Android 5 trở xuống: dùng setExact (không cần AllowWhileIdle vì chưa có Doze)
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // Bắt lỗi im lặng — tránh crash app nếu AlarmManager gặp sự cố
            e.printStackTrace()
        }
    }

    override fun cancelAll() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)

            // FLAG_NO_CREATE: CHỈ lấy PendingIntent NẾU ĐÃ TỒN TẠI
            //   Nếu chưa có báo thức nào đang chạy → trả về null (không tạo mới)
            //   Mục đích: tránh vô tình tạo PendingIntent mới khi chỉ muốn hủy
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)  // Xóa báo thức khỏi AlarmManager
                pendingIntent.cancel()              // Hủy PendingIntent để giải phóng bộ nhớ
            }
            // Nếu pendingIntent null → không có báo thức nào → không làm gì cả
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Hàm private: Parse chuỗi giờ dạng "08:30 AM" hoặc "20:30" (24h)
    private fun parseTime(timeString: String): Pair<Int, Int> {
        // "08:30 AM".trim().split(" ") → ["08:30", "AM"]
        val parts = timeString.trim().split(" ")

        // "08:30".split(":") → ["08", "30"]
        val timeParts = parts[0].split(":")
        var hour = timeParts[0].toInt()    // 8
        val minute = timeParts[1].toInt()  // 30

        // Lấy AM/PM nếu có (dùng getOrNull để tránh crash nếu không có)
        val amPm = parts.getOrNull(1)?.uppercase()  // "AM" hoặc null

        when {
            // PM và giờ < 12: cộng thêm 12 (ví dụ: 3 PM → 15)
            amPm == "PM" && hour < 12 -> hour += 12
            // AM và giờ = 12 (12:00 AM = nửa đêm): đổi về 0
            amPm == "AM" && hour == 12 -> hour = 0
            // Các trường hợp còn lại: giữ nguyên
        }

        return Pair(hour, minute)
    }
}
```

---

### File 3: `core/notification/ReminderReceiver.kt`

```kotlin
package com.edu.minlish.core.notification

import android.content.BroadcastReceiver    // Base class cho receiver nhận broadcast từ Android OS
import android.content.Context
import android.content.Intent
import com.edu.minlish.core.util.NotificationHelper

// BroadcastReceiver: Android OS sẽ tự khởi tạo và gọi onReceive()
//   khi đúng giờ đã hẹn trong AlarmManager
class ReminderReceiver : BroadcastReceiver() {

    // Hàm này được Android OS gọi khi báo thức reo
    override fun onReceive(context: Context?, intent: Intent?) {
        // Guard clause: nếu context null thì thoát ngay (không crash)
        if (context == null) return

        // BƯỚC 1: Hiển thị thông báo lên notification bar của điện thoại
        NotificationHelper.showReminderNotification(
            context,
            "MinLish Study Time! 🔥",                              // Tiêu đề thông báo
            "Keep up your streak! Tap here to practice your vocabulary words now."  // Nội dung
        )

        // BƯỚC 2: Tự lên lịch lại cho ngày mai
        //   Lý do: AlarmManager chỉ báo thức MỘT LẦN, không tự lặp lại
        //   Đây là kỹ thuật "self-rescheduling alarm" để tạo hiệu ứng lặp hàng ngày
        val reminderTime = com.edu.minlish.core.util.AppSettings.reminderTime
        val repository = AlarmReminderRepository(context)
        repository.scheduleDaily(reminderTime)  // → tự đặt cho ngày mai cùng giờ
    }
}
```

**Kỹ thuật Self-Rescheduling**:
- AlarmManager đặt báo thức → reo 1 lần → `ReminderReceiver.onReceive()` được gọi
- Bên trong `onReceive`, app **tự đặt lại báo thức** cho ngày hôm sau
- Cứ như vậy tạo ra vòng lặp vô tận hàng ngày

---

### File 4: `core/notification/ScheduleReminderUseCase.kt`

```kotlin
package com.edu.minlish.core.notification

// UseCase: lớp bọc mỏng nhưng quan trọng về mặt kiến trúc
// Tầng Presentation (SettingsScreen) gọi UseCase này
// → UseCase gọi Repository
// → Presentation KHÔNG BIẾT gì về AlarmManager hay chi tiết implement
class ScheduleReminderUseCase(
    private val reminderRepository: ReminderRepository  // Nhận interface, không phải implementation cụ thể
) {
    // Đặt nhắc nhở — delegate xuống repository
    fun schedule(timeString: String) = reminderRepository.scheduleDaily(timeString)

    // Hủy nhắc nhở — delegate xuống repository
    fun cancel() = reminderRepository.cancelAll()
}
```

**Tại sao cần UseCase khi chỉ là delegate?**
- Tách biệt tầng: Screen không import `AlarmManager` hay `AlarmReminderRepository`
- Dễ test: có thể inject mock `ReminderRepository` để unit test
- Mở rộng: sau này muốn thêm logic (ví dụ: log analytics) chỉ cần sửa UseCase

---

### File 5: `core/util/NotificationHelper.kt`

```kotlin
package com.edu.minlish.core.util

import android.app.NotificationChannel       // Kênh thông báo (bắt buộc Android 8+)
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat     // Builder để tạo notification
import androidx.core.app.NotificationManagerCompat  // Compat để gửi notification
import com.edu.minlish.MainActivity

// object: Singleton — chỉ có 1 instance trong toàn app
// Nhiệm vụ DUY NHẤT: BUILD và SHOW notification lên thanh thông báo của điện thoại
// Không liên quan đến lên lịch (đó là việc của ScheduleReminderUseCase)
object NotificationHelper {

    // Các hằng số cấu hình notification channel
    private const val CHANNEL_ID = "study_reminder_channel"   // ID duy nhất của channel
    private const val CHANNEL_NAME = "Study Reminders"         // Tên hiển thị trong cài đặt hệ thống
    private const val CHANNEL_DESC = "Daily study reminder notifications"  // Mô tả
    private const val NOTIFICATION_ID = 1001                   // ID để update/xóa notification sau này

    // Hàm duy nhất được gọi từ bên ngoài
    fun showReminderNotification(context: Context, title: String, message: String) {
        // Bước 1: Tạo channel (bắt buộc Android 8+, Android thấp hơn bỏ qua)
        createNotificationChannel(context)

        // Bước 2: Tạo Intent mở MainActivity khi người dùng bấm vào thông báo
        val intent = Intent(context, MainActivity::class.java).apply {
            // FLAG_ACTIVITY_NEW_TASK: cần thiết khi start Activity từ ngoài app context
            // FLAG_ACTIVITY_CLEAR_TASK: xóa back stack cũ, mở Activity ở trạng thái mới
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Bước 3: Bọc Intent trong PendingIntent
        //   requestCode = 0, FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE: tương tự AlarmReminderRepository
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Bước 4: Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icon nhỏ hiển thị trên status bar
            .setContentTitle(title)         // Dòng tiêu đề lớn
            .setContentText(message)        // Dòng nội dung nhỏ
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Độ ưu tiên: mặc định (không pop-up)
            .setAutoCancel(true)            // Tự xóa notification khi người dùng bấm vào
            .setContentIntent(pendingIntent) // Bấm vào → mở MainActivity
            .build()

        // Bước 5: Gửi notification ra màn hình
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Android 13+ yêu cầu quyền POST_NOTIFICATIONS phải được cấp thủ công bởi người dùng
            // Nếu chưa cấp → SecurityException → bắt lại, bỏ qua, không crash app
        }
    }

    // Hàm private: Tạo Notification Channel
    private fun createNotificationChannel(context: Context) {
        // Chỉ cần tạo channel trên Android 8.0 (API 26) trở lên
        // Các máy Android cũ hơn không có khái niệm channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT  // Hiển thị thông báo nhưng không âm thanh to
            ).apply {
                description = CHANNEL_DESC
            }
            // Đăng ký channel với hệ thống
            // Gọi createNotificationChannel nhiều lần với cùng CHANNEL_ID: không sao, hệ thống tự bỏ qua
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
```

---

## PHẦN 2: HỆ THỐNG 2 — In-App Notification List

### File 6: `features/notification/domain/model/Notification.kt`

```kotlin
package com.edu.minlish.features.notification.domain.model

import java.util.Date

// Data class: model biểu diễn một thông báo trong app
data class Notification(
    val id: String = "",        // ID document trên Firestore (gán sau khi đọc về)
    val title: String = "",     // Tiêu đề thông báo
    val message: String = "",   // Nội dung thông báo
    val createdAt: Date = Date(), // Thời điểm tạo — Firestore Timestamp tự convert sang Date
    val isSystem: Boolean = true  // true = thông báo hệ thống, false = thông báo khác
)
```

Tất cả field đều có **giá trị mặc định** để Firestore có thể tự động deserialize bằng `toObject()`.

---

### File 7: `features/notification/domain/repository/NotificationRepository.kt`

```kotlin
package com.edu.minlish.features.notification.domain.repository

import com.edu.minlish.features.notification.domain.model.Notification

// Domain interface cho notification list feature
interface NotificationRepository {
    // Lấy danh sách thông báo từ Firestore
    // suspend: phải gọi từ coroutine vì là tác vụ bất đồng bộ (network call)
    // Result<List<Notification>>: Kotlin Result wrapper — bọc success hoặc failure
    suspend fun getNotifications(): Result<List<Notification>>

    // Đăng thông báo mới lên Firestore
    // Hàm này tồn tại trong interface nhưng KHÔNG có nơi nào trong app gọi nó
    suspend fun publishNotification(notification: Notification): Result<Unit>
}
```

> ⚠️ **Điểm quan trọng**: `publishNotification` được khai báo nhưng **không bao giờ được gọi** trong bất kỳ ViewModel hay Screen nào. Collection `notifications` trên Firestore sẽ luôn rỗng trừ khi được thêm dữ liệu thủ công.

---

### File 8: `features/notification/data/repository/FirestoreNotificationRepositoryImpl.kt`

```kotlin
package com.edu.minlish.features.notification.data.repository

import com.edu.minlish.features.notification.domain.model.Notification
import com.edu.minlish.features.notification.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await       // Extension để dùng Firestore Task với coroutine
import kotlinx.coroutines.withTimeout       // Đặt timeout cho coroutine

// Implement cụ thể NotificationRepository bằng Firestore
class FirestoreNotificationRepositoryImpl(
    // Mặc định lấy Firestore singleton, có thể inject khác cho test
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {

    override suspend fun getNotifications(): Result<List<Notification>> {
        return try {
            withTimeout(10000) {  // Timeout 10 giây — nếu quá thời gian sẽ throw TimeoutCancellationException
                val snapshot = firestore
                    .collection("notifications")         // Trỏ đến collection "notifications" trên Firestore
                    .orderBy("createdAt", Query.Direction.DESCENDING)  // Sắp xếp mới nhất lên trên
                    .get()                               // Lấy toàn bộ documents
                    .await()                             // Chờ Firestore trả kết quả (coroutine-friendly)

                // Duyệt từng document, convert sang Notification object
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        // toObject(): Firestore tự map field → thuộc tính của data class
                        // copy(id = doc.id): gán thêm id vì Firestore không tự gán field "id"
                        doc.toObject(Notification::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        // Nếu 1 document bị lỗi (sai format) → bỏ qua, không crash toàn bộ list
                        null
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            // Lỗi network, timeout, hoặc bất kỳ exception nào → trả về failure
            Result.failure(e)
        }
    }

    override suspend fun publishNotification(notification: Notification): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("notifications")
                    .add(notification)   // Thêm document mới (Firestore tự tạo ID)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

### File 9: `features/notification/presentation/viewmodel/NotificationViewModel.kt`

```kotlin
package com.edu.minlish.features.notification.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.minlish.features.notification.data.repository.FirestoreNotificationRepositoryImpl
import com.edu.minlish.features.notification.domain.model.Notification
import com.edu.minlish.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Sealed class: định nghĩa tất cả trạng thái có thể của màn hình
// Mỗi state là 1 class con riêng biệt
sealed class NotificationUiState {
    object Loading : NotificationUiState()                              // Đang tải
    data class Success(val notifications: List<Notification>) : NotificationUiState()  // Tải xong
    data class Error(val message: String) : NotificationUiState()      // Lỗi
}

class NotificationViewModel(
    // Mặc định tự tạo implementation, có thể inject khác cho test
    private val repository: NotificationRepository = FirestoreNotificationRepositoryImpl()
) : ViewModel() {

    // StateFlow: luồng dữ liệu 1 chiều, UI observe để re-render khi thay đổi
    // Bắt đầu ở trạng thái Loading
    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()
    //   asStateFlow(): expose dạng read-only, UI không thể tự thay đổi

    // init block: chạy ngay khi ViewModel được tạo
    init {
        loadNotifications()  // Tự động load khi màn hình được mở
    }

    // Hàm load dữ liệu — có thể gọi lại để refresh
    fun loadNotifications() {
        _uiState.update { NotificationUiState.Loading }  // Reset về Loading trước
        viewModelScope.launch {  // Chạy coroutine trong scope gắn với lifecycle của ViewModel
            repository.getNotifications()
                .onSuccess { list ->
                    // Tải thành công → chuyển sang Success với danh sách
                    _uiState.update { NotificationUiState.Success(list) }
                }
                .onFailure { e ->
                    // Tải thất bại → chuyển sang Error với message lỗi
                    _uiState.update { NotificationUiState.Error(e.message ?: "Failed to load notifications") }
                }
        }
    }
}
```

---

### File 10: `features/notification/presentation/NotificationListScreen.kt`

```kotlin
// @OptIn: Cho phép dùng API đang ở trạng thái thử nghiệm (ExperimentalMaterial3Api)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    onBack: () -> Unit,                              // Lambda điều hướng: xử lý nút back
    viewModel: NotificationViewModel = viewModel()  // Inject ViewModel, Compose tự quản lý lifecycle
) {
    // Observe StateFlow của ViewModel
    // collectAsStateWithLifecycle(): chỉ collect khi UI đang active (tiết kiệm tài nguyên)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(                    // AppBar căn giữa tiêu đề
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {     // Nút back
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            val state = uiState  // Lấy snapshot để Kotlin smart cast hoạt động

            // Xử lý từng trạng thái — exhaustive when (phải xử lý hết mọi case)
            when (state) {

                // TRẠNG THÁI 1: Đang loading → hiển thị spinner giữa màn hình
                is NotificationUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                }

                // TRẠNG THÁI 2: Lỗi → hiển thị thông báo lỗi đỏ giữa màn hình
                is NotificationUiState.Error -> {
                    Text(
                        text = state.message,   // Smart cast: state ở đây chắc chắn là Error
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // TRẠNG THÁI 3: Thành công
                is NotificationUiState.Success -> {
                    val notifications = state.notifications  // Smart cast: lấy list

                    if (notifications.isEmpty()) {
                        // Danh sách rỗng → Empty state: icon + text "No notifications yet"
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray   // Icon xám nhạt
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notifications yet",
                                color = Color.Gray,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // Có data → hiển thị danh sách scroll được
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),      // Padding xung quanh list
                            verticalArrangement = Arrangement.spacedBy(12.dp)  // Khoảng cách giữa item
                        ) {
                            items(notifications) { notification ->
                                NotificationItem(notification = notification)  // Render từng item
                            }
                        }
                    }
                }
            }
        }
    }
}

// Component con: render 1 notification item
@Composable
fun NotificationItem(notification: Notification) {
    // Format ngày giờ: "07 Jun 2026, 08:30 AM"
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(notification.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Border, shape = RoundedCornerShape(12.dp)),  // Viền card
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,  // Tiêu đề trái, badge phải
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tiêu đề (bên trái)
                Text(
                    text = notification.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111)
                )
                // Badge loại thông báo (bên phải): "System" hoặc "Alert"
                Text(
                    text = if (notification.isSystem) "System" else "Alert",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Nội dung thông báo
            Text(text = notification.message, fontSize = 13.sp, color = Color(0xFF6B6B6B))
            Spacer(modifier = Modifier.height(8.dp))
            // Thời gian tạo (nhỏ, màu xám nhạt)
            Text(text = formattedDate, fontSize = 10.sp, color = Color.LightGray)
        }
    }
}
```

---

## Luồng hoạt động đầy đủ

### Luồng 1: Người dùng bật/chỉnh giờ nhắc học

```
1. Người dùng mở SettingsScreen
   → reminderTime = AppSettings.reminderTime (đọc từ SharedPreferences)

2. Người dùng bấm chọn giờ → TimePicker hiện ra
   → reminderTime state cập nhật = "08:30 AM"

3. Người dùng bấm "Save"
   → AppSettings.reminderTime = "08:30 AM"   (ghi vào SharedPreferences)
   → AlarmReminderRepository(context) được tạo
   → ScheduleReminderUseCase(repo) được tạo
   → scheduleUseCase.schedule("08:30 AM") được gọi
   → AlarmManager.setExactAndAllowWhileIdle() đặt báo thức lúc 08:30

4. Đến 08:30 sáng hôm sau
   → Android OS kích hoạt ReminderReceiver.onReceive()
   → NotificationHelper.showReminderNotification() → thông báo hiện trên màn hình
   → AlarmReminderRepository.scheduleDaily("08:30 AM") → đặt lại cho ngày mai

5. Lặp lại bước 4 mỗi ngày
```

### Luồng 2: Người dùng mở màn hình Notifications

```
1. User navigate đến NotificationListScreen
   → NotificationViewModel được tạo
   → init { loadNotifications() } chạy ngay

2. _uiState = Loading → UI hiển thị spinner

3. viewModelScope.launch {
     repository.getNotifications()  // Gọi Firestore
   }
   → Firestore query collection("notifications") order by createdAt DESC

4a. Firestore trả về danh sách rỗng (vì không có code nào publish thông báo)
    → _uiState = Success(emptyList())
    → UI hiển thị: icon 🔔 + "No notifications yet"

4b. Nếu Firestore lỗi (không có mạng, timeout...)
    → _uiState = Error("...")
    → UI hiển thị text đỏ ở giữa màn hình
```

---

## Phân tích: Hệ thống nào đang hoạt động thực sự?

| Tiêu chí | Hệ thống 1 (System Reminder) | Hệ thống 2 (In-App List) |
|---|---|---|
| **Kết nối vào Navigation** | ✅ Có (Settings gọi trực tiếp) | ✅ Có (có màn hình riêng) |
| **Dữ liệu thực tế** | ✅ Hoạt động | ⚠️ Collection Firestore rỗng |
| **Ai ghi dữ liệu** | AlarmManager tự xử lý | Không có code nào gọi `publishNotification` |
| **Tình trạng** | ✅ Hoàn chỉnh | ⚠️ UI đủ nhưng không có nguồn dữ liệu |
| **Người dùng thấy gì** | Thông báo hệ thống nhắc học | Luôn hiển thị "No notifications yet" |

> **Kết luận**: Hệ thống 2 (In-App Notification List) là một **tính năng chưa hoàn thiện** — kiến trúc đầy đủ (Model → Repository → ViewModel → UI), nhưng thiếu phần **producer** ghi dữ liệu vào Firestore. Để hoàn thiện, cần có code gọi `publishNotification()` khi có sự kiện đáng thông báo (ví dụ: hoàn thành bộ từ, đạt streak mới...).
