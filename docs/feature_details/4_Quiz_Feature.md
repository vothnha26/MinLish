# MinLish - Giải thích Chi tiết Từng dòng Code Feature Quiz (StateFlow)

Tài liệu này giải thích cặn kẽ TỪNG DÒNG CODE và LUỒNG HOẠT ĐỘNG của `QuizViewModel` và `QuizGameScreen` (đã được cập nhật kiến trúc StateFlow).

---

## 1. Mổ xẻ Chi tiết `QuizViewModel.kt`

`QuizViewModel` đóng vai trò là "Bộ não" của trò chơi. Nó lưu trữ điểm số, trạng thái UI và xử lý mọi thao tác của người dùng.

### 1.1. Khai báo các State (Trạng thái)
ViewModel dùng kiến trúc `StateFlow` gồm 2 biến cho mỗi thuộc tính: 1 biến `_biến` (dùng nội bộ để ghi) và 1 biến `biến` (bộc lộ ra ngoài cho UI đọc).

```kotlin
// [1] Trạng thái tổng quan của màn hình (Đang xoay, Lỗi, Thành công hiện câu hỏi, hay Đã xong)
private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

// [2] Vị trí của câu hỏi hiện tại đang chơi (bắt đầu từ 0)
private val _currentIndex = MutableStateFlow(0)
val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

// [3] Điểm số hiện tại & Tổng điểm tối đa
private val _score = MutableStateFlow(0)
val score: StateFlow<Int> = _score.asStateFlow()
private val _maxScore = MutableStateFlow(0)
val maxScore: StateFlow<Int> = _maxScore.asStateFlow()
```
**Luồng gọi:** UI sẽ "lắng nghe" các biến không có dấu gạch dưới (VD: `uiState`). Mỗi khi ViewModel gọi hàm `.update {}` vào biến `_uiState`, màn hình UI sẽ tự động vẽ lại (recompose) lập tức.

### 1.2. Luồng hoạt động: Load câu hỏi (`loadQuiz`)
Được gọi tự động khi `QuizGameScreen` vừa mở lên (thông qua `LaunchedEffect`).

```kotlin
fun loadQuiz(setId: String?, modes: String = "MULTIPLE_CHOICE", questionCount: Int = 10) {
    // Kiểm tra xem User đã đăng nhập chưa
    val currentUser = authRepository.getCurrentUser()
    if (currentUser == null) {
        // Nếu chưa, đẩy trạng thái Lỗi ra màn hình
        _uiState.update { QuizUiState.Error("User not logged in") }
        return
    }

    // Mở một luồng chạy ngầm (Coroutines) để gọi mạng (Database)
    viewModelScope.launch {
        // Lệnh cho UI hiện vòng xoay Loading
        _uiState.update { QuizUiState.Loading }
        // Xóa sạch điểm số, đưa câu hỏi về câu 0
        resetSession()
        
        // Gọi database để lấy danh sách từ vựng đến hạn ôn tập
        repository.getDueWords(currentUser.id, setId, forceAll = true)
            .onSuccess { dueWords ->
                // Nếu User chưa có từ vựng nào
                if (dueWords.isEmpty()) {
                    _uiState.update { QuizUiState.Error("Chưa có từ vựng nào để luyện tập...") }
                    return@launch
                }

                // Lưu lại tiến trình học cũ vào Cache để lát chấm điểm xong còn cộng dồn lên
                wordProgressCache = dueWords.associate { (word, progress) -> word.id to progress }

                // Gọi thuật toán băm từ vựng thành các câu hỏi trắc nghiệm/nối chữ
                val finalQuestions = buildQuestionsUseCase(dueWords, modes, questionCount)

                // Tính tổng điểm. Câu nối thẻ tính 4 điểm, câu khác tính 1 điểm.
                _maxScore.update { finalQuestions.sumOf { q ->
                    when (q.type) { QuestionType.MATCHING -> q.matchingPairs.size else -> 1 }
                } }

                // Ra lệnh cho UI ngừng xoay, chuyển sang hiển thị giao diện chơi game (Success)
                _uiState.update { QuizUiState.Success(finalQuestions) }
            }
            .onFailure { e -> // Bắt lỗi mạng
                _uiState.update { QuizUiState.Error(e.message ?: "Không thể tải câu hỏi.") }
            }
    }
}
```

### 1.3. Luồng hoạt động: Trả lời Trắc nghiệm (`selectOption`)
Được gọi từ UI khi người dùng lấy ngón tay chạm vào 1 trong 4 nút đáp án. `index` là số thứ tự của nút (0, 1, 2, 3).

