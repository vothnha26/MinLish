# Nhiệm vụ 3.4 - Analytics & Progress

Tài liệu này mô tả chi tiết cách triển khai 3 phần trong module **Analytics & Progress** của dự án MinLish:

- **3.4.1 Dashboard**
- **3.4.2 Biểu đồ**
- **3.4.3 Level estimation**

Các file chính cần làm việc:

- `app/src/main/java/com/edu/minlish/features/stats/presentation/StatsScreen.kt`
- `app/src/main/java/com/edu/minlish/features/stats/presentation/viewmodel/StatsViewModel.kt`
- `app/src/main/java/com/edu/minlish/features/stats/presentation/components/SimpleBarChart.kt`
- `app/src/main/java/com/edu/minlish/features/home/presentation/HomeScreen.kt`
- `app/src/main/java/com/edu/minlish/features/home/presentation/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/edu/minlish/features/learning/domain/model/UserReviewLog.kt`
- `app/src/main/java/com/edu/minlish/features/learning/domain/model/UserWordProgress.kt`

## 1. Dữ liệu cần sử dụng

### Collection `user_word_progress`

Mỗi document đại diện cho tiến độ hiện tại của một người dùng với một từ vựng.

Các trường đang có:

- `userId`: id người dùng Firebase Auth.
- `wordId`: id từ vựng.
- `setId`: id bộ từ.
- `easeFactor`: độ dễ của từ theo thuật toán SM-2, mặc định là `2.5`.
- `interval`: khoảng cách ôn tập hiện tại.
- `repetitions`: số lần nhớ liên tiếp.
- `nextReviewDate`: ngày/giờ ôn tiếp theo.
- `lastReviewedAt`: lần ôn gần nhất.
- `status`: trạng thái `learning`, `reviewing`, hoặc `mastered`.

Collection này dùng để tính:

- Tổng số từ đã học.
- Số từ đã thuộc.
- Số từ đang học/đang ôn.
- Tỷ lệ ghi nhớ.
- Số từ đến hạn ôn hôm nay.
- Ước lượng trình độ người học.

### Collection `user_review_logs`

Mỗi document là một lần người dùng bấm đánh giá trong màn hình Flashcard.

Các trường đang có:

- `userId`: id người dùng.
- `wordId`: id từ.
- `reviewedAt`: thời điểm ôn.
- `rating`: `AGAIN`, `HARD`, `GOOD`, `EASY`.
- `intervalBefore`: khoảng cách ôn trước khi đánh giá.
- `intervalAfter`: khoảng cách ôn sau khi đánh giá.

Collection này dùng để tính:

- Số từ đã học theo ngày trong tuần.
- Số từ đã học theo tháng.
- Tỷ lệ đánh giá Again/Hard/Good/Easy.
- Chuỗi ngày học liên tục.
- Tiến độ học hằng ngày.

## 2. 3.4.1 Dashboard

### Mục tiêu

Dashboard giúp người dùng nhìn nhanh tình hình học tập của mình, gồm:

- Chuỗi ngày học liên tục.
- Tổng số từ đã học.
- Số từ đã thuộc.
- Số từ đang học.
- Số từ đến hạn ôn hôm nay.
- Tỷ lệ ghi nhớ.
- Trình độ hiện tại.
- Tiến độ lên trình độ tiếp theo.

### Cách làm trong `StatsViewModel`

Cần mở rộng `StatsUiState.Success` để chứa đủ dữ liệu cho dashboard, biểu đồ và level estimation.

Gợi ý cấu trúc:

```kotlin
data class Success(
    val totalWords: Int,
    val masteredWords: Int,
    val learningWords: Int,
    val dueTodayWords: Int,
    val retentionRate: Float,
    val currentStreak: Int,
    val currentLevel: String,
    val nextLevel: String,
    val levelProgress: Float,
    val weeklyData: List<BarChartData>,
    val monthlyData: List<BarChartData>,
    val ratingBreakdown: RatingBreakdown
)
```

Các bước tính dữ liệu:

1. Lấy toàn bộ progress của user:

```kotlin
firestore.collection("user_word_progress")
    .whereEqualTo("userId", currentUser.id)
    .get()
    .await()
```

2. Tính tổng số từ đã học:

```kotlin
val totalWords = progresses.size
```

3. Tính số từ đã thuộc:

```kotlin
val masteredThreshold = AppSettings.masteredThreshold
val masteredWords = progresses.count {
    it.status == "mastered" || it.interval > masteredThreshold
}
```

4. Tính số từ đang học:

```kotlin
val learningWords = totalWords - masteredWords
```

5. Tính số từ đến hạn ôn hôm nay:

```kotlin
val now = Date()
val dueTodayWords = progresses.count {
    it.nextReviewDate.before(now) || it.nextReviewDate == now
}
```

6. Tính tỷ lệ ghi nhớ:

Nên ưu tiên tính từ `user_review_logs`:

```text
retentionRate = (GOOD + EASY) / tổng số rating * 100
```

Nếu user chưa có review log, có thể fallback bằng `easeFactor`:

```text
retentionRate = avgEaseFactor / 2.5 * 100
```

Giới hạn giá trị tối đa là `100%`.

7. Tính chuỗi ngày học liên tục:

Không nên hardcode `7`. Cần tính từ `user_review_logs`.

### Cách tính streak

Lấy toàn bộ log học của user, nhóm theo ngày, rồi đếm ngược từ hôm nay:

```text
Nếu hôm nay có log: bắt đầu tính từ hôm nay.
Nếu hôm nay chưa có log nhưng hôm qua có log: streak vẫn được tính đến hôm qua.
Lặp ngược từng ngày, gặp ngày không có log thì dừng.
```

Ví dụ:

- Có log ngày 30/05, 29/05, 28/05 => streak = 3.
- Hôm nay 30/05 chưa học, nhưng có log ngày 29/05 và 28/05 => streak = 2.
- Không có log hôm nay và hôm qua => streak = 0.

### Cách làm UI trong `StatsScreen`

Dashboard nên gồm:

- Header: `Progress` và nút Settings.
- Level card: hiển thị trình độ hiện tại, trình độ tiếp theo và progress bar.
- Các thẻ chỉ số nhanh:
  - Current streak.
  - Total words.
  - Mastered words.
  - Retention rate.
- Card `Due today`:
  - Hiển thị số từ cần ôn hôm nay.
  - Có thể thêm nút `Study now` để điều hướng sang Flashcard sau này.

### Tiêu chí hoàn thành

- Khi người dùng học xong Flashcard và quay lại Stats, số liệu phải cập nhật theo Firestore.
- Không còn hardcode `streak = 7` trong `StatsViewModel`.
- Khi user chưa có dữ liệu, UI hiển thị empty state hợp lý và không crash.
- Nếu Firestore lỗi, UI hiển thị `StatsUiState.Error`.

## 3. 3.4.2 Biểu đồ

### Mục tiêu

Biểu đồ giúp người dùng nhìn được tiến độ học theo thời gian:

- Số từ đã ôn trong 7 ngày gần nhất.
- Số từ đã ôn theo 4 tuần trong tháng.
- Tỷ lệ đánh giá Again/Hard/Good/Easy.
- Có thể mở rộng thêm biểu đồ phân bổ trạng thái từ: Learning/Reviewing/Mastered.

### Dữ liệu cần lấy

Lấy `user_review_logs` theo khoảng thời gian:

```kotlin
firestore.collection("user_review_logs")
    .whereEqualTo("userId", currentUser.id)
    .whereGreaterThanOrEqualTo("reviewedAt", startDate)
    .whereLessThan("reviewedAt", endDate)
    .get()
    .await()
```

Lưu ý: Firestore có thể yêu cầu composite index nếu query nhiều điều kiện. Khi chạy app lần đầu, nếu query `user_review_logs` bị lỗi index, cần kiểm tra Logcat để lấy link tạo index tự động từ Firebase Console.

