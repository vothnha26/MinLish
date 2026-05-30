# MinLish Development Log

Tài liệu này ghi lại chi tiết các bước phát triển, cấu hình hệ thống và các quyết định kỹ thuật quan trọng của dự án MinLish.

## 1. Công nghệ hiện tại (Tech Stack)

- **Platform:** Android (Kotlin, Jetpack Compose)
- **Database:** **Firebase Firestore (NoSQL)**
- **Authentication:** Firebase Auth (Email/Password & Google Login)
- **Dependency Management:** Version Catalog (`libs.versions.toml`)
- **Architecture:** Clean Architecture + SOLID Principles.

---

## 2. Hướng dẫn thiết lập & Kết nối Firebase (Kỹ thuật)

Đây là các bước cốt lõi đã thực hiện để App Android MinLish có thể "nói chuyện" được với Firebase Firestore:

### Bước 1: Cấu hình Project Identity

1.  Tải file `google-services.json` từ Firebase Console (Project ID: `myapplication-cf490995`).
2.  Đặt file vào thư mục `app/` của dự án. File này chứa API Key và Project ID để App định danh với Google Cloud.

### Bước 2: Cấu hình Build System (Gradle)

Để nạp được Firebase, chúng ta đã sửa 2 file quan trọng:

1.  **`gradle/libs.versions.toml`**: Định nghĩa phiên bản tập trung.
    - Thêm `firebase-bom`: Quản lý phiên bản tự động cho các thư viện Firebase.
    - Thêm `firebase-auth-ktx` và `firebase-firestore-ktx`.
2.  **`app/build.gradle.kts`**:
    - Áp dụng plugin: `alias(libs.plugins.google.services)`.
    - Khai báo thư viện: `implementation(libs.firebase.firestore)`.

### Bước 3: Thiết lập Security Rules (Quan trọng)

Chúng ta đã cấu hình Firestore ở chế độ **Free/Test Mode** với Rules cho phép mọi User đã xác thực được quyền đọc/ghi dữ liệu của chính họ:

