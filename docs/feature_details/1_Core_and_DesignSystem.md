# MinLish - Giải thích chi tiết Toàn bộ Module CORE

Tài liệu này giải thích chi tiết **từng dòng code** của TẤT CẢ 23 files nằm trong package `com.edu.minlish.core`. Đồng thời ghi rõ **các file này được gọi ở đâu** trong toàn bộ hệ thống dự án.

---

## Phần 1: Package `core.ai` (Trí tuệ nhân tạo)

### 1. `core/ai/AIModule.kt`
- **Sử dụng ở đâu**: Được gọi khi cần tiêm (inject) `GeminiAIService` vào các ViewModel (như `SpeakingViewModel`, `TranslateAndLookupViewModel`, `AICreateWordSetViewModel`).
```kotlin
package com.edu.minlish.core.ai

import com.edu.minlish.BuildConfig

object AIModule { // Object khởi tạo duy nhất cho toàn app
    val geminiService: GeminiAIService by lazy { // lazy: Chỉ khởi tạo khi được gọi lần đầu tiên
        val model = BuildConfig.GEMINI_MODEL // Lấy tên mô hình AI từ cấu hình Gradle (local.properties)
        if (model.isBlank()) { // Kiểm tra nếu rỗng thì báo lỗi văng app
            throw IllegalStateException("Gemini Model is not configured in local.properties. Please add 'gemini.model=...'")
        }
        GeminiAIService(modelName = model) // Trả về instance thực sự
    }
}
```

### 2. `core/ai/GeminiAIService.kt`
- **Sử dụng ở đâu**: Nòng cốt tương tác với Firebase Vertex AI SDK. Được dùng ở `TranslateAndLookupViewModel` (Dịch văn bản, bóc tách từ vựng), `SpeakingViewModel` (Chấm điểm phát âm đa phương tiện), và `AICreateWordSetViewModel` (Tự động sinh bộ từ theo chủ đề).
```kotlin
package com.edu.minlish.core.ai

import ...

class GeminiAIService(private val modelName: String) { // Nhận tên model vào từ AIModule
    // Khởi tạo model xử lý Text (nhiệt độ thấp để ít random, ép trả về JSON)
    private val textModel by lazy {
        Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                temperature = 0.1f // Nhiệt độ thấp (0.1) giúp AI trả lời khuôn mẫu, ít sáng tạo linh tinh
                responseMimeType = "application/json" // Ép cấu trúc đầu ra là JSON để code parse được
            }
        )
    }

    // Khởi tạo model xử lý Multimodal (Text + Audio)
    private val multimodalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                temperature = 0.0f // Nhiệt độ 0.0 giúp transcribe Audio chính xác nhất, không tự biên soạn
                responseMimeType = "application/json"
            }
        )
    }

    // Hàm gọi AI tự sinh nội dung chi tiết cho 1 từ vựng (Auto-fill)
    suspend fun generateAutoFillContent(word: String): Result<String> = withContext(Dispatchers.IO) { // Chạy ngầm IO
        try {
            val prompt = """ ... """ // Câu lệnh yêu cầu AI (Prompt)
            val response = textModel.generateContent(prompt) // Gửi đi
            val text = response.text ?: throw Exception("Empty response from AI") // Lấy kết quả văn bản
            Result.success(text.trim()) // Trả về thành công
        } catch (e: Exception) { Result.failure(mapGenerativeException(e)) } // Bắt lỗi và đổi thông báo
    }

    // ... (Các hàm generateVocabularySet, generateFirstQuestion, generateNextTurn, evaluateSession, translateText, translateAndExtractVocabulary đều hoạt động chung cơ chế Prompt -> textModel.generateContent -> Trả Result)
    
    // Hàm gom nhóm lỗi khi gọi AI (Ví dụ hết hạn Quota hoặc không có mạng)
    private fun mapGenerativeException(e: Exception): Exception {
        val msg = e.message ?: ""
        val errorString = e.toString()
        if (msg.contains("MissingFieldException") || msg.contains("GRpcError") || errorString.contains("GRpcError")) {
            return Exception("Lỗi kết nối Vertex AI: Vui lòng kiểm tra lại cấu hình 'gemini.model'...", e)
        }
        return e
    }
}
```

