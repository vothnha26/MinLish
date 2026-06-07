# MinLish — README & Kiến Trúc Dự Án

## Giới thiệu

**MinLish** là ứng dụng học từ vựng tiếng Anh trên Android, được xây dựng bằng **Jetpack Compose** và theo mô hình kiến trúc **MVVM + Clean Architecture**.

---

## Cấu trúc thư mục

```
com.edu.minlish/
│
├── MainActivity.kt                  ← Entry point của app
├── MinLishApp.kt                    ← NavHost, điều hướng toàn app
│
├── core/                            ← Dùng chung cho toàn app
│   ├── ai/                          ← Tích hợp Gemini AI
│   ├── designsystem/
│   │   ├── component/               ← Button, Card dùng chung
│   │   └── theme/                   ← Color, Typography, Theme
│   ├── navigation/                  ← Định nghĩa Screen routes
│   ├── network/                     ← Retrofit, ApiClient
│   ├── notification/
│   │   ├── ReminderRepository.kt    ← Interface lên lịch nhắc
│   │   ├── WorkManagerReminderRepository.kt ← Impl dùng WorkManager
│   │   └── ScheduleReminderUseCase.kt
│   └── util/
│       ├── AppSettings.kt           ← SharedPreferences cho cài đặt
│       ├── AudioPlayer.kt           ← TTS phát âm
│       ├── NotificationHelper.kt    ← Show system notification
│       ├── ReminderWorker.kt        ← WorkManager Worker
│       └── SpacedRepetitionUtil.kt  ← SM-2 local (legacy)
│
└── features/                        ← Mỗi tính năng là 1 module
    ├── auth/                        ← Đăng nhập / Đăng ký
    │   ├── data/
    │   │   ├── AuthApiService.kt    ← Retrofit API (nếu dùng REST)
    │   │   └── repository/
    │   │       └── FirebaseAuthRepositoryImpl.kt
    │   ├── domain/
    │   │   ├── model/User.kt
    │   │   └── repository/AuthRepository.kt   ← Interface
    │   └── presentation/
    │       ├── LoginScreen.kt
    │       ├── RegisterScreen.kt
    │       ├── ForgotPasswordScreen.kt
    │       └── viewmodel/AuthViewModel.kt
    │
    ├── library/                     ← Quản lý bộ từ vựng
    │   ├── data/
    │   │   ├── importer/            ← Import CSV/Excel
    │   │   └── repository/FirestoreVocabularyRepositoryImpl.kt
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── VocabularySet.kt
    │   │   │   ├── VocabularyWord.kt
    │   │   │   └── Category.kt
    │   │   └── repository/VocabularyRepository.kt
    │   └── presentation/
    │       ├── LibraryScreen.kt
    │       ├── AddWordScreen.kt
    │       ├── WordListScreen.kt
    │       ├── AICreateWordSetScreen.kt
    │       └── viewmodel/
    │           ├── LibraryViewModel.kt
    │           ├── AddWordViewModel.kt
    │           └── WordListViewModel.kt
    │
    ├── learning/                    ← Học, Quiz, Flashcard
    │   ├── data/
    │   │   └── repository/FirestoreLearningRepositoryImpl.kt
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── QuizQuestion.kt
    │   │   │   ├── UserWordProgress.kt
    │   │   │   └── UserReviewLog.kt
    │   │   ├── repository/LearningRepository.kt
    │   │   └── usecase/
    │   │       ├── UpdateWordProgressUseCase.kt  ← SM-2 algorithm
    │   │       └── BuildQuizQuestionsUseCase.kt  ← Tạo câu hỏi
    │   └── presentation/
    │       ├── FlashcardScreen.kt
    │       ├── QuizGameScreen.kt
    │       ├── GameHubScreen.kt
    │       └── viewmodel/
    │           ├── QuizViewModel.kt
    │           ├── QuizUiState.kt
    │           ├── QuizViewModelFactory.kt
    │           └── FlashcardViewModel.kt
    │
    ├── home/                        ← Màn hình chính
    ├── settings/                    ← Cài đặt thông báo, SM-2
    ├── stats/                       ← Thống kê học tập
    ├── speaking/                    ← Luyện phát âm
    ├── onboarding/                  ← Màn hình chào
    └── profilesetup/                ← Cài đặt hồ sơ
```

