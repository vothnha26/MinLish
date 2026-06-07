# MinLish - Giải thích Chi tiết Feature App Settings (Cài đặt)

Tài liệu này bao gồm **toàn bộ mã nguồn** và giải thích cặn kẽ TỪNG DÒNG CODE, cũng như LUỒNG HOẠT ĐỘNG của tính năng Cài đặt (App Settings) trong ứng dụng MinLish.

Tính năng này được cấu thành từ 2 file chính:
1. `AppSettings.kt`: Nơi chứa logic lưu trữ dữ liệu cục bộ (Local Storage) bằng `SharedPreferences`.
2. `SettingsScreen.kt`: Màn hình giao diện cài đặt (UI) và xử lý sự kiện lưu/đồng bộ dữ liệu.

---

## 1. Logic Lưu trữ: `AppSettings.kt`

`AppSettings` là một `object` (Singleton) trong Kotlin. Nghĩa là trong toàn bộ app chỉ có duy nhất một vùng nhớ chứa nó. Nó làm nhiệm vụ kết nối với `SharedPreferences` - một kho lưu trữ nhỏ trên Android dùng để lưu các cặp khóa-giá trị (Key-Value) đơn giản như cấu hình, tùy chọn của người dùng.

```kotlin
package com.edu.minlish.core.util

import android.content.Context
import android.content.SharedPreferences

object AppSettings {
    // Biến giữ tham chiếu đến kho lưu trữ SharedPreferences của Android
    private var prefs: SharedPreferences? = null

    // Hàm khởi tạo. Phải được gọi 1 lần khi app vừa mở lên (trong MainActivity hoặc ViewModel)
    fun init(context: Context) {
        // Mở file lưu trữ có tên "minlish_settings". Chế độ MODE_PRIVATE nghĩa là chỉ app này mới đọc được file này.
        prefs = context.applicationContext.getSharedPreferences("minlish_settings", Context.MODE_PRIVATE)
    }

    // --- CÁC BIẾN CẤU HÌNH ---
    // Cách viết Custom Getter/Setter này giúp việc lưu dữ liệu trở nên cực kỳ ngắn gọn.
    // Thay vì viết prefs.edit().putString(), ta chỉ cần gán: AppSettings.intervalUnit = "HOURS"

    // 1. Đơn vị thời gian cho thuật toán SM-2 (Phút, Giờ, Ngày)
    var intervalUnit: String
        get() = prefs?.getString("interval_unit", "DAYS") ?: "DAYS" // Nếu chưa lưu bao giờ thì mặc định là DAYS
        set(value) {
            prefs?.edit()?.putString("interval_unit", value)?.apply() // Lưu xuống ổ cứng
        }

    // 2. Ngưỡng thành thạo. (Bao nhiêu điểm/ngày thì coi như đã thuộc từ đó)
    var masteredThreshold: Int
        get() = prefs?.getInt("mastered_threshold", 30) ?: 30
        set(value) { prefs?.edit()?.putInt("mastered_threshold", value)?.apply() }

    // 3. Số từ mới cần học mỗi ngày (Mục tiêu hàng ngày)
    var dailyNewWordsTarget: Int
        get() = prefs?.getInt("daily_new_words_target", 10) ?: 10
        set(value) { prefs?.edit()?.putInt("daily_new_words_target", value)?.apply() }

    // 4. Giờ nhắc nhở học bài (Ví dụ: "09:00 PM")
    var reminderTime: String
        get() = prefs?.getString("reminder_time", "09:00 PM") ?: "09:00 PM"
        set(value) { prefs?.edit()?.putString("reminder_time", value)?.apply() }

    // 5. Cờ Bật/Tắt thông báo nhắc học
    var isNotificationsEnabled: Boolean
        get() = prefs?.getBoolean("is_notifications_enabled", true) ?: true
        set(value) { prefs?.edit()?.putBoolean("is_notifications_enabled", value)?.apply() }
        
    // (Bỏ qua giải thích một số biến phụ như lịch sử tra từ, streak)
}
```