### 3. `core/ai/model/AIAutoFillResult.kt`
- **Sử dụng ở đâu**: Data class dùng bởi `Gson` để ép kiểu chuỗi JSON mà AI trả về trong tính năng AutoFill (Màn hình `AddWordScreen` & `EditWordScreen`).
```kotlin
package com.edu.minlish.core.ai.model

import com.edu.minlish.features.library.domain.model.WordDefinition

data class AIAutoFillResult( // Khai báo khung chứa dữ liệu 1 từ do AI sinh ra
    val word: String = "", // Từ vựng
    val pronunciation: String = "", // Phiên âm
    val definitions: List<WordDefinition> = emptyList(), // Các định nghĩa, từ loại, ví dụ
    val collocations: String = "", // Cụm từ đi kèm
    val personalNote: String = "" // Ghi chú cá nhân
)
```

### 4. `core/ai/model/AIGeneratedSet.kt`
- **Sử dụng ở đâu**: Ép kiểu JSON cho tính năng tự động tạo cả 1 bộ từ vựng (`AICreateWordSetViewModel`).
```kotlin
package com.edu.minlish.core.ai.model

data class AIGeneratedSet( // Khung chứa cho toàn bộ bộ từ vựng AI sinh ra
    val title: String = "", // Tên bộ
    val description: String = "", // Mô tả bộ
    val words: List<AIAutoFillResult> = emptyList() // Danh sách các từ con (dùng lại Model AutoFillResult)
)
```

---

## Phần 2: Package `core.designsystem` (Giao diện chuẩn của App)

### 5. `core/designsystem/theme/Color.kt`
- **Sử dụng ở đâu**: Nguồn màu sắc chuẩn cho tất cả UI (Button, Background, Text).
```kotlin
package com.edu.minlish.core.designsystem.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF111111) // Màu chủ đạo đen nhám (cho Button, Focus)
val OnPrimary = Color(0xFFFFFFFF) // Chữ màu trắng trên nền Primary

val Background = Color(0xFFFFFFFF) // Nền app trắng
val OnBackground = Color(0xFF111111) // Chữ trên nền trắng là đen

val Surface = Color(0xFFFFFFFF) // Nền cho các ô/khối (Card, TextField)
val OnSurface = Color(0xFF111111) // Chữ trên Surface

val Muted = Color(0xFF6B6B6B) // Màu xám mờ cho chữ phụ
val Border = Color(0xFFE5E5E5) // Viền các ô input
val Placeholder = Color(0xFFCCCCCC) // Chữ gợi ý trong TextField

val Error = Color(0xFFD4183D) // Màu đỏ báo lỗi
val Success = Color(0xFF34A853) // Màu xanh báo thành công
// ... (màu độ mạnh mật khẩu)
```

### 6. `core/designsystem/theme/Type.kt`
- **Sử dụng ở đâu**: Định dạng font chữ dùng chung cho toàn dự án.
```kotlin
package com.edu.minlish.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
// ...

val Typography = Typography( // Ghi đè bộ phông chữ của Material3
    displayLarge = TextStyle( // Dùng cho Tiêu đề lớn
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp
    ),
    titleMedium = TextStyle( // Dùng cho Tiêu đề nhỏ (app bar)
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyLarge = TextStyle( // Dùng cho văn bản nội dung
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodySmall = TextStyle( // Dùng cho phụ đề, văn bản mờ
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = Muted
    ),
    labelMedium = TextStyle( // Dùng cho tên trường Input (Label)
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, color = Muted
    )
)
```

### 7. `core/designsystem/theme/Theme.kt`
- **Sử dụng ở đâu**: Nằm ở File `MinLishApp.kt` để gói toàn bộ ứng dụng, áp dụng màu và phông chữ.
```kotlin
package com.edu.minlish.core.designsystem.theme
// ...
private val DarkColorScheme = darkColorScheme(primary = Primary, onPrimary = OnPrimary, background = Background, /*...*/)
private val LightColorScheme = lightColorScheme(primary = Primary, onPrimary = OnPrimary, background = Background, /*...*/)

@Composable
fun MinLishTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Tự động lấy cấu hình sáng/tối của hệ điều hành
    content: @Composable () -> Unit // Component con (cả cái app)
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme // Chọn bảng màu
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content) // Bơm vào chuẩn Material3
}
```