---

## Luồng dữ liệu tổng quát

Tương tự web: **Controller → Service → Repository → Database**, trong Android/MVVM:

```
UI (Screen/Composable)
    │  user action (click, input)
    ▼
ViewModel
    │  gọi UseCase hoặc Repository
    ▼
UseCase (business logic)
    │  gọi Repository
    ▼
Repository (interface)
    │  quyết định lấy từ đâu
    ▼
Data Source (Firebase Firestore / Retrofit API / SharedPreferences)
    │  trả kết quả về (Result<T>)
    ▼ (ngược lại)
Repository → UseCase → ViewModel (cập nhật uiState)
    │
    ▼
UI tự cập nhật nhờ State observation (Compose recomposition)
```

### Ví dụ cụ thể — Luồng đăng nhập

```
LoginScreen.kt
  └─ Button click → authViewModel.login(email, password)
        └─ AuthViewModel.kt (viewModelScope.launch)
              └─ authRepository.login(email, password)         ← gọi interface
                    └─ FirebaseAuthRepositoryImpl.kt
                          └─ Firebase.auth.signInWithEmailAndPassword()
                    └─ trả Result<User>
              └─ uiState = AuthUiState.Success(user)           ← cập nhật state
        └─ LoginScreen.kt quan sát uiState → navigate to Home
```

### Ví dụ — Luồng Quiz (học từ vựng)

```
QuizGameScreen.kt
  └─ LaunchedEffect → viewModel.loadQuiz(setId, modes)
        └─ QuizViewModel.kt
              └─ repository.getDueWords(userId, setId)          ← lấy từ cần ôn
                    └─ FirestoreLearningRepositoryImpl.kt
                          ├─ Firestore: vocabulary_sets WHERE creatorId == userId
                          ├─ Firestore: vocabulary_words WHERE vocabularySetId IN [userSetIds]
                          └─ Firestore: user_word_progress WHERE userId == userId
              └─ buildQuestionsUseCase(dueWords, modes)         ← tạo câu hỏi
              └─ uiState = QuizUiState.Success(questions)

  User trả lời → selectOption(index)
        └─ QuizViewModel.kt
              └─ updateProgressUseCase(wordId, correct, intervalUnitMs, threshold)
                    └─ UpdateWordProgressUseCase.kt (SM-2 algorithm)
                          └─ repository.updateProgress(newProgress) → Firestore
```

---

## Mapping Kiến thức → Project

---

### A. Compose Fundamentals

**Khái niệm:** Jetpack Compose là UI framework khai báo (declarative). UI = f(state). Thay vì cập nhật từng element như XML, ta chỉ cần mô tả UI trông như thế nào khi state có giá trị nào đó.

**Trong project:**

| Khái niệm | Vị trí trong code |
|---|---|
| `@Composable` functions | Toàn bộ file `*Screen.kt` trong các feature |
| `setContent` | `MainActivity.kt` → `setContent { MinLishTheme { MinLishApp() } }` |
| `Column`, `Row`, `Box` | Mọi screen (vd: `LibraryScreen.kt`, `QuizGameScreen.kt`) |
| `Modifier` | Dùng khắp nơi: `.padding()`, `.fillMaxSize()`, `.background()`, `.clickable()` |
| `Text`, `Button`, `TextField` | `LoginScreen.kt`, `AddWordScreen.kt`, `SettingsScreen.kt` |
| `@Preview` | Các screen có `@Preview` annotation để xem trước UI |
| Component tái sử dụng | `core/designsystem/component/MinLishButton.kt` — nút dùng chung toàn app |
| Theming | `core/designsystem/theme/` — định nghĩa Color, Typography, MinLishTheme |

