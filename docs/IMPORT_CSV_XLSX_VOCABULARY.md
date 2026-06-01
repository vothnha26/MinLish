# Kế hoạch triển khai chức năng Import CSV/XLSX từ vựng

## 1. Mục tiêu

Triển khai chức năng import file `.csv` và `.xlsx` tại màn hình:

`app/src/main/java/com/edu/minlish/features/library/presentation/LibraryScreen.kt`

Người dùng bấm nút `Import CSV` trên `LibraryScreen`, chọn file từ máy, app đọc dữ liệu trong file và tạo một bộ từ vựng mới kèm danh sách từ trong Firestore.

Chức năng cần hỗ trợ:

- Import file `.csv`.
- Import file `.xlsx`.
- Tự tạo `VocabularySet` mới từ thông tin trong file hoặc từ tên file.
- Tạo nhiều `VocabularyWord` thuộc bộ từ vừa tạo.
- Cập nhật đúng `wordCount` của bộ từ.
- Hiển thị lỗi rõ ràng nếu file sai định dạng.
- Reload lại danh sách bộ từ sau khi import thành công.

## 2. Luồng người dùng

1. Người dùng mở tab `Library`.
2. Người dùng bấm nút `Import CSV` ở góc trên phải.
3. App mở trình chọn file của Android.
4. Người dùng chọn file `.csv` hoặc `.xlsx`.
5. App đọc file và kiểm tra định dạng.
6. App hiển thị màn hình xác nhận import:
   - Tên bộ từ.
   - Category.
   - Số lượng từ hợp lệ.
   - Số dòng bị lỗi nếu có.
7. Người dùng bấm `Import`.
8. App tạo `VocabularySet`.
9. App thêm từng `VocabularyWord` vào collection `vocabulary_words`.
10. App reload `LibraryScreen`.
11. Bộ từ mới xuất hiện trong danh sách.

## 3. Định dạng file đề xuất

### 3.1 Cột bắt buộc

File cần có dòng header ở hàng đầu tiên:

```csv
word,pronunciation,meaningVietnamese,definitionEnglish,exampleSentence,pos,collocations,personalNote,synonyms,antonyms
```

Trong đó:

- `word`: từ tiếng Anh, bắt buộc.
- `meaningVietnamese`: nghĩa tiếng Việt, bắt buộc.

Các cột còn lại có thể để trống.

### 3.2 Cột tùy chọn

- `pronunciation`: phiên âm.
- `definitionEnglish`: giải thích tiếng Anh.
- `exampleSentence`: ví dụ.
- `pos`: từ loại, ví dụ `noun`, `verb`, `adjective`.
- `collocations`: cụm từ đi kèm.
- `personalNote`: ghi chú cá nhân.
- `synonyms`: danh sách từ đồng nghĩa, ngăn cách bằng dấu `;`.
- `antonyms`: danh sách từ trái nghĩa, ngăn cách bằng dấu `;`.

### 3.3 Ví dụ CSV

```csv
word,pronunciation,meaningVietnamese,definitionEnglish,exampleSentence,pos,collocations,personalNote,synonyms,antonyms
abandon,/əˈbændən/,từ bỏ,to leave something behind,He abandoned the project.,verb,abandon a plan,,leave;quit,keep;continue
benefit,/ˈbenɪfɪt/,lợi ích,an advantage or profit,The benefit of exercise is clear.,noun,health benefit,,advantage;gain,drawback
```

### 3.4 Quy tắc đọc file XLSX

Với file `.xlsx`, sheet đầu tiên sẽ được dùng để import. Hàng đầu tiên là header giống CSV. Các hàng tiếp theo là dữ liệu từ vựng.

## 4. Mapping dữ liệu vào model hiện tại

Model hiện tại:

- `VocabularySet`
- `VocabularyWord`
- `WordDefinition`

### 4.1 Mapping `VocabularySet`

Khi import, app tạo một `VocabularySet`:

```kotlin
VocabularySet(
    creatorId = currentUser.id,
    title = importTitle,
    description = importDescription,
    category = selectedCategory,
    wordCount = 0
)
```

Gợi ý:

- `title`: lấy từ tên file hoặc cho người dùng nhập ở màn xác nhận.
- `description`: `"Imported from CSV/XLSX"`.
- `category`: mặc định `Custom`, hoặc cho người dùng chọn.
- `wordCount`: ban đầu là `0`, sau đó repository `addWord()` đang tự tăng `wordCount` bằng `FieldValue.increment(1)`.

### 4.2 Mapping `VocabularyWord`

Mỗi dòng hợp lệ trong file tạo một `VocabularyWord`:

```kotlin
VocabularyWord(
    vocabularySetId = createdSetId,
    word = row.word,
    pronunciation = row.pronunciation,
    audioUrl = "",
    definitions = listOf(
        WordDefinition(
            pos = row.pos,
            meaningVietnamese = row.meaningVietnamese,
            definitionEnglish = row.definitionEnglish,
            exampleSentence = row.exampleSentence,
            synonyms = row.synonyms,
            antonyms = row.antonyms
        )
    ),
    collocations = row.collocations,
    personalNote = row.personalNote,
    imageUrl = ""
)
```

## 5. Các file cần thêm mới

### 5.1 Data model cho import

Tạo file:

`app/src/main/java/com/edu/minlish/features/library/domain/model/ImportVocabularyModels.kt`

Nội dung đề xuất:

```kotlin
data class ImportVocabularyRow(
    val word: String,
    val pronunciation: String = "",
    val meaningVietnamese: String,
    val definitionEnglish: String = "",
    val exampleSentence: String = "",
    val pos: String = "",
    val collocations: String = "",
    val personalNote: String = "",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val rowNumber: Int
)

data class ImportVocabularyPreview(
    val fileName: String,
    val validRows: List<ImportVocabularyRow>,
    val errors: List<ImportVocabularyError>
)

data class ImportVocabularyError(
    val rowNumber: Int,
    val message: String
)
```

### 5.2 Parser interface

Tạo file:

`app/src/main/java/com/edu/minlish/features/library/domain/importer/VocabularyImportParser.kt`

```kotlin
interface VocabularyImportParser {
    suspend fun parse(
        fileName: String,
        inputStream: InputStream
    ): Result<ImportVocabularyPreview>
}
```

Phần parse file phải chạy trên `Dispatchers.IO`, không chạy trực tiếp trên Main thread. File CSV/XLSX có thể có vài trăm hoặc vài nghìn dòng, nếu đọc và parse ngay trong `viewModelScope.launch` mặc định sẽ làm UI bị đứng trong lúc xử lý.

Ví dụ:

```kotlin
class CsvVocabularyImportParser : VocabularyImportParser {
    override suspend fun parse(
        fileName: String,
        inputStream: InputStream
    ): Result<ImportVocabularyPreview> = withContext(Dispatchers.IO) {
        try {
            // Đọc stream và parse CSV ở đây.
            Result.success(preview)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 5.3 CSV parser

Tạo file:

`app/src/main/java/com/edu/minlish/features/library/data/importer/CsvVocabularyImportParser.kt`

Nhiệm vụ:

- Đọc `InputStream`.
- Parse header.
- Chuẩn hóa tên cột về lowercase.
- Validate cột `word` và `meaningVietnamese`.
- Bỏ qua dòng trống.
- Trả về `ImportVocabularyPreview`.

Nên ưu tiên dùng parser an toàn thay vì tự `split(",")` đơn giản, vì CSV có thể chứa dấu phẩy trong câu ví dụ. Nếu muốn nhẹ, có thể viết parser cơ bản xử lý dấu ngoặc kép. Nếu chấp nhận thêm thư viện, dùng Apache Commons CSV.

Cần xử lý an toàn khi file chỉ có các cột bắt buộc. Ví dụ người dùng có thể upload file chỉ gồm:

```csv
word,meaningVietnamese
abandon,từ bỏ
benefit,lợi ích
```

Khi đó các cột tùy chọn như `pos`, `pronunciation`, `definitionEnglish`, `exampleSentence`, `synonyms`, `antonyms` không tồn tại trong header. Nếu dùng Apache Commons CSV và gọi trực tiếp `record.get("pos")`, thư viện có thể ném `IllegalArgumentException`.

Cần viết helper lấy cột an toàn:

```kotlin
private fun CSVRecord.getOptional(name: String): String {
    return if (isMapped(name)) {
        get(name).trim()
    } else {
        ""
    }
}

