---
title: "Hướng dẫn Tính năng & Sơ đồ Luồng Nghiệp vụ MinLish"
description: "Cẩm nang hướng dẫn chi tiết các phân hệ chức năng cốt lõi của MinLish, thuật toán Streak và luồng nghiệp vụ thông báo, học tập."
---

# Hướng dẫn Tính năng & Sơ đồ Luồng Nghiệp vụ MinLish

Tài liệu này cung cấp hướng dẫn chi tiết về cách hoạt động của 10 phân hệ tính năng trong ứng dụng MinLish, thuật toán Streak (Chuỗi ngày học liên tiếp) và cơ chế lên lịch gửi thông báo nhắc nhở bằng WorkManager.

---

## 1. Tổng quan 10 phân hệ tính năng (Feature List)

MinLish được chia tách thành các module chức năng độc lập nhằm mục tiêu dễ dàng mở rộng và bảo trì:

| Tên Module | Vai trò & Tính năng chính | Các File Code chính |
| :--- | :--- | :--- |
| **`auth`** | Đăng ký, đăng nhập và khôi phục mật khẩu qua Firebase Auth. | [LoginScreen.kt](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/auth/presentation/LoginScreen.kt) |
| **`onboarding`** | Màn hình khởi động và các slide giới thiệu ứng dụng ban đầu. | [SplashScreen.kt](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/onboarding/presentation/SplashScreen.kt) |
| **`home`** | Trang chủ hiển thị tiến độ học ngày hôm nay, Streak và phím tắt nhanh. | [HomeScreen.kt](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/home/presentation/HomeScreen.kt) |
| **`library`** | Quản lý bộ từ vựng cá nhân, nhập/xuất file CSV từ vựng. | [LibraryScreen.kt](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/library/presentation/LibraryScreen.kt) |
| **`translate`** | Dịch thuật đảo chiều (Anh-Việt) và trích xuất từ vựng bằng Gemini AI. | [TranslateAndLookupScreen.kt](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/library/presentation/TranslateAndLookupScreen.kt) |
| **`learning`** | Học từ vựng bằng thẻ Flashcard (lật 3D) và mini game trắc nghiệm. | [FlashcardScreen.kt](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/learning/presentation/FlashcardScreen.kt) |
| **`speaking`** | Luyện nói tiếng Anh AI, chấm điểm và gợi ý ngữ pháp chuẩn IELTS/TOEIC. | [SpeakingScreen.kt](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/speaking/presentation/SpeakingScreen.kt) |
| **`stats`** | Thống kê tiến độ (Progress), trang bị Streak Freeze để giữ chuỗi học. | [StatsScreen.kt](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/stats/presentation/StatsScreen.kt) |
| **`profilesetup`**| Thiết lập hồ sơ ban đầu (Mục tiêu học, cấp độ hiện tại). | [ProfileSetupScreen.kt](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/profilesetup/presentation/ProfileSetupScreen.kt) |
| **`settings`** | Cấu hình bật/tắt nhắc nhở hàng ngày bằng TimePickerDialog hệ thống. | [SettingsScreen.kt](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/settings/presentation/SettingsScreen.kt) |

---

## 2. Nghiệp vụ Học tập (Learning Flow)

Ứng dụng khuyến khích người dùng học thông qua việc chọn học bộ từ trong thư viện. Màn hình **Game Hub** đóng vai trò điều phối người dùng chọn chế độ học: Flashcard hoặc Chơi Quiz Game.

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant Hub as GameHubScreen
    participant Flash as FlashcardScreen
    participant Quiz as QuizGameScreen
    participant VM as StatsViewModel
    participant Prefs as AppSettings

    User->>Hub: Click "Study Now" bộ từ vựng (IELTS)
    Hub->>Flash: Navigate sang FlashcardScreen (Line 360)
    Note over Flash: Hiển thị thẻ lật 3D từ vựng tiếng Anh
    User->>Flash: Bấm lật mặt sau để xem IPA, nghĩa tiếng Việt
    User->>Flash: Click "Play Quiz" để tự kiểm tra
    Flash->>Quiz: Điều hướng sang QuizGameScreen (Line 363)
    User->>Quiz: Trả lời 10 câu hỏi trắc nghiệm
    Note over Quiz: Chấm điểm bài làm & Tính toán % độ chính xác
    Quiz->>VM: updateAccuracyAndWordsLearned(score, total)
    VM->>Prefs: Tích lũy tiến trình học tập
    Quiz-->>User: Hiển thị màn hình kết quả (Victory / Completion)
