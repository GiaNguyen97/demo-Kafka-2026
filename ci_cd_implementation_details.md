# Tài liệu: Chi tiết các Hạng mục Đã Triển khai cho hệ thống CI/CD Jenkins

Tài liệu này tổng hợp đầy đủ và chi tiết toàn bộ những hạng mục (file cấu hình, mã script, thiết lập) đã được tạo ra và thay đổi trong dự án để xây dựng nên đường ống CI/CD tự động bằng Jenkins.

---

## 1. Pipeline Cốt Lõi: File `Jenkinsfile`
- **Vị trí:** Nằm ở thư mục gốc của dự án (`/stress-test-demo/Jenkinsfile`)
- **Tác dụng:** Đây là "bộ não" của toàn hệ thống tự động. Nó định nghĩa toàn bộ kịch bản tự động hóa (Pipeline) dưới dạng mã (Pipeline as Code theo chuẩn Declarative của Jenkins).
- **Phân tích các thiết lập nổi bật đã config:**
  - **Khối `options`:** 
    - `disableConcurrentBuilds()`: Ngăn chặn nhiều bản deploy chạy chồng chéo lên nhau làm treo hệ thống và xung đột tài nguyên mạng.
    - `timeout(time: 15, unit: 'MINUTES')`: Cơ chế phòng vệ, nếu một lệnh nào đó bị kẹt quá 15 phút, Jenkins sẽ tự ép dừng (kill) cả tiến trình thay vì để treo ram máy chủ.
  - **Khối `parameters`:** Thêm biến truyền vào `DEPLOY_VERSION` có thể nhập tay. Bình thường biến này là `latest` để kích hoạt Build tự động, nhưng khi cần Rollback (phục hồi) thủ công, ta nhập ID số Build (ví dụ: `12`), Jenkins sẽ tự hiểu để kéo bản Docker quá khứ về chạy.
  - **Các `stages` chính:**
    - **`Check môi trường`:** Đọc tên nhánh đẩy code (Branch Name). Nhánh `main` sẽ tự định danh là Production, nhánh `staging` là sân chơi Staging...
    - **`Unit Test`:** Cấp lại quyền thực thi File `sh 'chmod +x gradlew'` cho máy chủ Jenkins (phòng hờ máy chủ Linux chặn quyền). Xong tự động chạy Test của 3 dự án độc lập. Set cờ `failFast true` để nếu gặp 1 vi phạm test nào, ngay lập tức đá lỗi ngừng toàn bộ quy trình, không cho code hôi thối đi vào server thật.
    - **`Build & Package` & `Push Docker`:** Chạy lệnh build JAR và đóng gói chúng thành File Image hệ điều hành độc lập (Dockerizing). Kết quả sẽ được push cực kì lưu loát lên kho lưu trữ đám mây an toàn tên là Docker Hub (Tài khoản: *gianguyen97*) chứ không lưu ngay trên máy.
    - **`Deploy To Target`:** Bước dỡ ứng dụng mới thay vào server qua lệnh Compose kết hợp cờ chờ Healthcheck (`--wait`).

---

## 2. Thiết Kế Dockerizing: Các File `Dockerfile`
Để CI/CD có thể đem app qua lại mọi máy chủ, mọi ứng dụng rải rác phải được chuẩn hóa qua Docker.
- **Vị trí:** Nằm rải rác ở từng service như: `frontend/Dockerfile`, `backend/async-service/Dockerfile`, `backend/direct-service/Dockerfile`...
- **Tác dụng:** Hướng dẫn Jenkins biên dịch ra các máy vi tính ảo thu nhỏ chỉ nặng vài chục MB chứa đúng ứng dụng đó.
- **Chi tiết config:**
  - Đối với Backend (Spring): Config sử dụng môi trường Runtime tinh gọn là `eclipse-temurin:21-jre-alpine`. Và tận dụng kết quả đã được khâu Compile Jenkins xử lý bằng lệnh `COPY build/libs/*.jar`. Tránh việc phải build code JAVA ngay trên máy server thực tế. Giúp giải phóng Ram cho Production.

---

