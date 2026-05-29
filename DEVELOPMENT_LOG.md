# MinLish Development Log

Tài liệu này ghi lại chi tiết các bước phát triển, cấu hình hệ thống và các quyết định kỹ thuật quan trọng của dự án MinLish.

## 1. Công nghệ hiện tại (Tech Stack)
*   **Platform:** Android (Kotlin, Jetpack Compose)
*   **Database:** **Firebase Firestore (NoSQL)**
*   **Authentication:** Firebase Auth (Email/Password & Google Login)
*   **Dependency Management:** Version Catalog (`libs.versions.toml`)
*   **Architecture:** Clean Architecture + SOLID Principles.

---

## 2. Hướng dẫn thiết lập & Kết nối Firebase (Kỹ thuật)

Đây là các bước cốt lõi đã thực hiện để App Android MinLish có thể "nói chuyện" được với Firebase Firestore:

### Bước 1: Cấu hình Project Identity
1.  Tải file `google-services.json` từ Firebase Console (Project ID: `myapplication-cf490995`).
2.  Đặt file vào thư mục `app/` của dự án. File này chứa API Key và Project ID để App định danh với Google Cloud.

### Bước 2: Cấu hình Build System (Gradle)
Để nạp được Firebase, chúng ta đã sửa 2 file quan trọng:
1.  **`gradle/libs.versions.toml`**: Định nghĩa phiên bản tập trung.
    *   Thêm `firebase-bom`: Quản lý phiên bản tự động cho các thư viện Firebase.
    *   Thêm `firebase-auth-ktx` và `firebase-firestore-ktx`.
2.  **`app/build.gradle.kts`**:
    *   Áp dụng plugin: `alias(libs.plugins.google.services)`.
    *   Khai báo thư viện: `implementation(libs.firebase.firestore)`.

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
*   **Interface:** Định nghĩa các hàm cần thiết (ví dụ: `saveProfile`).
*   **Implementation:** `FirestoreProfileRepositoryImpl` sử dụng `FirebaseFirestore.getInstance()` để thực thi lệnh.
*   **Timeout & Error Handling:** Mọi lệnh gọi DB đều bọc trong `withTimeout(10000)` và `try-catch` để đảm bảo App không bị treo khi mạng yếu.

---

## 3. Các công việc đã hoàn thành

### 3.1. Hệ thống Xác thực (Auth)
*   Hoàn thiện luồng Đăng ký/Đăng nhập.
*   **Tự động Sync:** Khi User đăng nhập lần đầu, hệ thống tự tạo một bản ghi cơ bản trong collection `users` dựa trên **UID** của Firebase.

### 3.2. Module Hồ sơ người dùng (Profile)
*   **Trang Setup:** Cho phép chọn Goal (IELTS, TOEIC...) và Level (A1-C2).
*   **Trang Cá nhân (Personal Profile):** Hiển thị thẻ thông tin đẹp mắt, hỗ trợ Đăng xuất và Chỉnh sửa thông tin.
*   **Điều hướng thông minh:** App tự động nhận diện User đã setup profile chưa để dẫn đường (Setup hoặc Home).

---

## 4. Nhật ký Sửa lỗi & Tối ưu hóa (Dành cho Dev)

| Lỗi gặp phải | Cách xử lý |
| :--- | :--- |
| **OutOfMemory (Gradle)** | Tăng `Xmx` lên 4096m trong `gradle.properties`. |
| **Timed Out (Firestore)** | Thêm `withTimeout` và kiểm tra `request.auth != null` trong Rules. |
| **Serialization Error** | Thêm giá trị mặc định cho toàn bộ trường trong Data Class (Kotlin). |
| **SQL Connect Fail** | Quyết định gỡ bỏ Data Connect (do tốn phí/phức tạp) và quay lại Firestore (Free). |

---

## 5. Trạng thái Dự án
*   **Build:** ✅ Thành công (39 tasks executed).
*   **Dữ liệu:** ✅ Đã kết nối NoSQL thật (Firestore).
*   **Cleanup:** ✅ Đã xóa toàn bộ file dư thừa của Data Connect/SQL.

---
*Cập nhật lần cuối: 29/05/2026 bởi Gemini CLI*