**Luồng ví dụ:**
```
MinLishTheme (theme)
  └─ MinLishApp() (NavHost)
        └─ LibraryScreen() (Composable)
              └─ Column { LazyColumn { VocabularySetCard() } }
                    └─ MinLishButton(text="Add Set") { ... }
```

---

### B. State & Recomposition

**Khái niệm:** State là dữ liệu có thể thay đổi. Khi state thay đổi, Compose tự động vẽ lại (recompose) những phần UI bị ảnh hưởng. `remember` giữ state qua recomposition.

**Trong project:**

| Khái niệm | Vị trí |
|---|---|
| `mutableStateOf` | `AuthViewModel.kt`: `var uiState by mutableStateOf(AuthUiState.Idle)` |
| `remember` | `SettingsScreen.kt`: `var notificationsEnabled by remember { mutableStateOf(true) }` |
| `mutableStateListOf` | `QuizViewModel.kt`: `val matchedEnglishCards = mutableStateListOf<String>()` |
| State hoisting | `QuizGameScreen.kt` nhận `viewModel` từ ngoài, truyền state xuống sub-composable |
| Sealed class cho state | `LibraryUiState: Loading / Success / Error` trong `LibraryViewModel.kt` |
| Unidirectional Data Flow | UI gọi `viewModel.loadQuiz()` → ViewModel cập nhật `uiState` → UI observe |

**Luồng ví dụ:**
```
LibraryScreen:
  val uiState = viewModel.uiState          ← đọc state
  when (uiState) {
    is Loading  → CircularProgressIndicator()
    is Success  → LazyColumn(sets)
    is Error    → Text(error.message)
  }
  Button(onClick = { viewModel.loadUserSets() })  ← gửi event lên ViewModel
```

---

### C. Navigation

**Khái niệm:** Single Activity, nhiều màn hình. `NavHost` quản lý back stack. Mỗi màn hình có một `route` (chuỗi định danh). Điều hướng bằng `navController.navigate(route)`.

**Trong project:**

| Khái niệm | Vị trí |
|---|---|
| `NavHost` | `MinLishApp.kt` — định nghĩa toàn bộ navigation graph |
| Routes | `core/navigation/Screen.kt` — sealed class chứa tất cả routes |
| Truyền data qua navigation | `Screen.QuizGame.createRoute(setId, modes, count)` — dùng path args |
| Back stack | `navController.popBackStack()` trong mỗi `onBack` callback |
| Deep link / argument | `navArgument(ARG_SET_ID) { type = NavType.StringType; nullable = true }` |

**Luồng ví dụ:**
```
FlashcardScreen → onPlayQuiz(setId)
  → navController.navigate(Screen.GameHub.createRoute(setId))
      → GameHubScreen → chọn mode → onStartGame(modes, count)
          → navController.navigate(Screen.QuizGame.createRoute(setId, modes, count))
              → QuizGameScreen(setId, modes, count)
```

---

### D. Architecture (MVVM)

**Khái niệm:** MVVM = Model–View–ViewModel. View chỉ hiển thị, không chứa logic. ViewModel xử lý logic, lưu state. Model là data (domain model + data source).

**Trong project:**

| Layer | Vai trò | Ví dụ trong code |
|---|---|---|
| **View** | Composable screens, hiển thị state | `LibraryScreen.kt`, `QuizGameScreen.kt` |
| **ViewModel** | Xử lý logic, expose state | `LibraryViewModel.kt`, `QuizViewModel.kt` |
| **Model** | Domain entities + data sources | `VocabularySet.kt`, `FirestoreVocabularyRepositoryImpl.kt` |