### 8. `core/designsystem/component/MinLishButton.kt`
- **Sử dụng ở đâu**: Được gọi làm nút bấm chuẩn trên mọi màn hình (Login, AddWord, Translate, etc.).
```kotlin
@Composable
fun MinLishButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, 
    enabled: Boolean = true, containerColor: Color = Primary, contentColor: Color = OnPrimary
) {
    Button( // Gọi component Button gốc của Compose
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp), // Fix chiều cao nút là 52dp, rộng 100%
        shape = RoundedCornerShape(12.dp), // Bo góc 12dp
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor, contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f), // Chế độ bị tắt (disabled) mờ đi 50%
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}
```

### 9. `core/designsystem/component/MinLishTextField.kt`
- **Sử dụng ở đâu**: Hộp nhập liệu chuẩn của app (nhập Email, Mật khẩu, Từ vựng, Dịch thuật).
```kotlin
@Composable
fun MinLishTextField(
    value: String, onValueChange: (String) -> Unit, label: String, placeholder: String,
    modifier: Modifier = Modifier, visualTransformation: VisualTransformation = VisualTransformation.None, // visual = ẩn mật khẩu
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, rightElement: @Composable (() -> Unit)? = null // icon con mắt bên phải
) {
    var isFocused by remember { mutableStateOf(false) } // Nhớ trạng thái đang trỏ chuột

    Column(modifier = modifier.fillMaxWidth()) { // Xếp dọc Label phía trên ô nhập liệu
        Text(text = label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 6.dp))
        
        Box( // Ô chữ nhật chính
            modifier = Modifier.fillMaxWidth().height(48.dp).background(Color.White, RoundedCornerShape(8.dp))
                .border( // Tự đổi màu viền khi đang focus
                    width = if (isFocused) 2.dp else 1.dp, color = if (isFocused) Primary else Border, shape = RoundedCornerShape(8.dp)
                ).padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { // Chiếm phần lớn không gian
                    if (value.isEmpty()) { Text(text = placeholder, style = MaterialTheme.typography.bodyLarge.copy(color = Placeholder)) } // Gợi ý mờ
                    BasicTextField( // TextField gốc không viền
                        value = value, onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }, // Bắt sự kiện focus
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface),
                        visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, singleLine = true
                    )
                }
                rightElement?.invoke() // Render Component bên phải (ví dụ icon con mắt)
            }
        }
    }
}
```

### 10. `core/designsystem/component/MinLishBottomNav.kt`
- **Sử dụng ở đâu**: Nằm ở file `MainActivity.kt` để hiển thị thanh điều hướng đáy.
```kotlin
data class TabItem(val screen: Screen, val icon: ImageVector, val label: String)

@Composable
fun MinLishBottomNav(currentRoute: String?, onTabClick: (Screen) -> Unit) {
    val items = listOf(TabItem(Screen.Home, Icons.Default.Home, "Home"), /*... 3 màn hình kia*/)

    Column(modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        HorizontalDivider(color = Border, thickness = 1.dp) // Kẻ vạch xám phân tách với nội dung
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), /*...*/) {
            items.forEach { tab -> // Lặp 4 cái nút
                // Kiểm tra xem Màn hình hiện tại có chứa Route của tab này không để làm sáng (Active)
                val isActive = when (tab.screen) {
                    Screen.Home -> currentRoute == Screen.Home.route
                    Screen.Library -> currentRoute?.startsWith("word_list/") == true || currentRoute == Screen.Library.route || /*... các luồng con của library*/ true
                    /*... các tab khác*/
                }
                
                Column(modifier = Modifier.weight(1f).clickable { onTabClick(tab.screen) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = tab.icon, contentDescription = tab.label, tint = if (isActive) Primary else Color(0xFFAAAAAA)) // Nếu active thì đen, nếu không xám
                    Text(text = tab.label, color = if (isActive) Primary else Color(0xFFAAAAAA), fontSize = 10.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                    // Vẽ dấu chấm tròn đen báo hiệu active
                    if (isActive) Box(modifier = Modifier.size(4.dp).background(Primary, shape = CircleShape))
                    else Spacer(modifier = Modifier.height(6.dp)) 
                }
            }
        }
    }
}
```

