Phân tích nghiệp vụ
1. Luồng nghiệp vụ cốt lõi
   Hệ thống vận hành theo chu trình khép kín: người dùng tạo bộ từ vựng → học qua flashcard → SRS tự động lên lịch ôn → luyện tập hàng ngày → hệ thống thống kê tiến độ & nhắc nhở. Đây là vòng lặp giữ engagement cao, phù hợp với phương pháp học ngôn ngữ khoa học.

2. Module User Management
   Nghiệp vụ: Xác thực và cá nhân hóa trải nghiệm học.
   Người dùng đăng ký bằng email/password hoặc Google OAuth. Sau khi đăng nhập, hệ thống lưu hồ sơ gồm mục tiêu học (IELTS/giao tiếp...) và trình độ (A1–C2). Thông tin này được dùng để gợi ý bộ từ phù hợp và điều chỉnh kế hoạch học.
   Điểm kỹ thuật quan trọng: JWT cần có cơ chế refresh token; bcrypt nên dùng cost factor ≥ 12.

3. Module Vocabulary Management
   Nghiệp vụ: Cung cấp "kho nguyên liệu" cho việc học.
   Mỗi từ vựng mang đủ thông tin để học theo ngữ cảnh: word, pronunciation, meaning, description (EN), example, collocation, related words, note. Đây là lợi thế so với flashcard đơn giản — học từ trong ngữ cảnh tự nhiên giúp nhớ lâu hơn.
   Tính năng import CSV/Excel rất quan trọng vì giúp onboarding nhanh (người dùng không phải nhập từng từ thủ công), đặc biệt với học sinh IELTS đã có sẵn wordlist.

4. Module Learning Engine — trọng tâm của hệ thống
   Nghiệp vụ: Tối ưu hóa thời gian học — học đúng từ, đúng lúc, đúng lượng.
   Thuật toán SM-2 hoạt động như sau:

Sau mỗi lần ôn, người dùng tự đánh giá: Again / Hard / Good / Easy (tương đương rating 0–5)
Hệ thống tính ease factor mới và interval (số ngày đến lần ôn tiếp theo)
Từ trả lời Again sẽ reset về ngày 1; từ trả lời Easy có thể nhảy nhiều tuần

Daily Learning Plan cần cân bằng giữa từ mới (new cards) và từ cần ôn (due cards), tránh tình trạng "nợ ôn" tích lũy quá nhiều.

5. Module Analytics & Progress
   Nghiệp vụ: Tạo động lực duy trì học tập.
   Ba chỉ số cốt lõi: streak (chuỗi ngày học liên tục — yếu tố gamification quan trọng nhất), accuracy (% đúng theo từng bộ từ), retention rate (tỷ lệ nhớ qua thời gian — đo hiệu quả thực sự của SRS).
   Level estimation (Beginner/Intermediate/Advanced) nên dựa vào tỷ lệ từ đã thành thạo, không chỉ số lượng.

6. Module Notification System
   Nghiệp vụ: Đảm bảo người dùng không bỏ lịch ôn — vì SRS mất hiệu quả nếu ôn trễ.
   Cần phân biệt hai loại thông báo:

Daily reminder: nhắc giờ học cố định (cá nhân hóa theo múi giờ, thói quen người dùng)
Due cards alert: nhắc khi có từ đến hạn ôn (time-sensitive hơn)

Push notification cho mobile cần đăng ký FCM/APNs; email là fallback khi người dùng không bật push.

7. Các rủi ro nghiệp vụ cần lưu ý
   Rủi roMô tảGiải pháp đề xuấtOver-due accumulationNgười dùng nghỉ dài, hàng trăm từ cần ôn cùng lúcCap số từ ôn/ngày, có chế độ "recovery mode"Ease inflationNgười dùng luôn bấm "Easy" để tránh ônKhông ảnh hưởng nhiều; SM-2 tự điều chỉnhData loss khi importFile CSV sai format làm hỏng bộ từValidate + preview trước khi importNotification fatigueNhắc quá nhiều → người dùng tắt thông báoGiới hạn tần suất, cho phép tùy chỉnh