**Luồng MVVM cụ thể:**
```
View:        LibraryScreen.kt
               ↓ viewModel.loadUserSets()
ViewModel:   LibraryViewModel.kt
               ↓ repository.getUserSets(userId)
Repository:  FirestoreVocabularyRepositoryImpl.kt
               ↓ firestore.collection("vocabulary_sets").whereEqualTo("creatorId", userId)
Database:    Firebase Firestore
               ↑ Result<List<VocabularySet>>
ViewModel:     uiState = LibraryUiState.Success(sets)
View:          recompose → hiển thị danh sách sets
```

---

### E. ViewModel & State Management

**Khái niệm:** ViewModel tồn tại qua configuration change (xoay màn hình). Dùng sealed class để mô tả các trạng thái UI. State phải là single source of truth.

**Trong project:**

| Khái niệm | Vị trí |
|---|---|
| `ViewModel` | `AuthViewModel`, `LibraryViewModel`, `QuizViewModel` |
| `AndroidViewModel` | `QuizViewModel.kt` — cần `applicationContext` cho AppSettings |
| Sealed class state | `AuthUiState`, `LibraryUiState`, `QuizUiState`, `ImportUiState` |
| Loading/Success/Error | Tất cả ViewModel đều có 3 trạng thái này |
| `viewModelScope.launch` | Mọi coroutine trong ViewModel dùng scope này (tự hủy khi VM destroyed) |
| `private set` | Chỉ ViewModel được ghi state, UI chỉ đọc |

**Ví dụ AuthUiState:**
```kotlin
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User, val isSetupComplete: Boolean) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
```

---

### F1. Networking

**Khái niệm:** App giao tiếp với server qua HTTP. Dùng Retrofit để định nghĩa endpoints, parse JSON. Không được gọi network trên main thread.

**Trong project:**

| Khái niệm | Vị trí |
|---|---|
| Retrofit | `core/network/` — ApiClient, Retrofit setup |
| API Service | `features/auth/data/AuthApiService.kt` — định nghĩa REST endpoints |
| Firebase Firestore | Thay thế REST API cho hầu hết tính năng (realtime database) |
| Dictionary API | `FreeDictionaryStrategy.kt` — gọi `api.dictionaryapi.dev` để tra nghĩa từ |
| Error handling | `.onSuccess { } .onFailure { }` pattern trong mọi repository |

**Luồng tra từ điển:**
```
AddWordScreen → nhập từ "ephemeral" → viewModel.fetchWordDetails("ephemeral")
  → AddWordViewModel.kt
      → repository.fetchWordDetails("ephemeral")
          → FreeDictionaryStrategy.kt
              → Retrofit GET https://api.dictionaryapi.dev/api/v2/entries/en/ephemeral
              → parse JSON → List<DictionaryEntry>
  → uiState cập nhật → UI hiển thị nghĩa, ví dụ
```

---

### F2. Coroutines & Async Programming

**Khái niệm:** Coroutines cho phép viết code bất đồng bộ theo kiểu tuần tự, không block main thread. `suspend fun` có thể dừng và tiếp tục mà không tốn thread.

**Trong project:**

| Khái niệm | Vị trí |
|---|---|
| `viewModelScope.launch` | Tất cả ViewModel: `AuthViewModel`, `LibraryViewModel`, `QuizViewModel` |
| `suspend fun` | Toàn bộ function trong Repository Impl (`getDueWords`, `updateProgress`...) |
| `Dispatchers.IO` | Firestore và Retrofit tự xử lý (dùng `.await()`) |
| `withTimeout` | `FirestoreLearningRepositoryImpl.kt` — timeout 15s cho Firestore query |
| `kotlinx.coroutines.tasks.await` | Chuyển Firebase Task thành suspend (`firestore.get().await()`) |
| Coroutine scope lifecycle | `viewModelScope` tự cancel khi ViewModel bị destroy → không leak |

**Luồng coroutine:**
```kotlin
// QuizViewModel.kt
viewModelScope.launch {                    // chạy trên Main
    uiState = QuizUiState.Loading
    repository.getDueWords(userId, setId)  // suspend, chạy trên IO thread
        .onSuccess { dueWords ->
            uiState = QuizUiState.Success(...)  // về Main, cập nhật UI
        }
}
```