### 11. `core/designsystem/component/ShimmerEffect.kt`
- **Sử dụng ở đâu**: Dành cho các giao diện tải dữ liệu (`LibraryScreen`, `HomeScreen`). Tạo hiệu ứng vệt sáng xám lướt qua lướt lại mô phỏng loading.
```kotlin
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) } // Nhớ kích thước khối UI đó
    val transition = rememberInfiniteTransition(label = "shimmer_transition") // Tạo chuyển động chạy mãi mãi
    
    // Tạo 1 Float tịnh tiến từ -2*size sang +2*size
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(), targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "shimmer_offset"
    )

    // Tô nền bằng hiệu ứng Gradient đổ 3 màu xám - xám nhạt - xám
    background(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFF2F2F2), Color(0xFFE5E5E5), Color(0xFFF2F2F2)),
            start = Offset(startOffsetX, 0f), end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned { size = it.size } // Lấy kích thước thật lúc render để tính toán
}
```

---

## Phần 3: Package `core.navigation` & `core.network`

### 12. `core/navigation/Screen.kt`
- **Sử dụng ở đâu**: Define TẤT CẢ TÊN ĐƯỜNG DẪN (Routes) cho điều hướng `NavHost` ở `MainActivity.kt`.
```kotlin
package com.edu.minlish.core.navigation

sealed class Screen(val route: String) { // Cấu trúc Sealed đảm bảo không khai báo route ảo bậy bạ
    object Splash : Screen("splash") // Màn hình chớp ban đầu
    object Login : Screen("login")
    // Màn hình có truyền biến số
    object WordDetail : Screen("word_detail/{wordId}") { // {wordId} là tham số
        const val ARG_WORD_ID = "wordId" // Tên hằng số truyền vào Bundle Nav
        fun createRoute(wordId: String) = "word_detail/$wordId" // Hàm tạo link an toàn để gọi khi Navigate
    }
    object QuizGame : Screen("quiz_game?setId={setId}&modes={modes}&count={count}") {
        // Query param (?a=1&b=2) dùng khi biến có thể bị rỗng
        const val ARG_SET_ID = "setId"; const val ARG_MODES = "modes"; const val ARG_COUNT = "count"
        fun createRoute(setId: String?, modes: String, count: Int) = /* ... ghép chuỗi ... */ ""
    }
    // ... Khai báo cho toàn bộ 22 màn hình
}
```

### 13. `core/network/NetworkModule.kt`
- **Sử dụng ở đâu**: Nơi tạo Client HTTP (Retrofit) để gọi tới các Web Service. Dùng cho việc tra từ điển Free (`DictionaryApiLookupStrategy`).
```kotlin
object NetworkModule {
    private const val AUTH_BASE_URL = "http://10.0.2.2:8081/" // Link giả lập server localhost
    private const val DICTIONARY_BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/" // API từ điển có thật

    private val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY } // In ra màn hình Log mọi request

    private val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build() // Trình duyệt ngầm

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create()) // Ép kiểu JSON sang Kotlin Object
            .client(client).build()
    }

    val dictionaryRetrofit: Retrofit = createRetrofit(DICTIONARY_BASE_URL)

    inline fun <reified T> createDictionaryService(): T { // reified giúp lấy Class<?> tại runtime
        return dictionaryRetrofit.create(T::class.java)
    }
}
```

---

## Phần 4: Package `core.util` & `core.notification` (Logic Đa dụng)

### 14. `core/util/AppSettings.kt`
- **Sử dụng ở đâu**: Đọc/ghi cài đặt cục bộ (Cơ sở dữ liệu tĩnh siêu nhẹ). Dùng ở mọi nơi (SettingsViewModel, LibraryViewModel, ReminderWorker).
```kotlin
object AppSettings {
    private var prefs: SharedPreferences? = null // Con trỏ tới file xml
    fun init(context: Context) { prefs = context.applicationContext.getSharedPreferences("minlish_settings", Context.MODE_PRIVATE) }
    // Khai báo property có setter & getter ngầm. Đọc/ghi key "interval_unit"
    var intervalUnit: String get() = prefs?.getString("interval_unit", "DAYS") ?: "DAYS" set(value) { prefs?.edit()?.putString("interval_unit", value)?.apply() }
}
```