**Luồng hoạt động của AppSettings:**
Khi bạn code `AppSettings.dailyNewWordsTarget = 15`, máy ảo Android sẽ chui vào file XML ẩn có tên `minlish_settings.xml` trên điện thoại, tìm cái khóa `daily_new_words_target` và ghi đè số `15` vào. Lần sau mở app lên, biến này sẽ tự động đọc lại số 15 đó ra.

---

## 2. Giao diện Cài đặt: `SettingsScreen.kt`

Đây là file vẽ ra màn hình Setting. Màn hình này cho phép người dùng thay đổi các thông số cấu hình và đặc biệt có tính năng **yêu cầu quyền gửi thông báo** (Permission) trên các máy Android đời mới.

### 2.1. Khởi tạo và Xin quyền (Permissions)

```kotlin
@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    
    // Tạo các biến State cục bộ để hứng dữ liệu từ AppSettings. 
    // Khi người dùng chỉnh sửa trên màn hình, các biến State này thay đổi trước, chứ chưa lưu thẳng vào ổ cứng vội.
    var notificationsEnabled by remember { mutableStateOf(AppSettings.isNotificationsEnabled) }
    var reminderTime by remember { mutableStateOf(AppSettings.reminderTime) }
    var dailyGoalVal by remember { mutableStateOf(AppSettings.dailyNewWordsTarget) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Lệnh này DÀNH CHO ANDROID 13+: Hộp thoại xin quyền gửi thông báo (Push Notifications).
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Khởi tạo công cụ đặt báo thức (AlarmManager)
        val reminderRepo = AlarmReminderRepository(context)
        val scheduleUseCase = ScheduleReminderUseCase(reminderRepo)
        
        if (isGranted) {
            // NẾU NGƯỜI DÙNG CHO PHÉP: Cài đặt giờ báo thức
            scheduleUseCase.schedule(reminderTime)
            Toast.makeText(context, "Đã kích hoạt...", Toast.LENGTH_SHORT).show()
        } else {
            // NẾU TỪ CHỐI: Báo lỗi
            Toast.makeText(context, "Quyền thông báo bị từ chối...", Toast.LENGTH_LONG).show()
        }
    }
```

### 2.2. Vẽ Layout: Bật/Tắt và Chọn giờ thông báo

```kotlin
    // Vẽ cái nút công tắc gạt Bật/Tắt (Switch)
    Switch(
        checked = notificationsEnabled,
        onCheckedChange = { notificationsEnabled = it }, // Khi gạt công tắc, cập nhật biến state
        colors = SwitchDefaults.colors(...)
    )

    // Khung chọn Giờ (TimePickerDialog của Android gốc)
    Row(
        modifier = Modifier
            .clickable(enabled = notificationsEnabled) {
                // 1. Phân tích chuỗi giờ hiện tại (VD: "09:00 PM" -> Tách ra số 9 và chữ PM)
                // 2. Mở cửa sổ đồng hồ xoay xoay của Android để chọn giờ mới
                android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute -> // Callback khi người dùng ấn OK chọn giờ xong
                        val amPm = if (hourOfDay >= 12) "PM" else "AM"
                        var hour = hourOfDay % 12
                        if (hour == 0) hour = 12
                        // Định dạng lại thành chuỗi "HH:MM AM/PM" và lưu vào biến state
                        reminderTime = String.format("%02d:%02d %s", hour, minute, amPm)
                    },
                    initialHour, initialMinute, false
                ).show()
            }
    ) {
        Text(text = reminderTime) // Hiển thị giờ đang chọn lên màn hình
    }
```

### 2.3. Vẽ Layout: Mục tiêu học từ mới