---

### G. Local Storage

**Khái niệm:** Lưu data local để dùng offline hoặc cache. Các loại: SharedPreferences (key-value), Room (database), File.

**Trong project:**

| Loại storage | Dùng cho | Vị trí |
|---|---|---|
| **SharedPreferences** | Cài đặt app (intervalUnit, masteredThreshold, reminderTime) | `AppSettings.kt` |
| **Firebase Firestore** | Dữ liệu chính: từ vựng, progress, user | Remote (nhưng có offline cache) |
| **SharedPreferences** | SM-2 data (legacy, đã chuyển lên Firestore) | `SpacedRepetitionUtil.kt` |

**Ví dụ AppSettings:**
```kotlin
object AppSettings {
    var intervalUnit: String               // "DAYS" / "HOURS" / "MINUTES"
        get() = prefs?.getString("interval_unit", "DAYS") ?: "DAYS"
        set(value) { prefs?.edit()?.putString("interval_unit", value)?.apply() }
    
    var masteredThreshold: Int             // số interval để coi là "mastered"
        get() = prefs?.getInt("mastered_threshold", 30) ?: 30
}
```

---

### H. Repository Pattern

**Khái niệm:** Repository là lớp trung gian giữa ViewModel và data source. ViewModel chỉ biết đến interface, không biết data đến từ Firestore hay API hay local.

**Trong project:**

| Thành phần | File |
|---|---|
| Interface | `AuthRepository.kt`, `VocabularyRepository.kt`, `LearningRepository.kt`, `ReminderRepository.kt` |
| Implementation | `FirebaseAuthRepositoryImpl.kt`, `FirestoreVocabularyRepositoryImpl.kt`, `FirestoreLearningRepositoryImpl.kt`, `WorkManagerReminderRepository.kt` |
| Quy tắc | ViewModel chỉ giữ reference đến interface, không import Impl trực tiếp |

**Luồng Repository:**
```
LibraryViewModel
  private val repository: VocabularyRepository     ← chỉ biết interface
    = FirestoreVocabularyRepositoryImpl()           ← impl được inject

repository.getUserSets(userId)
  → FirestoreVocabularyRepositoryImpl.getUserSets()
      → firestore.collection("vocabulary_sets")
               .whereEqualTo("creatorId", userId).get().await()
      → map documents → List<VocabularySet>
      → return Result.success(sets)
```

---

### I. Dependency Injection

**Khái niệm:** Thay vì class tự tạo dependency, dependency được truyền từ bên ngoài vào (constructor injection). Giúp dễ test và thay thế impl.

**Trong project:**

| Hình thức DI | Vị trí |
|---|---|
| **Constructor injection (thủ công)** | `QuizViewModelFactory.kt` — tạo ViewModel với đầy đủ dependency |
| **Default param** (đơn giản hơn) | `LibraryViewModel(repository = FirestoreVocabularyRepositoryImpl())` — vẫn có thể override khi test |
| **ViewModelFactory** | `QuizViewModelFactory` inject `UpdateWordProgressUseCase`, `BuildQuizQuestionsUseCase` |

**Ví dụ QuizViewModelFactory:**
```kotlin
class QuizViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = FirestoreLearningRepositoryImpl()
        val updateProgressUseCase = UpdateWordProgressUseCase(repository)
        val buildQuestionsUseCase = BuildQuizQuestionsUseCase()
        return QuizViewModel(application, repository, authRepo,
                             updateProgressUseCase, buildQuestionsUseCase) as T
    }
}
```

> **Lưu ý:** Project chưa dùng Hilt/Koin. DI được thực hiện thủ công qua constructor và factory.

---

### K. Testing & Debugging

**Khái niệm:** Unit test kiểm tra logic độc lập (ViewModel, UseCase). Debugging dùng Logcat, breakpoint. Mocking thay thế real dependency bằng fake.