```kotlin
fun selectOption(index: Int) {
    // Bước 1: Khóa nút. Nếu `_selectedOptionIndex` đã có giá trị tức là User đã bấm rồi, hàm return ngay.
    if (_selectedOptionIndex.value != null) return
    
    // Bước 2: Lưu lại vị trí nút vừa bấm để UI đổi màu viền của nút đó
    _selectedOptionIndex.update { index }

    // Lấy câu hỏi hiện tại ra để chấm điểm
    val state = _uiState.value as? QuizUiState.Success ?: return
    val question = state.questions[_currentIndex.value]
    
    // So sánh: Nút bấm có trùng với vị trí đáp án đúng không?
    val isCorrect = index == question.correctIndex

    // Nếu đúng, cộng 1 điểm
    if (isCorrect) _score.update { it + 1 }
    
    // Gọi hàm lưu kết quả lên Database
    recordAnswer(question, isCorrect)
}
```

### 1.4. Luồng hoạt động: Ghép thẻ Matching (`onCardClick`)
Được gọi khi người dùng chạm vào bất kỳ thẻ tiếng Anh hoặc tiếng Việt nào trên màn hình.

```kotlin
fun onCardClick(cardText: String, isEnglish: Boolean) {
    val state = _uiState.value as? QuizUiState.Success ?: return
    val question = state.questions[_currentIndex.value]
    
    // Xóa lỗi nhấp nháy đỏ của lần bấm sai trước đó (nếu có)
    _matchingErrorPair.update { null }

    if (isEnglish) {
        // Nếu người dùng bấm Thẻ Tiếng Anh
        // Kiểm tra xem thẻ này đã được nối thành công trước đó chưa? Nếu rồi thì return không làm gì.
        if (_matchedEnglishCards.value.contains(cardText)) return
        
        // Nếu thẻ đang được chọn, bấm thêm phát nữa thì bỏ chọn (nhả ra null). Ngược lại thì gán tên thẻ vào biến.
        _selectedEnglishCard.update { if (it == cardText) null else cardText }
    } else {
        // Tương tự với việc bấm Thẻ Tiếng Việt
        if (_matchedVietnameseCards.value.contains(cardText)) return
        _selectedVietnameseCard.update { if (it == cardText) null else cardText }
    }

    // Lấy tên 2 thẻ đang được chọn
    val eng = _selectedEnglishCard.value
    val viet = _selectedVietnameseCard.value
    
    // Nếu người dùng đã chọn đủ cả 1 Anh và 1 Việt
    if (eng != null && viet != null) {
        // So sánh với đáp án (matchingPairs) xem 2 từ này có khớp nghĩa nhau không
        val matched = question.matchingPairs.any { it.first == eng && it.second == viet }
        
        if (matched) {
            // ĐÚNG: Ném 2 thẻ này vào danh sách "Đã nối thành công" để UI làm mờ thẻ đi
            _matchedEnglishCards.update { it + eng }
            _matchedVietnameseCards.update { it + viet }
            // Xóa vùng chọn
            _selectedEnglishCard.update { null }
            _selectedVietnameseCard.update { null }
            // Cộng 1 điểm
            _score.update { it + 1 }
        } else {
            // SAI: Ném tên 2 thẻ vào biến Error để màn hình bọc viền đỏ nhấp nháy báo sai
            _matchingErrorPair.update { Pair(eng, viet) }
            // Xóa vùng chọn để người dùng chọn lại
            _selectedEnglishCard.update { null }
            _selectedVietnameseCard.update { null }
        }
    }
}
```

### 1.5. Luồng hoạt động: Chuyển câu (`nextQuestion`)
Được gọi khi người dùng bấm nút "Next Question" nằm ở dưới cùng màn hình (nút này chỉ hiện ra sau khi trả lời xong câu hiện tại).

```kotlin
fun nextQuestion() {
    val state = _uiState.value as? QuizUiState.Success ?: return
    // Kiểm tra xem đã tới câu cuối cùng chưa
    if (_currentIndex.value < state.questions.size - 1) {
        // Nếu chưa: Tăng số chỉ mục lên 1 để qua câu mới
        _currentIndex.update { it + 1 }
        
        // Dọn dẹp tàn dư của câu cũ (Reset lại đáp án đã chọn, ô nhập text, lỗi ghép chữ, v.v.)
        _selectedOptionIndex.update { null }
        _spellingInput.update { "" }
        _isSpellingChecked.update { false }
        _isSpellingCorrect.update { false }
        resetMatchingState()
    } else {
        // Nếu đã làm hết câu: Đẩy trạng thái sang Finished kèm theo tổng điểm số.
        _uiState.update { QuizUiState.Finished(score = _score.value, maxScore = _maxScore.value) }
    }
}
```