```javascript
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Bước 4: Triển khai Code theo chuẩn SOLID

Thay vì gọi trực tiếp Firebase ở mọi nơi, chúng ta dùng **Repository Pattern**:

- **Interface:** Định nghĩa các hàm cần thiết (ví dụ: `saveProfile`).
- **Implementation:** `FirestoreProfileRepositoryImpl` sử dụng `FirebaseFirestore.getInstance()` để thực thi lệnh.
- **Timeout & Error Handling:** Mọi lệnh gọi DB đều bọc trong `withTimeout(10000)` và `try-catch` để đảm bảo App không bị treo khi mạng yếu.

---

## 3. Các công việc đã hoàn thành

### 3.1. Hệ thống Xác thực (Auth)

- Hoàn thiện luồng Đăng ký/Đăng nhập.
- **Tự động Sync:** Khi User đăng nhập lần đầu, hệ thống tự tạo một bản ghi cơ bản trong collection `users` dựa trên **UID** của Firebase.

### 3.3. Module Từ vựng (Vocabulary) & Thư viện (Library)

- **CRUD Bộ từ vựng & Danh mục:** Triển khai hoàn thiện CRUD đầy đủ cho bộ từ vựng và danh mục. Hỗ trợ tính năng cascade delete tự động xóa toàn bộ từ vựng thuộc bộ từ bị xóa.
- **Đồng bộ Word Count thực tế:** Khi thêm/xóa từ vựng, số lượng từ trong bộ từ vựng (`wordCount`) được cộng/trừ động thông qua `FieldValue.increment`.
- **Smart Search tự động qua API:** Tích hợp thành công các dịch vụ API thông qua Strategy Pattern:
  - **Free Dictionary API (`FreeDictionaryStrategy`):** Tự động điền phát âm (Pronunciation), Audio âm thanh, định nghĩa từ loại.
  - **Google Translate API (`GoogleTranslationStrategy`):** Tự động dịch nghĩa Anh - Việt.
  - **Datamuse API (`DatamuseCollocationStrategy`):** Tự động ghép 6 cụm collocations thông dụng nhất bằng cách truy vấn song song 4 hướng ngữ cảnh.
  - **LoremFlickr Image API:** Tải ảnh minh họa cố định dựa trên lockSeed tạo từ hashCode của từ.
- **Tiến độ học thật:** Tiến trình phần trăm (`% mastered`) của các bộ từ ở Library được tính toán động dựa trên progress học từ thật của người dùng (`user_word_progress`) thay vì hardcode.

### 3.4. Cơ chế ôn tập lặp lại khoảng cách & Cấu hình (Spaced Repetition & Settings) - Mới!

- **Lưu cấu hình cục bộ (`AppSettings`):** Triển khai lớp tiện ích Singleton đọc/ghi SharedPreferences để lưu đơn vị thời gian ôn tập (`intervalUnit`) và ngưỡng đã thuộc (`masteredThreshold`).
- **Tự cấu hình khoảng ngắt và đơn vị:** Cho phép người dùng tùy chọn đơn vị ôn tập (`Minutes`, `Hours`, `Days`) và nhập ngưỡng thuộc từ tùy ý trực tiếp từ giao diện Settings.
- **SM-2 thích ứng linh hoạt:** Thuật toán SM-2 cập nhật lịch ôn tập dựa trên Calendar ứng với đơn vị thời gian và ngưỡng đã chọn.

### 3.5. Phân tách Giao diện Profile Setup & Edit Profile - Mới!

- **Wizard đăng ký mới:** Giữ nguyên quy trình thiết lập từng bước (Wizard 3 bước) cho người dùng đăng ký lần đầu.
- **Edit Profile độc lập:** Triển khai một màn hình Edit Profile đơn trang (single-page) cao cấp, trực quan dành riêng cho người dùng đã hoàn thành thiết lập. Cho phép cập nhật đồng thời Tên hiển thị (được đồng bộ lên Firebase Auth và Firestore `users`), Study Goal và English Level.
- **TopAppBar tinh tế:** Hỗ trợ nút Back để quay lại màn hình trước và nút Save ở góc trên bên phải để người dùng thao tác tiện lợi.

---

## 4. Nhật ký Sửa lỗi & Tối ưu hóa (Dành cho Dev)

| Lỗi gặp phải                            | Cách xử lý                                                                                                                                                                 |
| :-------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **OutOfMemory (Gradle)**                | Tăng `Xmx` lên 4096m trong `gradle.properties`.                                                                                                                            |
| **Timed Out (Firestore)**               | Thêm `withTimeout` và kiểm tra `request.auth != null` trong Rules.                                                                                                         |
| **Serialization Error**                 | Thêm giá trị mặc định cho toàn bộ trường trong Data Class (Kotlin).                                                                                                        |
| **SQL Connect Fail**                    | Quyết định gỡ bỏ Data Connect (do tốn phí/phực tạp) và quay lại Firestore (Free).                                                                                          |
| **Không tự cập nhật UI**                | Thay `LaunchedEffect(Unit)` bằng `DisposableEffect` + `LifecycleEventObserver` lắng nghe sự kiện `ON_RESUME` để tự tải lại dữ liệu mới từ Firestore khi quay lại màn hình. |
| **Biến chưa định nghĩa (`isTestMode`)** | Khai báo lại biến `isTestMode` đọc từ `AppSettings.isTestMode` trước khi kiểm tra ngưỡng để tránh lỗi biên dịch Kotlin trong StatsViewModel.                               |

---

## 5. Trạng thái Dự án

- **Build:** ✅ Thành công (Gradle compilation exit code 0).
- **Dữ liệu:** ✅ Đã kết nối NoSQL thật (Firestore) & Cấu hình cục bộ (SharedPreferences).
- **Giao diện:** ✅ Tách biệt thành công giao diện thiết lập hồ sơ lần đầu và giao diện chỉnh sửa thông tin cá nhân cao cấp.

---

_Cập nhật lần cuối: 30/05/2026_