**Trong project:**

| Khái niệm | Hiện trạng trong project |
|---|---|
| Sealed class state | Dễ test: `assertEquals(QuizUiState.Error("..."), viewModel.uiState)` |
| UseCase tách biệt | `BuildQuizQuestionsUseCase` là pure function → dễ unit test (không cần mock) |
| `Result<T>` pattern | Repository trả `Result.success()` / `Result.failure()` → dễ test error path |
| Debug log | `println("DEBUG: ...")` còn sót trong `FirestoreVocabularyRepositoryImpl.kt` |
| Testability | ViewModel nhận interface (không hardcode impl) → có thể mock trong test |

**Ví dụ test giả định:**
```kotlin
// Test BuildQuizQuestionsUseCase
val useCase = BuildQuizQuestionsUseCase()
val words = listOf(fakeWord1, fakeWord2, ...)
val questions = useCase(words.map { it to null }, modes = "MULTIPLE_CHOICE", questionCount = 5)
assertEquals(5, questions.size)
assertTrue(questions.all { it.type == QuestionType.MULTIPLE_CHOICE })
```

---

### L. Performance & Optimization

**Khái niệm:** Tránh recomposition thừa. Dùng `LazyColumn` thay `Column` cho list dài. Tránh tính toán nặng trên main thread.

**Trong project:**

| Khái niệm | Vị trí |
|---|---|
| `LazyColumn` | `LibraryScreen.kt` — hiển thị danh sách bộ từ |
| `LazyVerticalGrid` | `QuizGameScreen.kt` — matching game dạng grid |
| Background thread | Mọi Firestore/network call dùng coroutine (không block UI) |
| `withTimeout` | `FirestoreLearningRepositoryImpl.kt` — timeout 15-20s tránh treo app |
| `remember` cho state local | `SettingsScreen.kt` tránh re-init mỗi recompose |
| `private set` trên state | Giảm recompose không cần thiết (UI không thể trigger write) |
| Chunked Firestore query | `fetchUserWords()` dùng `.chunked(10)` vì `whereIn` giới hạn 10 items |

---

## Tóm tắt kiến trúc theo sơ đồ

```
┌─────────────────────────────────────────────────┐
│                  PRESENTATION                    │
│  Screen (Composable) ←→ ViewModel ←→ UiState    │
│  Mô tả: Chỉ hiển thị, không có business logic   │
└────────────────────┬────────────────────────────┘
                     │ gọi UseCase / Repository
┌────────────────────▼────────────────────────────┐
│                    DOMAIN                        │
│  UseCase: UpdateWordProgressUseCase (SM-2)       │
│           BuildQuizQuestionsUseCase              │
│           ScheduleReminderUseCase                │
│  Model: VocabularyWord, QuizQuestion, UserWordProgress │
│  Repository Interface: VocabularyRepository,...  │
└────────────────────┬────────────────────────────┘
                     │ implement
┌────────────────────▼────────────────────────────┐
│                     DATA                         │
│  FirestoreVocabularyRepositoryImpl               │
│  FirestoreLearningRepositoryImpl                 │
│  FirebaseAuthRepositoryImpl                      │
│  WorkManagerReminderRepository                   │
│  Data Sources: Firebase Firestore, Retrofit API, │
│                SharedPreferences, WorkManager    │
└─────────────────────────────────────────────────┘
```

---

## Tech Stack

| Công nghệ | Dùng cho |
|---|---|
| Kotlin | Ngôn ngữ chính |
| Jetpack Compose | UI declarative |
| Firebase Auth | Đăng nhập (Email/Google) |
| Firebase Firestore | Database chính (NoSQL, realtime) |
| Retrofit + OkHttp | Gọi REST API (Dictionary API) |
| WorkManager | Lên lịch notification nền |
| Coroutines | Async/concurrency |
| ViewModel + State | MVVM state management |
| SharedPreferences | Lưu cài đặt local |
| Coil | Load ảnh |
