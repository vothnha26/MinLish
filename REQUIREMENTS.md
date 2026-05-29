# MinLish App - Ứng dụng hỗ trợ học từ vựng tiếng Anh

## 1. Giới thiệu

### 1.1 Mục tiêu

Xây dựng ứng dụng hỗ trợ học từ vựng tiếng Anh theo phương pháp:

- Flashcard
- Spaced Repetition (lặp lại ngắt quãng)
- Context-based learning (học qua ví dụ, collocation)

### 1.2 Phạm vi

Ứng dụng cho phép:

- Người học tạo / học bộ từ vựng
- Hệ thống nhắc học thông minh
- Theo dõi tiến độ học

### 1.3 Đối tượng người dùng

- Học sinh, sinh viên
- Người học IELTS / TOEIC
- Người đi làm cần nâng cao từ vựng

## 2. Các module chính

1. User Management
2. Vocabulary Management
3. Learning Engine (Spaced Repetition)
4. Practice Module
5. Analytics & Progress
6. Notification System

## 3. Chức năng hệ thống

### 3.1 User Management

#### 3.1.1 Đăng ký / đăng nhập

- Email + password
- Google login

#### 3.1.2 Hồ sơ người dùng

- Tên
- Mục tiêu học (IELTS, giao tiếp,…)
- Level (A1–C2)

### 3.2 Vocabulary Management

#### 3.2.1 Tạo bộ từ vựng

- Tên bộ từ
- Mô tả
- Tags (IELTS, Business, Travel…)

#### 3.2.2 Thêm từ vựng

Mỗi từ gồm:

- Word
- Pronunciation
- Meaning
- Description (English)
- Example
- Collocation
- Related words
- Note

#### 3.2.3 Import / Export

- Import CSV / Excel
- Export bộ từ

### 3.3 Learning Module

#### 3.3.1 Flashcard Learning

- Front: word
- Back: meaning + example
- Flip animation

#### 3.3.2 Spaced Repetition (SRS)

Áp dụng thuật toán SM-2:

- User chọn:
    - Again
    - Hard
    - Good
    - Easy
- Hệ thống tính:
    - Next review time
    - Ease factor

#### 3.3.3 Daily Learning Plan

- Số từ mới mỗi ngày
- Số từ cần ôn

### 3.4 Progress Tracking

#### 3.4.1 Dashboard

- Số từ đã học
- Streak (chuỗi ngày học)
- Accuracy (% đúng)

#### 3.4.2 Biểu đồ

- Daily activity
- Retention rate

#### 3.4.3 Level estimation

- Beginner / Intermediate / Advanced

### 3.5. Notification System

- Nhắc học mỗi ngày
- Nhắc từ đến hạn ôn
- Email / push notification

## 4. Yêu cầu phi chức năng

### 4.1. Performance

- Load < 2s
- Support 1k user concurrent

### 4.2. Security

- JWT authentication
- Encrypt password (bcrypt)

### 4.3. Usability

- UI/UX đơn giản mà hiệu quả