### 15. `core/util/SessionDataManager.kt`
- **Sử dụng ở đâu**: Xương sống bộ nhớ tạm của app. Firebase tải dữ liệu về để vào đây, UI các màn hình quan sát (observe) các flow trong này.
```kotlin
object SessionDataManager {
    private val _userProfile = MutableStateFlow<UserProfile?>(null) // Chứa trạng thái có thể biến đổi
    val userProfileFlow = _userProfile.asStateFlow() // Giao diện chỉ được xem, không được tự biến đổi nó

    var isDataLoaded = false private set // Giúp màn hình Loading biết khi nào tải xong data vào phiên

    private var profileListener: ListenerRegistration? = null // Firebase Listener

    suspend fun preFetchUserData(userId: String) = coroutineScope {
        // Tải 1 lần (async) -> giảm độ trễ khởi động
        val profileDeferred = async { firestore.collection("users").document(userId).get().await().toObject(UserProfile::class.java) }
        userProfile = profileDeferred.await()
        isDataLoaded = true

        // Gắn móc Listener nghe lén sự thay đổi trên server, đẩy thẳng vào Flow -> UI tự cập nhật
        profileListener = firestore.collection("users").document(userId).addSnapshotListener { snapshot, error ->
                snapshot?.toObject(UserProfile::class.java)?.let { userProfile = it }
        }
    }
    // ...
}
```

### 16. `core/util/AudioPlayer.kt`
- **Sử dụng ở đâu**: Phát âm thanh (Tiếng Anh/Mỹ) ở màn hình `WordDetailScreen` và `QuizGameScreen`.
```kotlin
object AudioPlayer : TextToSpeech.OnInitListener {
    private var mediaPlayer: MediaPlayer? = null // File Player của Android
    private var tts: TextToSpeech? = null // Text To Speech của Google
    // ... onInit cài đặt giọng Mỹ (Locale.US)
    fun play(url: String, fallbackWord: String = "") {
        // Dùng API Youdao nếu URL rỗng
        val playUrl = if (url.isBlank() && fallbackWord.isNotBlank()) "https://dict.youdao.com/dictvoice?audio=${fallbackWord}&type=2" else url
        if (playUrl.isNotBlank()) {
            try {
                mediaPlayer?.release() // Xóa player cũ
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    setDataSource(playUrl)
                    prepareAsync() // Khởi tạo không làm treo app
                    setOnPreparedListener { start() }
                    setOnErrorListener { _, _, _ -> playTts(fallbackWord); true } // Lỗi mạng thì phát TTS Offline
                }
            } catch (e: Exception) { playTts(fallbackWord) }
        } else playTts(fallbackWord)
    }
    private fun playTts(word: String) { tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null) } // Phát TTS 
}
```

### 17. `core/util/AudioRecorder.kt`
- **Sử dụng ở đâu**: Dành riêng cho màn hình Luyện phát âm (`SpeakingScreen`).
```kotlin
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null 
    private var pollJob: Job? = null // Luồng đo độ ồn micro
    fun startRecording(): Boolean {
        // ... tạo thư mục lưu file REC_xxx.m4a
        // ... setEncoder(MediaRecorder.AudioEncoder.AAC) -> Lưu định dạng AAC nén cao cấp
        // ... setAudioSamplingRate(44100) -> Tần số lấy mẫu tiêu chuẩn ghi âm
        pollJob = CoroutineScope(Dispatchers.Default).launch {
            while (isRecording) {
                val amp = recorder?.maxAmplitude ?: 0 // Đo từ 0 đến 32767
                if (amp > maxAmplitudeObserved) maxAmplitudeObserved = amp
                delay(50) // Đo 20 lần 1 giây
            }
        }
    }
}
```