```

---

## 3. Thuật toán Bảo vệ Streak (Streak & Streak Freeze)

**Streak** (Số ngày học liên tục) là động lực to lớn giúp giữ chân người dùng. Tuy nhiên, nếu người dùng có việc bận đột xuất và không thể học, chuỗi Streak sẽ bị reset về `0`.

**Giải pháp**: Tính năng **Streak Freeze** cho phép đóng băng Streak.
* Trạng thái trang bị Streak Freeze (`isStreakFreezeEquipped`) được lưu bền vững qua SharedPreferences tại [AppSettings.kt:37](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/core/util/AppSettings.kt#L37).
* Khi người dùng trang bị Streak Freeze trong [StatsScreen.kt:385](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/stats/presentation/StatsScreen.kt#L385), trạng thái này được lưu lại vĩnh viễn và hiển thị huy hiệu xanh lá cây `"Protected"` kèm icon băng tuyết `AcUnit` trên [HomeScreen.kt:116](file:///D:/Fullit/projects/Android/MinLish/app/src/main/java/com/edu/minlish/features/home/presentation/HomeScreen.kt#L116).
* Vào cuối ngày (hệ thống kiểm tra tiến độ lúc 00:00):
  * Nếu người dùng chưa hoàn thành mục tiêu ngày (Today's Plan) **NHƯNG** đã trang bị Streak Freeze:
    * Streak được bảo toàn.
    * Trạng thái `isStreakFreezeEquipped` được đặt lại về `false`.
  * Nếu chưa hoàn thành mục tiêu ngày và **KHÔNG** trang bị Streak Freeze:
    * Streak reset về `0`.

---

## 4. Nghiệp vụ Nhắc nhở Hàng ngày (Daily Reminder Flow)

Để nhắc nhở người dùng học tập đúng giờ, MinLish sử dụng **WorkManager** để kích hoạt tác vụ chạy ngầm định kỳ 24 giờ một lần.

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant Settings as SettingsScreen
    participant Picker as android.app.TimePickerDialog
    participant Repo as WorkManagerReminderRepository
    participant WM as Android WorkManager
    participant Worker as ReminderWorker

    User->>Settings: Click chọn "Reminder Time" (Line 89)
    Settings->>Picker: Hiển thị dialog chọn giờ/phút hệ thống
    User->>Picker: Chọn "08:30 PM" và bấm OK
    Picker-->>Settings: Trả về Hour=20, Minute=30
    Settings->>Repo: scheduleDaily("20:30") (Line 495)
    Note over Repo: Tính toán khoảng thời gian delay đến 20:30 (initialDelay)
    Repo->>WM: enqueueUniquePeriodicWork("MinLishDailyReminder", 1_DAYS, workRequest) (Line 43)
    Note over WM: Đăng ký tác vụ định kỳ chạy ngầm thành công
    
    Note over WM, Worker: Khi đến 20:30 hàng ngày
    WM->>Worker: execute()
    Worker->>Worker: Gửi thông báo đẩy (Push Notification) lên thanh trạng thái điện thoại
    Worker-->>User: Hiển thị thông báo: "Đã đến giờ ôn luyện tiếng Anh cùng MinLish rồi!"
```

---

## 5. Tài liệu tham khảo (References)

* **Android WorkManager Guide**: Jetpack Libraries.
* **Kotlin SharedPreferences Delegate Pattern**: Android best practices.