## 3. Hệ thống Cấu hình Đa Môi trường (Docker Compose Overrides)
Môi trường ở Dev khác với khách hàng xài Production. Việc tách biệt là cực kì quan trọng để không bị lộ pass.
- **Vị trí:** Nằm tại `/stress-test-demo/docker/docker-compose.override.dev.yml` và `docker-compose.override.staging.yml`.
- **Tác dụng:** Cung cấp cơ chế đính kèm cấu hình (Merge Overrides). Nghĩa là ta sử dụng chung một file sườn cốt lõi là `docker-compose.yml`, sau đó Jenkins sẽ đắp thêm file override tương ứng tùy môi trường nhánh code.
- **Chi tiết config:** 
  - Trong `Jenkinsfile`, giai đoạn (stage) *Set Target Configuration* sẽ nhìn biến môi trường (Ví dụ đang là 'Dev') để nhặt cái lệnh gọi là `OVERRIDE_FILE = docker-compose.override.dev.yml`.
  - Bên trong các file Override này, các Port public ra ngoài sẽ được thay đổi phù hợp hoặc Password Admin DB cũng được cách ly bảo mật theo môi trường nhánh.

---

## 4. Thiết lập Cơ chế Tự Phục Hồi Hệ thống (Auto-Rollback)
Đây là kiến trúc đảm bảo Server luôn duy trì đặc tính High Availability (sống sót 24/7).
- **Tác dụng:** Cứu mạng cho hệ thống. Nếu Jenkins deploy xong khởi động con hàng mới mà hàng mới lăn ra chết do thiếu tài nguyên server hay sai mật khẩu Database thì nó tự quay về hàng cũ.
- **Chi tiết thiết lập:**
  - Ở khâu Deploy ở bài file `Jenkinsfile`, em sử dụng ngữ pháp `try { ... } catch { ... }` của Groovy Script.
  - Phía khối lệnh `try`: Em dùng lệnh `docker-compose up -d --wait --remove-orphans`. Lệnh này nghĩa là "Container mới vào phải hoạt động bật sáng đèn 100% thì em mới tháo code bản cũ bỏ đi". Quá trình xong vô sự, em báo Jenkins in cái ID version này lưu vô File rỗng mang tên `.env.last_stable_production`.
  - Nếu bản mới cài vô bị vấp dây lỗi tạch (Khối `catch` đón lỗi): Jenkins đọc báo lỗi dội ngược lại -> Đọc file `last_stable*` kia lôi ra số phiên bản cũ trước đó -> Chạy tự động tiến trình kéo phiên bản ổn định đó dập lại vào Server bảo toàn tính mạng ngay lập tức. Sau đó mới báo lỗi báo động gửi về Mail trưởng nhóm.

---

## 5. Tích hợp Trình Kích hoạt Tự Động: Github Webhooks qua Ngrok
Không thể lúc nào mình cũng lên Jenkins tự tay ấn nút Build. Phải tự động hóa.
- **Vấn đề:** Máy chủ Jenkins của chúng ta đang thử nghiệm là một mạng Local. Khi mình đẩy code lên trang Web Public Github, Github sẽ vô cùng hoang mang vì không thể nhìn thấy máy Jenkins nội bộ của ta mặt mũi ra sao để gọi điện thoại nhắc nhở.
- **Thiết lập:**
  - Khởi tạo tool đường hầm **Ngrok**. Nó sẽ đào ra ngoài Internet cấp cho ta một địa chỉ miền ngoài sáng ví dụ `https://abcd.ngrok-free.app` liên kết thẳng tắp vào cổng `localhost:8080` (Của Jenkins).
  - Copy URL HTTPS đó đưa cho cài đặt thẻ Webhooks trong Settings Repository Github.
- **Tác dụng:** Từ giờ, Cứ mỗi lần Team ấn lệnh `git push`, Github sẽ tức tốc ném một quả tên lửa qua đường hầm Ngrok, xuyên vào thẳng Jenkins, đánh thức Jenkins báo "Có code mới kìa kéo xuống Test và Deploy cho tôi đi!". 

---

## 6. Chỉnh sửa Gradle Config & Phân quyền Executable Shell
- Ngay trong phần CI `Jenkinsfile`, em có bố trí dòng lệnh cấu hình `sh 'chmod +x ./backend/async-service/gradlew'`. Tác dụng là cấp cứu quyền truy cập tệp script gradle thực thi. 
- Nhờ vậy, Agent Jenkins chạy trong các môi trường giả lập hạt nhân (Linux VM) sẽ không bị khóa mồm bắn văng khỏi luồng vì Lỗi Permission Denied kinh điển do lệnh `test` hay `assemble`. Hàng ngũ file Java được build độc lập và rành mạch. Lịch sử hiển thị trên Test Results Trend trong Jenkins rất minh bạch.