---

## 2. Mổ xẻ Chi tiết `QuizGameScreen.kt`

File này chứa giao diện Jetpack Compose. Nó trực tiếp "lắng nghe" các Flow trong ViewModel để vẽ ra màn hình.

### 2.1. Lắng nghe State & Gọi LoadQuiz (Khởi tạo)

```kotlin
@Composable
fun QuizGameScreen(viewModel: QuizViewModel = ...) {
    // 1. LẮNG NGHE DỮ LIỆU
    // collectAsStateWithLifecycle(): Quan sát luồng dữ liệu từ ViewModel.
    // Hễ _score trong ViewModel tăng 1, biến `score` ở đây tự động tăng 1 và màn hình vẽ lại ngay lập tức.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val score by viewModel.score.collectAsStateWithLifecycle()
    val maxScore by viewModel.maxScore.collectAsStateWithLifecycle()

    // 2. CHẠY LOGIC LẦN ĐẦU MỞ MÀN HÌNH
    LaunchedEffect(normalizedSetId, modes, questionCount) {
        // Gọi ViewModel để tải câu hỏi từ Internet/Local DB về
        viewModel.loadQuiz(normalizedSetId, modes, questionCount)
    }

    // 3. DỌN DẸP KHI THOÁT
    DisposableEffect(Unit) {
        // Khi người dùng ấn nút Back thoát game, lệnh onDispose chạy để tắt bộ nhớ phát nhạc (AudioPlayer)
        onDispose { AudioPlayer.release() }
    }
    
    // 4. VẼ KHUNG MÀN HÌNH CHÍNH (Scaffold bao gồm thanh Header ở trên)
    Scaffold(...) { padding ->
        Box(...) {
            // Dựa vào uiState, màn hình rẽ làm 4 nhánh: Xoay tròn, Chữ đỏ báo lỗi, Hiện tổng kết quả, Vẽ trò chơi
            when (val state = uiState) {
                is QuizUiState.Success -> { ... } // Chạy vào nhánh này khi có câu hỏi
            }
        }
    }
}
```

### 2.2. Vẽ màn hình Trắc nghiệm (`MultipleChoiceLayout`)
Được gọi nếu `currentQuestion.type == MULTIPLE_CHOICE`.

```kotlin
@Composable
fun MultipleChoiceLayout(question: QuizQuestion, viewModel: QuizViewModel) {
    // Lấy ra vị trí nút mà người dùng đang chọn (từ ViewModel)
    val selectedOptionIndex by viewModel.selectedOptionIndex.collectAsStateWithLifecycle()
    
    // Biến này bằng True nếu người dùng ĐÃ BẤM CHỌN ít nhất 1 đáp án
    val isAnswered = selectedOptionIndex != null

    Column(...) {
        // [PHẦN 1] THẺ TIẾNG ANH TO Ở TRÊN
        Card(...) {
            // Vẽ chữ Tiếng Anh và phiên âm
            Text(text = question.word.word) 
            Text(text = question.word.pronunciation)
            
            // Nút bấm hình Cái loa
            IconButton(onClick = { 
                // Gọi module phát âm thanh, truyền link MP3 vào
                AudioPlayer.play(question.word.audioUrl) 
            }) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, ...)
            }
        }

        // [PHẦN 2] VÒNG LẶP VẼ 4 NÚT ĐÁP ÁN TIẾNG VIỆT
        question.options.forEachIndexed { index, option ->
            // Kiểm tra nút số [index] này có đang bị bấm không? Có phải là đáp án đúng không?
            val isSelected = selectedOptionIndex == index
            val isCorrect = index == question.correctIndex

            // LOGIC TÍNH TOÁN MÀU SẮC (Cực kỳ quan trọng để tạo hiệu ứng game)
            val containerColor = when {
                !isAnswered -> Color.White            // Nếu chưa bấm chọn gì -> 4 nút màu trắng
                isCorrect -> Color(0xFFE8F5E9)        // Đáp án Đúng (kể cả có bấm hay không) -> Đổi thành màu Xanh
                isSelected -> Color(0xFFFFEBEE)       // Nút mà mình bấm Nhầm (Sai) -> Đổi thành màu Đỏ
                else -> Color.White                   // Các nút sai mà mình không bấm -> Để yên màu Trắng
            }
            
            // Vẽ nút
            Button(
                onClick = { 
                    // Khi người dùng chạm ngón tay vào Nút, gửi Lệnh (index) về cho ViewModel xử lý điểm
                    viewModel.selectOption(index) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = containerColor), // Áp dụng màu vừa tính ở trên
            ) {
                Text(text = option) // Nghĩa Tiếng Việt
            }
        }

        // [PHẦN 3] NÚT NEXT QUESTION
        if (isAnswered) { // Chỉ xuất hiện sau khi người dùng đã chạm vào 1 đáp án
            MinLishButton(text = "Next Question", onClick = { viewModel.nextQuestion() })
        }
    }
}
```