### Biểu đồ tuần

Tạo danh sách 7 ngày gần nhất hoặc 7 ngày trong tuần hiện tại.

Với mỗi ngày:

- Lọc các log có `reviewedAt` nằm trong ngày đó.
- Đếm số `wordId` khác nhau để tránh một từ được ôn nhiều lần bị tính lặp.

Kết quả map sang:

```kotlin
BarChartData(label = "Mon", value = count)
```

Ví dụ:

```kotlin
val weeklyData = listOf(
    BarChartData("Mon", 10),
    BarChartData("Tue", 15),
    BarChartData("Wed", 8)
)
```

### Biểu đồ tháng

Có 2 cách làm:

1. Chia theo 4 tuần trong tháng:
   - W1: ngày 1-7.
   - W2: ngày 8-14.
   - W3: ngày 15-21.
   - W4: ngày 22 đến cuối tháng.

2. Lấy 4 tuần gần nhất:
   - Week -3.
   - Week -2.
   - Week -1.
   - This week.

Để dễ triển khai trong deadline, nên dùng cách 1.

### Rating breakdown

Từ `user_review_logs`, đếm số lượng:

```text
AGAIN count
HARD count
GOOD count
EASY count
```

Tính phần trăm:

```text
pct = count / totalRating
```

UI hiển thị:

- Tên rating.
- Số lần rating.
- Thanh progress ngang.

### Component cần dùng

Hiện dự án đã có:

- `SimpleBarChart`
- `BarChartData`

Có thể giữ component này và truyền dữ liệu thật từ `StatsViewModel`, thay vì hardcode trong `StatsScreen`.

Có thể thêm component mới:

```kotlin
@Composable
fun RatingBreakdownChart(items: List<RatingBreakdownItem>)
```

Hoặc tiếp tục render bằng `Column` như `StatsScreen` hiện tại, nhưng dữ liệu phải lấy từ ViewModel.

### Tiêu chí hoàn thành

- Weekly chart lấy dữ liệu từ `user_review_logs`, không hardcode.
- Monthly chart lấy dữ liệu từ `user_review_logs`, không hardcode.
- Rating breakdown lấy dữ liệu từ trường `rating`, không suy luận bằng `easeFactor`.
- Nếu không có log, biểu đồ hiển thị cột 0 và có empty state phù hợp.

## 4. 3.4.3 Level estimation

### Mục tiêu

Tự động ước lượng trình độ của người dùng dựa trên dữ liệu học thật, không chỉ dựa vào level người dùng chọn lúc setup profile.

Level đề xuất:

- Beginner.
- Elementary.
- Intermediate.
- Upper-intermediate.
- Advanced.
- Mastery.

Hoặc map theo CEFR:

- A1.
- A2.
- B1.
- B2.
- C1.
- C2.

### Dữ liệu đầu vào

Dùng các chỉ số:

- `totalWords`: tổng số từ user đã từng học.
- `masteredWords`: số từ đã thuộc.
- `masteryRate`: `masteredWords / totalWords`.
- `retentionRate`: tỷ lệ rating `GOOD + EASY`.
- `avgEaseFactor`: ease factor trung bình.
- `consistency`: streak hoặc số ngày có log trong 7 ngày gần nhất.

### Công thức đề xuất

Tính điểm từ 0 đến 100:

```text
score =
  wordScore * 0.35 +
  masteryScore * 0.30 +
  retentionScore * 0.25 +
  consistencyScore * 0.10
```

Trong đó:

```text
wordScore = min(totalWords / 1000 * 100, 100)
masteryScore = masteryRate * 100
retentionScore = retentionRate
consistencyScore = min(activeDaysLast7 / 7 * 100, 100)
```

Map điểm sang level:

```text
0 - 15   => A1 Beginner
16 - 30  => A2 Elementary
31 - 50  => B1 Intermediate
51 - 70  => B2 Upper-intermediate
71 - 88  => C1 Advanced
89 - 100 => C2 Mastery
```

### Hàm nên tạo

