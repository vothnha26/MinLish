# Kế hoạch chuyển đổi StateFlow toàn diện

## Bối cảnh và Động lực
Dự án hiện đang trong quá trình chuyển đổi sang sử dụng `StateFlow`. Hiện tại, nhiều `ViewModel` đã có `StateFlow` nhưng chưa tuân thủ các chuẩn idiomatic của Compose:
- **Phân mảnh State:** Nhiều ViewModel chứa nhiều biến `StateFlow` riêng lẻ (VD: `_name`, `_category`, `_prompt`) thay vì gom nhóm vào một data class `UiState` duy nhất.
- **Lạm dụng mutableStateOf ở UI:** Trong các màn hình như `LoginScreen` và `RegisterScreen`, nhiều state liên quan đến form (email, password, lỗi) được quản lý bằng `remember { mutableStateOf(...) }`. Điều này vi phạm nguyên tắc UDF (Unidirectional Data Flow) vì ViewModel không nắm giữ toàn bộ state của UI.
- **Sử dụng value thay vì update:** Việc sử dụng `_state.value = _state.value.copy(...)` kém an toàn hơn `_state.update { it.copy(...) }` trong môi trường đa luồng.

## Phạm vi tác động (Scope & Impact)
Việc tái cấu trúc này sẽ chạm đến hầu hết các màn hình và ViewModel trong dự án, cụ thể:
1. **Auth Feature:** `AuthViewModel`, `LoginScreen`, `RegisterScreen`
2. **Profile Setup Feature:** `ProfileSetupViewModel`, `ProfileSetupScreen`
3. **Library Feature:** `CreateSetViewModel`, `AICreateSetViewModel`, `TranslateAndLookupViewModel`, cùng các Screen tương ứng.
4. **Learning / Speaking / Stats:** Chuẩn hóa các file còn lại (đổi `.value =` thành `.update`).

## Giải pháp đề xuất
1. **Gom nhóm UI State:** Mỗi ViewModel sẽ có một data class (ví dụ: `AuthUiState`, `ProfileSetupUiState`) chứa *tất cả* các trường dữ liệu cần thiết cho màn hình đó.
2. **UDF (Unidirectional Data Flow):** Tất cả input của người dùng (nhập text, check box) sẽ đẩy event qua ViewModel. ViewModel cập nhật `StateFlow`, và Compose tự động re-compose. Xóa bỏ hoàn toàn `mutableStateOf` (trừ các state thuần túy UI như animation).
3. **Chuyển đổi cú pháp:** Thay thế `.value` bằng `.update` cho MutableStateFlow.

## Kế hoạch triển khai (Phased Implementation)
Sẽ được chia thành các track/task nhỏ để dễ dàng review và test:
- **Giai đoạn 1:** Tái cấu trúc module `Auth` (Login/Register).
- **Giai đoạn 2:** Tái cấu trúc module `ProfileSetup`.
- **Giai đoạn 3:** Tái cấu trúc module `Library` (Create, Translate...).
- **Giai đoạn 4:** Rà soát và chuẩn hóa các module còn lại.

## Xác minh (Verification)
- Mở từng màn hình, kiểm tra luồng nhập liệu (form) xem có bị lag hay crash không.
- Chạy các Unit Test và UI Test hiện có để đảm bảo không gãy đổ logic.
- Đảm bảo Compose Preview không bị lỗi.