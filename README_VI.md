# Nhật ký kỹ thuật - bản vá camera cho APK Android

## Quan trọng: đây là bộ bản vá, chưa phải mã nguồn đầy đủ của APK V3.6.26

File APK anh cung cấp là APK đã biên dịch (có `classes.dex`), không có project Android Studio/Gradle gốc. Vì vậy không thể khôi phục chính xác toàn bộ mã nguồn, cấu hình Firebase/thông báo đẩy và các chức năng khác chỉ bằng cách đưa APK lên GitHub.

Bộ này chứa phần mã cần ghép vào project Android gốc để sửa lỗi WebView không mở được camera, cùng workflow GitHub Actions mẫu. Không thay thế toàn bộ `MainActivity` hiện tại bằng một WebView mới vì có thể làm mất logic thông báo đẩy và các chức năng riêng của app.

## Cách ghép bản vá

1. Mở project Android Studio gốc của Nhật ký kỹ thuật.
2. Trong `app/src/main/AndroidManifest.xml`, thêm quyền CAMERA, feature và provider theo `patch/AndroidManifest-additions.xml`.
3. Tạo `app/src/main/res/xml/file_paths.xml` từ file mẫu `app/src/main/res/xml/file_paths.xml`.
4. Mở Activity đang quản lý WebView. APK hiện tại có package/class `com.engineering.nhatky.MainActivity`.
5. Ghép các import, field, launcher, hàm `openFileChooser()` và override `onShowFileChooser()` từ `patch/MainActivity-camera-snippet.kt` vào Activity hiện có. Chỉ có một `webChromeClient`; nếu đã có `WebChromeClient`, thêm override vào object/class hiện tại, không tạo một WebChromeClient thứ hai ghi đè cái cũ.
6. Đảm bảo WebView đã bật JavaScript nếu trang hiện tại cần JavaScript. Cập nhật URL máy chủ của ứng dụng thành:

   `https://vancuong.reyeeddns.com:8767`

   Nếu project có hằng số như `BASE_URL`, `SERVER_URL` hoặc URL được truyền trực tiếp vào `webView.loadUrl(...)`, hãy thay giá trị cũ bằng URL HTTPS trên. Không dùng lại địa chỉ HTTP cũ.
7. Build thử trên Android Studio, cài lên điện thoại thật và kiểm tra cả camera lẫn Bộ sưu tập.

## Địa chỉ máy chủ cần dùng

`https://vancuong.reyeeddns.com:8767`

Đây là địa chỉ HTTPS chính thức cần cấu hình trong mã nguồn Android trước khi build APK. Bộ vá camera không tự sửa URL bên trong APK đã biên dịch.

## Build trên GitHub

Workflow `.github/workflows/android-build.yml` sẽ build khi project Android Gradle đầy đủ được đặt ở repository root và có `gradlew`.

- Đưa project Android Studio gốc lên GitHub, không đưa APK cũ vào thay cho source.
- Đặt `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEY_ALIAS`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_PASSWORD` trong GitHub Repository > Settings > Secrets and variables > Actions.
- Workflow hiện chỉ build và lưu APK artifact. Phần ký release chỉ bật sau khi có đúng file keystore Android gốc và xác minh nó khớp chữ ký APK đang phát hành.

## Bảo mật khóa ký

Không commit `nhatky-signing-key.pem`, `.jks`, `.keystore`, mật khẩu, Firebase service account hoặc `google-services.json` vào repository công khai. File PEM anh gửi là private key dạng PEM, không phải Android keystore `.jks` hoàn chỉnh; không thể mặc định dùng nó để ký APK cập nhật cài đè. Nếu ký bằng khóa khác, Android có thể từ chối cập nhật và yêu cầu gỡ app cũ (có nguy cơ mất dữ liệu cục bộ).

## Kiểm thử bắt buộc

- Lần đầu bấm Chụp ảnh: Android hỏi quyền camera.
- Chấp thuận: camera mở, chụp xong ảnh được trả lại WebView.
- Từ chối quyền camera: Bộ sưu tập vẫn mở được.
- Bấm chọn ảnh/Bộ sưu tập: vẫn hoạt động như trước.
- Hủy camera hoặc bộ chọn: callback được trả `null`, không khóa lần chọn ảnh tiếp theo.