Nên tạo file mới:

`app/src/main/java/com/edu/minlish/features/stats/domain/LevelEstimator.kt`

Gợi ý logic:

```kotlin
data class LevelEstimate(
    val code: String,
    val label: String,
    val nextCode: String?,
    val score: Float,
    val progressToNext: Float
)

object LevelEstimator {
    fun estimate(
        totalWords: Int,
        masteredWords: Int,
        retentionRate: Float,
        activeDaysLast7: Int
    ): LevelEstimate {
        val masteryRate = if (totalWords > 0) masteredWords.toFloat() / totalWords else 0f
        val wordScore = (totalWords / 1000f * 100f).coerceAtMost(100f)
        val masteryScore = masteryRate * 100f
        val consistencyScore = (activeDaysLast7 / 7f * 100f).coerceAtMost(100f)

        val score = wordScore * 0.35f +
            masteryScore * 0.30f +
            retentionRate * 0.25f +
            consistencyScore * 0.10f

        return mapScoreToLevel(score)
    }
}
```

Hàm `mapScoreToLevel` sẽ trả về code, label, level tiếp theo và phần trăm tiến độ lên level tiếp theo.

### Cách hiển thị trên UI

Trong `StatsScreen`, Level Card nên hiển thị:

- `Current level`: ví dụ `B1 Intermediate`.
- `Next`: ví dụ `B2 Upper-intermediate`.
- Progress bar: `levelProgress`.
- Text phụ: `Based on mastered words, retention and study consistency`.

### Tiêu chí hoàn thành

- User mới chưa có dữ liệu được xếp A1, progress 0.
- Khi `masteredWords` và `retentionRate` tăng, level tăng theo.
- UI không còn phụ thuộc vào hardcode `"Intermediate B1"`.
- Logic level nằm ngoài Composable để dễ test.

## 5. Thứ tự triển khai đề xuất

1. Cập nhật `StatsUiState.Success` để chứa dashboard data, chart data và level estimate.
2. Tạo các helper function trong `StatsViewModel`:
   - Lấy progress.
   - Lấy logs theo khoảng ngày.
   - Tính weekly data.
   - Tính monthly data.
   - Tính rating breakdown.
   - Tính streak.
3. Tạo `LevelEstimator`.
4. Sửa `StatsScreen` để dùng dữ liệu từ ViewModel, bỏ dữ liệu hardcode.
5. Chạy build để kiểm tra lỗi compile.
6. Test với 3 trường hợp:
   - User mới chưa có progress.
   - User có progress nhưng chưa có review log.
   - User có nhiều review log trong nhiều ngày.

## 6. Rủi ro cần tránh

- Không query Firestore trực tiếp trong Composable. Mọi query nên đặt trong ViewModel hoặc Repository.
- Không hardcode chart data trong UI.
- Không dùng email string để xác định role admin cho các phần analytics sau này; nên dùng field `role`.
- Cần cẩn thận timezone khi tính start/end of day.
- Nếu query Firestore bị lỗi index, kiểm tra Logcat và tạo index theo link Firebase Console trả về.
- File `hs_err_pid26032.log` là crash log JVM, nên đưa vào `.gitignore` và không dùng làm tài liệu tiến độ.

## 7. Kết quả mong muốn

Sau khi hoàn thành 3.4.1, 3.4.2 và 3.4.3:

- Dashboard Stats hiển thị số liệu thật theo user đang đăng nhập.
- Biểu đồ tuần/tháng cập nhật theo lịch sử học trong `user_review_logs`.
- Rating breakdown phản ánh đúng hành vi học của user.
- Level của user được ước lượng từ dữ liệu học thật, có progress lên level tiếp theo.
- Module Analytics sẵn sàng để mở rộng cho Admin Dashboard sau này.

## 8. Ghi chú triển khai đã thực hiện

### 8.1 File đã tạo mới

Đã tạo file:

`app/src/main/java/com/edu/minlish/features/stats/domain/LevelEstimator.kt`

Nội dung đã làm:

- Tạo `LevelEstimate` để chứa thông tin level hiện tại.
- Tạo `LevelEstimator.estimate(...)` để tính level dựa trên:
  - Tổng số từ đã học.
  - Số từ đã thuộc.
  - Tỷ lệ ghi nhớ.
  - Số ngày có hoạt động trong 7 ngày gần nhất.
- Tạo công thức score từ 0 đến 100.
- Map score sang các level A1, A2, B1, B2, C1, C2.
- Tính `progressToNext` để hiển thị thanh tiến độ lên level tiếp theo.

### 8.2 File đã cập nhật

Đã cập nhật:

`app/src/main/java/com/edu/minlish/features/stats/presentation/viewmodel/StatsViewModel.kt`

Nội dung đã làm:

- Mở rộng `StatsUiState.Success` để chứa:
  - `totalWords`
  - `masteredWords`
  - `learningWords`
  - `dueTodayWords`
  - `retentionRate`
  - `ratingBreakdown`
  - `currentStreak`
  - `weeklyData`
  - `monthlyData`
  - `levelEstimate`
- Bỏ phần mock `streak = 7`.
- Bỏ cách suy luận rating breakdown bằng `easeFactor`.
- Lấy dữ liệu thật từ Firestore:
  - `user_word_progress`
  - `user_review_logs`
- Tính `retentionRate` theo rating thật:

```text
(GOOD + EASY) / tổng số review log * 100
```

- Nếu chưa có review log thì fallback về `easeFactor`.
- Tính weekly chart từ 7 ngày gần nhất.
- Tính monthly chart theo 4 tuần trong tháng hiện tại.
- Tính streak bằng cách nhóm log theo ngày và đếm ngược từ hôm nay/hôm qua.
- Gọi `LevelEstimator` để tính level hiện tại.

Đã cập nhật:

`app/src/main/java/com/edu/minlish/features/stats/presentation/StatsScreen.kt`

Nội dung đã làm:

- Level card không còn hardcode `Intermediate B1`.
- Level card hiển thị:
  - Level hiện tại.
  - Level tiếp theo.
  - Score.
  - Thanh tiến độ lên level tiếp theo.
- Dashboard mini cards lấy dữ liệu thật từ `StatsUiState`.
- Weekly chart dùng `uiState.weeklyData`.
- Monthly chart dùng `uiState.monthlyData`.
- Rating breakdown dùng dữ liệu từ `user_review_logs`.
- Streak calendar dùng dữ liệu weekly thật thay vì danh sách hardcode.
- Text streak dùng `uiState.currentStreak` thay vì cố định `7-day streak`.

### 8.3 Kết quả hiện tại

Phần 3.4.1 Dashboard đã được triển khai ở mức dữ liệu thật:

- Total words.
- Due today.
- Mastered words.
- Retention.
- Current streak.
- Level estimation.

Phần 3.4.2 Biểu đồ đã được triển khai ở mức dữ liệu thật:

- Weekly bar chart lấy từ `user_review_logs`.
- Monthly bar chart lấy từ `user_review_logs`.
- Rating breakdown lấy từ rating `AGAIN`, `HARD`, `GOOD`, `EASY`.

Phần 3.4.3 Level estimation đã được triển khai:

- Có logic riêng trong `LevelEstimator`.
- Có score.
- Có progress lên level tiếp theo.
- UI đã hiển thị level động.

### 8.4 Việc vẫn cần kiểm thử thêm

- Cần chạy app với user thật có dữ liệu Firestore để kiểm tra số liệu hiển thị đúng.
- Firestore có thể yêu cầu tạo composite index cho query `user_review_logs`; nếu Firebase báo lỗi, kiểm tra Logcat để lấy link tạo index tự động.
- Cần kiểm tra timezone khi tính start/end of day trên thiết bị thật.
- Cần kiểm tra UI khi user mới chưa có progress hoặc review log.
- Có thể cần cải tiến `StatsScreenPreview` sau này để không phụ thuộc ViewModel thật.