private fun CSVRecord.getRequired(name: String): String {
    if (!isMapped(name)) {
        throw IllegalArgumentException("Thiếu cột bắt buộc: $name")
    }
    return get(name).trim()
}
```

Khi parse:

```kotlin
val word = record.getRequired("word")
val meaningVietnamese = record.getRequired("meaningVietnamese")
val pos = record.getOptional("pos")
val pronunciation = record.getOptional("pronunciation")
```

Nếu không dùng Apache Commons CSV, vẫn cần nguyên tắc tương tự: kiểm tra header có chứa tên cột trước khi đọc giá trị.

### 5.4 XLSX parser

Tạo file:

`app/src/main/java/com/edu/minlish/features/library/data/importer/XlsxVocabularyImportParser.kt`

Nhiệm vụ:

- Đọc sheet đầu tiên.
- Hàng đầu là header.
- Các hàng sau là dữ liệu.
- Convert cell sang `String`.
- Dùng chung logic validate với CSV.

Thư viện đề xuất:

- Apache POI đọc `.xlsx`, nhưng khá nặng cho Android.
- Nếu muốn nhẹ hơn, ưu tiên chỉ hỗ trợ `.csv` trước, sau đó thêm `.xlsx` bằng thư viện chuyên cho Android hoặc xử lý `.xlsx` ở backend.

Nếu dùng Apache POI, cần kiểm tra kỹ kích thước app và lỗi dependency trên Android.

### 5.5 Import coordinator

Tạo file:

`app/src/main/java/com/edu/minlish/features/library/data/importer/VocabularyImportManager.kt`

Nhiệm vụ:

- Nhận `Uri`.
- Lấy file name.
- Mở `InputStream` bằng `ContentResolver`.
- Chọn parser theo extension:
  - `.csv` dùng `CsvVocabularyImportParser`.
  - `.xlsx` dùng `XlsxVocabularyImportParser`.
- Trả về preview.

Phần mở `InputStream` và parse cũng phải chạy trong `Dispatchers.IO`. Nên dùng `use {}` để đóng stream sau khi đọc xong:

```kotlin
suspend fun parseFromUri(context: Context, uri: Uri): Result<ImportVocabularyPreview> =
    withContext(Dispatchers.IO) {
        val fileName = resolveFileName(context, uri)
        val parser = selectParser(fileName)

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            parser.parse(fileName, inputStream)
        } ?: Result.failure(Exception("Không thể mở file import"))
    }
