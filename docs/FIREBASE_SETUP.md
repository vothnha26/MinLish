# Hướng dẫn tích hợp Firebase trong MinLish

Dự án MinLish kết nối với Firebase thông qua sự kết hợp giữa cấu hình hệ thống (Gradle) và kiến trúc code (SOLID). Dưới đây là các thành phần chính cùng mã nguồn minh họa:

## 1. Cấu hình Hệ thống (Infrastructure)

### Version Catalog (`gradle/libs.versions.toml`)
Chúng ta sử dụng **Firebase BOM (Bill of Materials)** để quản lý version của các thư viện Firebase một cách đồng bộ.

```toml
[versions]
firebaseBom = "33.10.0"
googleSignIn = "21.3.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
google-signin = { group = "com.google.android.gms", name = "play-services-auth", version.ref = "googleSignIn" }
```

### Gradle Kotlin DSL (`app/build.gradle.kts`)
- Tự động cấu hình `GOOGLE_CLIENT_ID` thông qua `buildConfigField`. Mã này được lấy từ `oauth_client` (client_type: 3) trong file `google-services.json`.

```kotlin
plugins {
    alias(libs.plugins.google.services)
}

android {
    buildFeatures {
        buildConfig = true
    }
    buildTypes.all {
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"18906284489-fvnr49eli24luu9cfvhmbkkb2ntaokhb.apps.googleusercontent.com\"")
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.google.signin)
}
```

## 2. Kiến trúc Code (Software Architecture)

Dự án áp dụng nguyên tắc **Dependency Inversion** (chữ D trong SOLID) để việc kết nối Firebase trở nên linh hoạt.

### Domain Layer (Interface)
Lớp này định nghĩa "Cần làm gì" thay vì "Làm như thế nào". UI và ViewModel chỉ tương tác với Interface này.

```kotlin
// app/src/main/java/com/edu/minlish/features/auth/domain/repository/AuthRepository.kt
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, fullName: String): Result<User>
    suspend fun loginWithGoogle(idToken: String): Result<User>
    suspend fun forgotPassword(email: String): Result<Unit>
}
```

### Data Layer (Implementation)
Lớp này trực tiếp giao tiếp với Firebase SDK. Sử dụng `kotlinx-coroutines-play-services` (`.await()`) để chuyển `Task` của Firebase thành Coroutines.

```kotlin
// app/src/main/java/com/edu/minlish/features/auth/data/repository/FirebaseAuthRepositoryImpl.kt
class FirebaseAuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Gọi Firebase SDK và dùng .await() để chờ kết quả
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("User is null"))
            
            // Map dữ liệu Firebase sang Domain Model nội bộ
            Result.success(
                User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    fullName = firebaseUser.displayName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Tương tự cho register, loginWithGoogle...
}
```

### Presentation Layer (ViewModel)
ViewModel gọi Repository mà không cần biết dữ liệu đang được xử lý bằng Firebase hay Backend tự xây.

```kotlin
// app/src/main/java/com/edu/minlish/features/auth/presentation/viewmodel/AuthViewModel.kt
class AuthViewModel(
    private val repository: AuthRepository = FirebaseAuthRepositoryImpl()
) : ViewModel() {

    fun login(email: String, password: String, onSuccess: (User) -> Unit) {
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            repository.login(email, password).onSuccess { user ->
                uiState = AuthUiState.Success(user)
                onSuccess(user)
            }.onFailure { e ->
                uiState = AuthUiState.Error(e.message ?: "Login failed")
            }
        }
    }
}
```

## 3. Tổng kết quy trình hoạt động
1. **Khởi tạo**: Plugin Google Services đọc `google-services.json` và khởi tạo kết nối Firebase.
2. **UI Gọi ViewModel**: Người dùng nhấn nút, UI gọi `viewModel.login()`.
3. **ViewModel Gọi Repository**: Gọi hàm trong interface `AuthRepository`.
4. **Implementation Xử lý**: `FirebaseAuthRepositoryImpl` dùng `FirebaseAuth.getInstance().signIn...().await()` để gọi Firebase server.
5. **Trả kết quả**: Trả về `Result<User>`, ViewModel cập nhật State, UI tự động thay đổi.