### 18. `core/util/SpacedRepetitionUtil.kt`
- **Sử dụng ở đâu**: Thuật toán tính ngày học SM-2. Không dùng Firebase mà lưu lịch sử lên đĩa điện thoại (Local File/SharedPreferences). Dùng trong `UpdateWordProgressUseCase`.
```kotlin
data class SpacedData(val interval: Int = 1, val repetition: Int = 0, val factor: Double = 2.5, val nextReview: Long = System.currentTimeMillis())

object SpacedRepetitionUtil {
    // ...
    fun updateData(context: Context, wordId: String, correct: Boolean) {
        val existing = getData(context, wordId) // Đọc lịch sử ôn luyện của từ
        val now = System.currentTimeMillis()
        val updated = if (existing == null) {
            // Lần đầu học
            SpacedData(interval = 1, repetition = if (correct) 1 else 0, factor = 2.5, nextReview = now + ONE_DAY_MS)
        } else {
            // Đã học nhiều lần -> Áp dụng SM-2
            var rep = existing.repetition; var intv = existing.interval; var fac = existing.factor
            if (correct) { // Đúng
                rep += 1 // Tăng chuỗi đúng liên tiếp
                intv = when (rep) { 1 -> 1; 2 -> 6; else -> (intv * fac).toInt() } // Ngày học = Khoảng cách trước * Ease Factor
            } else { // Sai
                rep = 0 // Đứt chuỗi
                intv = 1 // Học lại ngay ngày mai
                fac = (fac - 0.2).coerceAtLeast(1.3) // Hạ Ease Factor xuống, nhưng min là 1.3
            }
            SpacedData(interval = intv, repetition = rep, factor = fac, nextReview = now + intv * ONE_DAY_MS)
        }
        saveData(context, wordId, updated) // Ghi đè file
    }
}
```

### 19. `core/notification/ReminderRepository.kt` & `ScheduleReminderUseCase.kt`
- **Sử dụng ở đâu**: Khởi tạo và hủy lịch báo thức. Gọi bởi `SettingsScreen`.
```kotlin
interface ReminderRepository { fun scheduleDaily(timeString: String); fun cancelAll() } // Định nghĩa Rule (Interface)

class ScheduleReminderUseCase(private val reminderRepository: ReminderRepository) { // Class để UI gọi
    fun schedule(timeString: String) = reminderRepository.scheduleDaily(timeString)
    fun cancel() = reminderRepository.cancelAll()
}
```

### 20. `core/notification/WorkManagerReminderRepository.kt`
- **Sử dụng ở đâu**: Cài đặt Android cho Interface ở trên bằng **WorkManager**.
```kotlin
class WorkManagerReminderRepository(private val context: Context) : ReminderRepository {
    override fun scheduleDaily(timeString: String) { // timeString: "09:00 PM"
        val (hour, minute) = parseTime(timeString)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); /*...*/}
        val initialDelay = target.timeInMillis - now.timeInMillis // Tính toán milliseconds còn lại đến lúc báo

        // Tạo cục WorkManager hẹn giờ định kỳ 1 NGÀY (PeriodicWorkRequest) chạy file ReminderWorker
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork("MinLishDailyReminder", ExistingPeriodicWorkPolicy.UPDATE, workRequest) // Xếp hàng WorkManager, đè lền cái cũ nếu có
    }
}
```

### 21. `core/util/ReminderWorker.kt`
- **Sử dụng ở đâu**: Được `WorkManager` (chạy ngầm hệ điều hành) đánh thức vào đúng giờ hẹn.
```kotlin
class ReminderWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        // Được Android HĐH kích hoạt -> Gọi lệnh bắn Notification ra màn hình
        NotificationHelper.showReminderNotification(applicationContext, "MinLish Study Time! 🔥", "Keep up your streak!...")
        return Result.success()
    }
}
```

### 22. `core/util/NotificationHelper.kt`
- **Sử dụng ở đâu**: Được `ReminderWorker` ở trên gọi ra để hiện cục Noti lên hệ thống.
```kotlin
object NotificationHelper {
    fun showReminderNotification(context: Context, title: String, message: String) {
        createNotificationChannel(context) // Từ Android 8 trở lên bắt buộc phải tạo Channel mới cho thông báo hiển thị

        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK } // Cài đặt hành động: Nếu bấm Noti -> Mở App ở MainActivity
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, "study_reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icon nhỏ xíu trên thanh trạng thái
            .setContentTitle(title).setContentText(message) // Tên và nội dung
            .setAutoCancel(true) // Bấm vào tự tắt
            .setContentIntent(pendingIntent) // Lệnh thực thi khi bấm
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification) // Bắn lệnh đẩy lên hệ thống HĐH
    }
}
```