```

## 6. Cập nhật repository

Hiện tại `VocabularyRepository` đã có:

```kotlin
suspend fun createSetAndGetId(set: VocabularySet): Result<String>
suspend fun addWord(word: VocabularyWord): Result<Unit>
```

Có thể dùng lại hai hàm này để import.

Tuy nhiên, import nhiều từ bằng `addWord()` sẽ gọi Firestore nhiều lần và update `wordCount` từng dòng. Với file lớn, nên thêm hàm batch:

```kotlin
suspend fun importWords(
    set: VocabularySet,
    words: List<VocabularyWord>
): Result<Unit>
```

Trong `FirestoreVocabularyRepositoryImpl`, dùng `WriteBatch`:

1. Tạo document mới cho `vocabulary_sets`.
2. Tạo nhiều document trong `vocabulary_words`.
3. Set `wordCount = words.size`.
4. Commit batch.

Điểm quan trọng: khi dùng `WriteBatch`, cần có `vocabularySetId` trước khi tạo các `VocabularyWord`. Không thể chờ set ghi lên server xong rồi mới lấy id, vì toàn bộ thao tác đang nằm trong batch. Cách đúng là tạo `DocumentReference` rỗng trước bằng `.document()`, lấy `id` từ reference đó, rồi đưa cả set và words vào batch.

Ví dụ:

```kotlin
override suspend fun importWords(
    set: VocabularySet,
    words: List<VocabularyWord>
): Result<Unit> {
    return try {
        withContext(Dispatchers.IO) {
            val batch = firestore.batch()

            val setRef = firestore.collection("vocabulary_sets").document()
            val setId = setRef.id
            val setWithId = set.copy(
                id = setId,
                wordCount = words.size
            )

            batch.set(setRef, setWithId)

            words.forEach { word ->
                val wordRef = firestore.collection("vocabulary_words").document()
                val wordWithSetId = word.copy(
                    id = wordRef.id,
                    vocabularySetId = setId
                )
                batch.set(wordRef, wordWithSetId)
            }

            batch.commit().await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

Lưu ý: `.document()` ở đây chỉ tạo reference và id ở client, chưa ghi dữ liệu lên server. Dữ liệu chỉ được ghi khi gọi `batch.commit().await()`.

Lợi ích:

- Ít request hơn.
- Tránh `wordCount` bị sai nếu import giữa chừng bị lỗi.
- Dễ rollback hơn so với thêm từng từ.

Lưu ý Firestore batch giới hạn 500 writes/lần. Một batch nếu có cả set thì gồm:

- 1 write cho `VocabularySet`.
- N write cho `VocabularyWord`.

Vì vậy batch đầu tiên chỉ nên chứa tối đa 490 từ để an toàn. Nếu file có nhiều hơn 490 từ, cần chia nhỏ bằng `chunked()`.

Ví dụ:

```kotlin
withContext(Dispatchers.IO) {
    val setRef = firestore.collection("vocabulary_sets").document()
    val setId = setRef.id
    val wordChunks = words.chunked(490)

    wordChunks.forEachIndexed { index, chunk ->
        val batch = firestore.batch()

        if (index == 0) {
            val setWithId = set.copy(
                id = setId,
                wordCount = words.size
            )
            batch.set(setRef, setWithId)
        }

        chunk.forEach { word ->
            val wordRef = firestore.collection("vocabulary_words").document()
            val wordWithSetId = word.copy(
                id = wordRef.id,
                vocabularySetId = setId
            )
            batch.set(wordRef, wordWithSetId)
        }

        batch.commit().await()
    }
}
```

Với import lớn hơn 490 từ, nhiều batch không còn đảm bảo atomic toàn bộ file. Nếu batch sau bị lỗi, Firestore có thể đã lưu set và một phần words. Cần có chiến lược xử lý:

- Giới hạn import tối đa 490 từ ở phiên bản đầu tiên để giữ atomic đơn giản.
- Hoặc cho phép import nhiều batch nhưng nếu lỗi thì hiển thị số từ đã lưu thành công và ghi log rõ ràng.
- Hoặc thêm trạng thái `importStatus = "importing" | "completed" | "failed"` cho `VocabularySet`, chỉ hiển thị set khi import hoàn tất.

## 7. Cập nhật ViewModel

Tạo hoặc mở rộng:

`app/src/main/java/com/edu/minlish/features/library/presentation/viewmodel/LibraryViewModel.kt`

Thêm state:

```kotlin
sealed class ImportUiState {
    object Idle : ImportUiState()
    object Parsing : ImportUiState()
    data class Preview(val preview: ImportVocabularyPreview) : ImportUiState()
    object Importing : ImportUiState()
    data class Success(val importedCount: Int) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}
```

Thêm function:

```kotlin
fun parseImportFile(context: Context, uri: Uri)
fun confirmImport(title: String, category: String)
fun clearImportState()
```

Luồng xử lý:

1. `parseImportFile()` đọc file và cập nhật `ImportUiState.Preview`.
2. UI hiển thị preview.
3. Người dùng bấm `Import`.
4. `confirmImport()` tạo set và words.
5. Thành công thì gọi `loadUserSets()`.

## 8. Cập nhật LibraryScreen

File:

`app/src/main/java/com/edu/minlish/features/library/presentation/LibraryScreen.kt`

Hiện tại nút import đang là:

```kotlin
onClick = { /* TODO: Import CSV */ }
```

Cần thay bằng Activity Result API:

```kotlin
val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    if (uri != null) {
        viewModel.parseImportFile(context, uri)
    }
}
```

Nút import:

```kotlin
onClick = {
    importLauncher.launch(
        arrayOf(
            "text/csv",
            "text/comma-separated-values",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    )
}
```

Cần thêm:

- `val context = LocalContext.current`
- Dialog preview import.
- Dialog loading khi đang parse/import.
- Snackbar hoặc AlertDialog khi import thành công/lỗi.

## 9. Giao diện preview import

Khi parse xong, hiển thị dialog:

Thông tin:

- Tên file.
- Ô nhập `Tên bộ từ`.
- Chọn `Category`.
- Tổng số từ hợp lệ.
- Số dòng lỗi.
- Danh sách 5 từ đầu tiên để người dùng kiểm tra.

Nút:

- `Cancel`
- `Import`

Nếu có lỗi:

- Vẫn cho import các dòng hợp lệ.
- Có thể thêm nút `View errors`.

## 10. Validate dữ liệu

Một dòng hợp lệ cần:

- `word` không rỗng.
- `meaningVietnamese` không rỗng.
- `word` không dài quá 100 ký tự.
- `meaningVietnamese` không dài quá 500 ký tự.

Nên chuẩn hóa:

- Trim khoảng trắng.
- Bỏ dòng trống.
- Xóa trùng từ trong cùng file theo `word.lowercase()`.
- `synonyms` và `antonyms` tách bằng dấu `;`.

Ví dụ lỗi:

- `Dòng 4: thiếu word`.
- `Dòng 8: thiếu meaningVietnamese`.
- `Dòng 12: từ bị trùng trong file`.

## 11. Quy tắc xử lý trùng từ

Cần chọn một trong hai hướng:

### Hướng đề xuất

Chỉ kiểm tra trùng trong file import. Nếu từ đã tồn tại ở bộ từ khác thì vẫn cho import.

Lý do:

- Một từ có thể nằm trong nhiều bộ khác nhau.
- Người dùng có thể muốn tạo bộ IELTS riêng dù từ đã có trong TOEIC.

### Hướng chặt hơn

Kiểm tra trùng trong cùng `VocabularySet`. Nếu import vào bộ đã tồn tại, bỏ qua từ trùng.

Hướng này chỉ cần nếu sau này hỗ trợ import vào bộ từ có sẵn.

## 12. Firestore schema sau import

Collection `vocabulary_sets`:

```json
{
  "creatorId": "userId",
  "title": "IELTS Import",
  "description": "Imported from CSV/XLSX",
  "category": "IELTS",
  "wordCount": 120,
  "createdAt": "Timestamp"
}
```

Collection `vocabulary_words`:

```json
{
  "vocabularySetId": "createdSetId",
  "word": "abandon",
  "pronunciation": "/əˈbændən/",
  "audioUrl": "",
  "definitions": [
    {
      "pos": "verb",
      "meaningVietnamese": "từ bỏ",
      "definitionEnglish": "to leave something behind",
      "exampleSentence": "He abandoned the project.",
      "synonyms": ["leave", "quit"],
      "antonyms": ["keep", "continue"]
    }
  ],
  "collocations": "abandon a plan",
  "personalNote": "",
  "imageUrl": "",
  "createdAt": "Timestamp"
}
```

## 13. Quyền truy cập và bảo mật

Cần đảm bảo:

- Chỉ user đang đăng nhập mới được import.
- `creatorId` của set phải là `currentUser.id`.
- Không tin dữ liệu `creatorId` từ file.
- Không cho import file quá lớn.
- Giới hạn số dòng import, ví dụ tối đa 500 từ/lần.

Nếu Firestore rules có kiểm tra owner:

- User được create `vocabulary_sets` khi `request.resource.data.creatorId == request.auth.uid`.
- User được create `vocabulary_words` nếu set thuộc về user đó.

## 14. Dependency đề xuất

### CSV

Nếu dùng Apache Commons CSV:

`gradle/libs.versions.toml`

```toml
commonsCsv = "1.11.0"
commons-csv = { group = "org.apache.commons", name = "commons-csv", version.ref = "commonsCsv" }
```

`app/build.gradle.kts`

```kotlin
implementation(libs.commons.csv)
```

### XLSX

Với `.xlsx`, cần cân nhắc kỹ.

Phương án 1: Apache POI.

Ưu điểm:

- Đọc XLSX đầy đủ.

Nhược điểm:

- Nặng.
- Có thể phát sinh lỗi dependency trên Android.

Phương án 2: Chuyển XLSX sang CSV trước khi import.

Ưu điểm:

- App nhẹ hơn.
- Dễ làm và ít lỗi hơn.

Nhược điểm:

- Người dùng phải tự export CSV.

Phương án 3: Dùng backend parse XLSX.

Ưu điểm:

- Android app nhẹ.
- Xử lý file lớn tốt hơn.

Nhược điểm:

- Cần backend/API upload file.

Đề xuất thực tế cho MinLish:

1. Giai đoạn 1: làm CSV import hoàn chỉnh.
2. Giai đoạn 2: thêm XLSX nếu cần, ưu tiên backend hoặc thư viện nhẹ tương thích Android.

## 15. Test case cần kiểm tra

### 15.1 Import thành công

- File CSV có 10 dòng hợp lệ.
- App tạo 1 `VocabularySet`.
- App tạo 10 `VocabularyWord`.
- `wordCount = 10`.
- Library reload và hiển thị bộ từ mới.

### 15.2 File thiếu header

- File không có dòng header.
- App báo lỗi rõ ràng.
- Không ghi Firestore.

### 15.3 File thiếu cột bắt buộc

- Thiếu `word`.
- Thiếu `meaningVietnamese`.
- App báo lỗi.

### 15.4 Có dòng lỗi lẫn dòng đúng

- 8 dòng đúng, 2 dòng lỗi.
- Preview hiển thị `8 từ hợp lệ, 2 dòng lỗi`.
- Nếu user xác nhận, chỉ import 8 dòng hợp lệ.

### 15.5 Từ trùng trong file

- File có `abandon` lặp 2 lần.
- Chỉ import lần đầu.
- Preview báo dòng sau bị trùng.

### 15.6 User chưa đăng nhập

- Không cho import.
- Hiển thị `User not logged in`.

### 15.7 File quá lớn

- File trên giới hạn cho phép.
- App báo lỗi và không parse.

## 16. Các bước triển khai đề xuất

1. Tạo model import.
2. Tạo CSV parser.
3. Tạo `VocabularyImportManager`.
4. Thêm state import vào `LibraryViewModel`.
5. Thêm file picker vào `LibraryScreen`.
6. Thêm preview dialog.
7. Thêm hàm batch import trong repository.
8. Reload library sau khi import thành công.
9. Test với file CSV mẫu.
10. Sau khi CSV ổn định, mới thêm XLSX.

## 17. Gợi ý UI trong LibraryScreen

Nút hiện tại:

```kotlin
Text("Import CSV", fontSize = 14.sp, color = Primary)
```

Nên đổi thành:

```kotlin
Text("Import", fontSize = 14.sp, color = Primary)
```

Lý do:

- Chức năng sẽ hỗ trợ cả CSV và XLSX.
- Text ngắn hơn, phù hợp top bar.

Dialog preview nên dùng Material 3 `AlertDialog` hoặc `ModalBottomSheet`.

## 18. Tiêu chí hoàn thành

Chức năng được xem là hoàn thành khi:

- Người dùng chọn được file `.csv`.
- App parse đúng dữ liệu.
- App hiển thị preview trước khi import.
- App tạo đúng `VocabularySet`.
- App tạo đúng `VocabularyWord`.
- `wordCount` đúng.
- Library tự reload sau import.
- File sai định dạng không làm app crash.
- Logcat có thông tin lỗi đủ rõ khi import thất bại.

## 19. Ghi chú triển khai

Ở trạng thái hiện tại, `LibraryScreen.kt` mới có nút import nhưng chưa có xử lý:

```kotlin
onClick = { /* TODO: Import CSV */ }
```

Do đó điểm bắt đầu hợp lý là thay TODO này bằng `rememberLauncherForActivityResult`, sau đó đưa logic parse/import xuống `LibraryViewModel` để giữ đúng mô hình MVVM.

Không nên parse file trực tiếp trong Composable vì:

- UI sẽ bị nặng.
- Khó test.
- Khó xử lý lỗi.
- Dễ vi phạm tách lớp giữa UI và business logic.