```kotlin
    // Hàng chứa nút Dấu Trừ (-) và Dấu Cộng (+) để chỉnh số từ mới mỗi ngày
    Row(...) {
        IconButton(
            onClick = { if (dailyGoalVal > 1) dailyGoalVal-- } // Trừ đi 1, tối thiểu là 1
        ) { Text("-") }
        
        Text(text = dailyGoalVal.toString()) // Số ở giữa
        
        IconButton(
            onClick = { if (dailyGoalVal < 100) dailyGoalVal++ } // Cộng thêm 1, tối đa 100
        ) { Text("+") }
    }
```

### 2.4. Lưu dữ liệu (`Save Settings`)

Đây là luồng quan trọng nhất. Khi người dùng bấm nút **"Save Settings"**, một hộp thoại (Dialog) hiện ra hỏi xác nhận. Khi bấm "OK" trên hộp thoại, đoạn code sau chạy:

```kotlin
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    
                    // BƯỚC 1: LƯU LOCAL (Ổ CỨNG ĐIỆN THOẠI)
                    // Chép dữ liệu từ các biến State trên màn hình vào trong Singleton AppSettings (để lưu vào SharedPreferences)
                    AppSettings.dailyNewWordsTarget = dailyGoalVal
                    AppSettings.isNotificationsEnabled = notificationsEnabled
                    AppSettings.reminderTime = reminderTime
                    
                    // BƯỚC 2: ĐỒNG BỘ LÊN ĐÁM MÂY (FIRESTORE)
                    // Tại sao phải đưa lên mây? Để khi User đổi điện thoại đăng nhập lại, vẫn giữ nguyên thiết lập Mục Tiêu Số Từ này.
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        coroutineScope.launch {
                            FirebaseFirestore.getInstance()
                                .collection("profiles")
                                .document(userId)
                                .update("dailyNewWordsTarget", dailyGoalVal)
                        }
                    }
                    
                    // BƯỚC 3: KÍCH HOẠT HỆ THỐNG BÁO THỨC CỦA ĐIỆN THOẠI
                    val reminderRepo = AlarmReminderRepository(context)
                    val scheduleUseCase = ScheduleReminderUseCase(reminderRepo)
                    
                    if (notificationsEnabled) {
                        // Nếu Android 13 trở lên (TIRAMISU), bắt buộc phải kiểm tra quyền Notification
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                scheduleUseCase.schedule(reminderTime) // Đã có quyền -> Bật báo thức
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) // Chưa có -> Hiện Popup xin quyền
                            }
                        } else {
                            // Android 12 trở xuống không cần xin quyền này
                            scheduleUseCase.schedule(reminderTime)
                        }
                    } else {
                        // Nếu gạt tắt thông báo -> Hủy báo thức
                        scheduleUseCase.cancel()
                    }
                }) { Text("OK") }
            },
            title = { Text("Settings Saved") },
            text = { Text("Your profile settings have been updated successfully.") }
        )
    }
```

---

## Tổng kết Luồng Hoạt động (Activity Flow)

1. **Khởi động**: App mở lên, `AppSettings` đọc dữ liệu cũ từ `SharedPreferences` (ổ cứng).
2. **Mở Màn hình Setting**: Giao diện `SettingsScreen` lấy dữ liệu từ `AppSettings` đắp lên màn hình (Ví dụ: hiện chữ 09:00 PM).
3. **Tương tác**: Người dùng bấm dấu cộng trừ, gạt công tắc, chọn lại giờ. Tất cả các thao tác này mới chỉ làm thay đổi biến ảo (State) trên RAM, giao diện vẽ lại nhưng chưa lưu.
4. **Lưu**: Người dùng bấm "Save Settings".
5. **Thực thi**:
   - Ghi đè vào ổ cứng (SharedPreferences).
   - Gắn ID người dùng và đẩy mục tiêu số từ lên Database Firebase.
   - Gọi hệ thống `AlarmManager` của hệ điều hành Android để thọc sâu vào đồng hồ hệ thống, bắt điện thoại phải réo chuông/bắn thông báo đúng vào giờ đó mỗi ngày. (Có kết hợp cơ chế xin quyền bảo mật Android 13).