### 2.3. Vẽ màn hình Nối thẻ (`MatchingLayout`)
Được gọi nếu `currentQuestion.type == MATCHING`.

```kotlin
@Composable
fun MatchingLayout(question: QuizQuestion, viewModel: QuizViewModel) {
    // Tách cặp từ ra và XÁO TRỘN ngẫu nhiên, nếu không xáo trộn thì chữ tiếng Anh và chữ tiếng Việt sẽ nằm chung 1 hàng, quá dễ.
    val englishWords = remember(question) { question.matchingPairs.map { it.first }.shuffled() }
    val vietnameseWords = remember(question) { question.matchingPairs.map { it.second }.shuffled() }

    // Lấy trạng thái của các thẻ từ ViewModel (Thẻ nào bị chọn, thẻ nào nối đúng, thẻ nào bấm sai)
    val matchedEnglishCards by viewModel.matchedEnglishCards.collectAsStateWithLifecycle()
    val selectedEnglishCard by viewModel.selectedEnglishCard.collectAsStateWithLifecycle()
    val matchingErrorPair by viewModel.matchingErrorPair.collectAsStateWithLifecycle()

    Row(...) {
        // [CỘT BÊN TRÁI] - CỘT CHỨA CÁC THẺ TIẾNG ANH
        Column(modifier = Modifier.weight(1f)) {
            // Lặp qua từng chữ Tiếng Anh để vẽ thẻ
            englishWords.forEach { word ->
                // Kiểm tra trạng thái của cái Thẻ này dựa vào Database trong ViewModel
                val isMatched = matchedEnglishCards.contains(word) // Thẻ đã nối xong chưa?
                val isSelected = selectedEnglishCard == word       // Thẻ có đang bị ngón tay bấm vào không?
                val isErr = matchingErrorPair?.first == word       // Thẻ này có bị bấm sai với từ kia không?

                // TÍNH MÀU SẮC DỰA VÀO TRẠNG THÁI
                val borderColor = when {
                    isMatched -> Color.Transparent // Đã nối xong: Xóa viền
                    isErr -> Color(0xFFFF5252)     // Nối sai: Đổi viền sang Đỏ nhấp nháy
                    isSelected -> Primary          // Đang bấm chọn đợi thẻ bên kia: Viền màu Xanh Lam
                    else -> Border                 // Bình thường: Viền Xám mờ
                }

                Card(
                    modifier = Modifier
                        // Cấu hình sự kiện bấm: Thẻ đã nối xong (isMatched = true) thì KHÓA nút không cho bấm nữa
                        .clickable(enabled = !isMatched) { 
                            // Gửi lệnh về cho ViewModel. Báo cho ViewModel biết vừa có Thẻ Tiếng Anh (isEnglish = true) bị bấm.
                            viewModel.onCardClick(word, isEnglish = true) 
                        }
                ) {
                    Text(text = word) // In chữ ra thẻ
                }
            }
        }

        // [CỘT BÊN PHẢI] - CỘT CHỨA THẺ TIẾNG VIỆT
        // (Logic hoàn toàn y hệt cột Tiếng Anh, chỉ khác lúc truyền sự kiện bấm sẽ truyền `isEnglish = false`)
        Column(modifier = Modifier.weight(1f)) {
            vietnameseWords.forEach { meaning -> 
                ...
                .clickable(enabled = !isMatched) { viewModel.onCardClick(meaning, isEnglish = false) }
            }
        }
    }
}
```

---
*Tổng kết: Sự phân chia này tuân thủ chặt chẽ mô hình **UDF (Unidirectional Data Flow)** trong Android: UI (`QuizGameScreen`) chỉ vẽ theo state và truyền sự kiện `onClick` lên. ViewModel (`QuizViewModel`) nhận sự kiện, tính toán điểm số, cập nhật StateFlow, sau đó UI tự động vẽ lại sự thay đổi màu sắc ngay tắp lự.*
