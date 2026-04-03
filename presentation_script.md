# 🎤 Kịch bản Thuyết trình Demo CI/CD Pipeline với Jenkins cho Dự án Kafka Stress Test

**Mục tiêu:** Trình bày rõ ràng, mạch lạc, và trực quan cho lớp/giáo viên thấy được sự chuyên nghiệp, sức mạnh và tính minh bạch của quy trình tự động hóa (CI/CD) áp dụng công nghệ Jenkins, Docker Compose và Spring Boot.

## 🌟 Mở đầu (1-2 Phút)
- **Chào hỏi:** Chào thầy/cô và các bạn. Hôm nay nhóm (hoặc em) xin trình bày về việc áp dụng tư duy DevOps, cụ thể là thực hành quy trình CI/CD tự động bằng Jenkins cho dự án Hệ thống Kafka Stress Test.
- **Vấn đề đặt ra:** Ở các quy trình phát triển truyền thống hoặc những file đồ án đơn thuần, mỗi lần hoàn thiện code, chúng ta phải tự chạy test thủ công, tự build thành file `.jar`, tự viết lại cấu hình và đem code update lên server bằng tay qua SSH. Những thao tác này lặp đi lặp lại rất dễ hỏng hóc (Human Error) và tốn nhiều thời gian. 
- **Giải pháp:** Mục tiêu của em là triển khai phương án **Zero-click Deployment** cực kỳ mượt mà – nghĩa là lập trình viên chỉ việc viết code, đẩy code lên (git push), mọi thao tác thủ tục rườm rà còn lại hệ thống đường ống Jenkins sẽ lo từ A -> Z. 
- **Công nghệ được sử dụng:** Jenkins, Gradle, Docker, Docker Compose, Git Webhooks (Kênh truyền thông tin thông qua Ngrok).

---

## 📊 Phần 1: Kiến trúc đường ống CI/CD (Pipeline Architecture)
*(Mở màn hình Jenkins Pipeline đang chạy hoặc vẽ sơ đồ Flow trên Slide để minh họa)*
Toàn bộ quy trình sinh mệnh của mã nguồn từ lúc nằm trên máy cá nhân đến khi là sản phẩm hoạt động trên máy chủ được chia thành các hệ thống trạm tự động hóa (gọi là **Stages**).

1. **Triggering (Kích hoạt tự động):** Khi lập trình viên push code lên Github, Github sẽ tự khởi tạo một dòng sự kiện (Webhook) bắn về máy chủ Jenkins của chúng ta.
2. **Environment Detection (Phân luồng môi trường):** Đoạn mã pipeline thông minh sẽ nhìn vào tên nhánh (branch) bạn vừa cập nhật để quyết định môi trường. Ví dụ nhánh `main` tự động mapping với môi trường `Production`, `staging` vào `Staging` và các nhánh khác được chạy dưới phân vùng `Dev`.

## ⚙️ Phần 2: CI - Continuous Integration (Tích hợp liên tục - Build & Test)
*(Chỉ vào giao diện UI log Stage của Jenkins, khoanh vùng đoạn quá trình Kiểm thử)*
- **Checkout Code:** Trước tiên, Jenkins Agent chuyên trách có nhiệm vụ kéo bộ mã nguồn mới nhất từ repo về workspace.
- **Unit Test (Chiến lược Fail Fast):** Đây là màng chắn cốt lõi - Tự động phát hiện lỗi sớm bằng lệnh chạy Unit Test trên cả 3 microservices độc lập: `async-service`, `direct-service` và `kafka-service`. Chỉ cần có bất kì một test case nào test bị "Fail", pipeline sẽ dừng ngắt tức khắc. Lợi ích là bảo vệ mã nguồn "lỗi" rủi ro không bị lọt qua bước tiếp theo.
- **Code Analysis (SonarQube):** Pipeline có thể tích hợp SonarQube phân tích tĩnh code để đóng vai trò làm Quality Gate (chốt chặn chất lượng).
- **Build & Package:** Sau khi hệ thống thông báo mọi test vượt qua an toàn xanh lá. Code mới được bắt đầu biên dịch (assemble) ra các file cấu hình tối ưu JAR.

## 🐳 Phần 3: Cuộc cách mạng Containerization (Đóng gói Image)
*(Chỉ vào log việc Build lệnh Docker image trong Jenkins)*
- **Dockerizing:** Toàn vẹn hệ sinh thái gồm 4 thành phần (3 ứng dụng backend, 1 frontend React) sẽ đồng loạt được nhét vào 4 cái hộp chứa (Container). Điều này nhằm đảm bảo: *"Nếu bộ code đó đã chạy yên ổn được ở máy test, thì chắc chắn đi lên server sản xuất thiết lập cũng chạy đúng và hoàn toàn không lệch đi chút nào."*
- **Version Tagging Vững Chắc:** Không dùng "tag `latest`" lỏng lẻo, hệ thống Jenkins sẽ dựa vào số thứ tự chạy (Build Number) để cấp một định danh duy nhất (vd: bản Build `#14`).
- **Lưu trữ Image:** Mọi images sẽ được tự động Push xuất ra **Docker Hub** để tích trữ những mốc lịch sử không thể thay đổi. Kho lưu trữ này là xương sống cho khả năng cấp cứu phía sau.

## 🚀 Phần 4: CD - Continuous Deployment & Quản trị rủi ro Rollback
*(Mở file script `Jenkinsfile` hoặc hiển thị text Log quá trình docker-compose ở màn hình trình chiếu)*
Đây là phần giải quyết rủi ro thực tế khi vận hành:
- **Dynamic Configuration:** Pipeline tự detect để gán các file cấu hình ghi đè `docker-compose.override.(dev|staging).yml` tùy theo branch để chạy chung file gốc, áp dụng đúng cấu hình DataBase.
- **Zero-Downtime Update (Cập nhật không làm đứt quãng Client):** Bằng việc sử dụng tham số `docker-compose up -d --wait`. Container cũ sẽ không bị dỡ bỏ nếu container bản vá mới chưa hoàn toàn sáng đèn (healthy). Nhờ đó, người dùng đang xài tool stress-test không bị downtime quá lớn.
- **Auto-Rollback (Cứu Hộ Tự Động):** Đôi khi code vượt qua mọi bước Test nhưng deploy gặp lỗi biến môi trường server và bị crash. Tại khâu này nếu Jenkins deploy bị failed (bắt bằng try-catch), tự động nó sẽ lục lại cuốn ghi chép version `.env.last_stable` để tìm ra **phiên bản hoạt động không bị sao gần nhất** (VD: Bản Build `#13`). Và thực hiện thao tác Rollback tự kéo đắp lại bản cũ ngay lập tức. Khôi phục tự động trong chớp mắt bảo vệ hệ thống chạy thông suốt!

---

## 🎬 KỊCH BẢN LIVE DEMO TRÊN LỚP (Các bước thực diễn)

Để buổi biểu diễn thật trực quan và thuyết phục nhất, hãy thao tác chậm và giải thích theo các STEP sau:

### Kịch bản 1: Mở màn con đường hoàn mỹ (The Happy Path)
**Chuẩn bị:** Đang bật giao diện Web frontend stress-test bình thường. Trang chủ Jenkins sẵn sàng mở.
1. Mở trang web để khán giả thấy nó đang hoạt động bình thường, và ghi lại thời gian.
2. Em mở Code Editor, thực hiện thay đổi 1 chữ hoặc dòng màu sắc nhỏ trên UI bài Stress Test (ví dụ đổi title thẻ div h3).
3. Gõ trên terminal: Commit (`git commit -m "Update title UI"`) & Push code lên nhánh Github (`git push`).
4. Ngay lập tức mở UI Jenkins: *"Mọi người có thể thấy! Webhook Github đang báo về và một tiến trình Pipeline tự động xuất hiện đang chạy"*
5. Cả lớp cùng nhìn lướt các Stage xanh lá chạy. Giải thích ngắn gọn từng cái ngang qua (Test, Build, Push Docker...).
6. Lúc Pipeline báo Deploy thành công: Lặp tức F5 trang frontend. *"Và đây rồi! Giao diện mới thay đổi, và em chả cần gõ một dòng lệnh lặp lại nào trên Server".*

### Kịch bản 2: Bức tường ranh giới Unit Test ngăn chặn lỗi sớm
1. *"Chuyện gì xảy ra nếu lập trình viên lỡ tay viết sai logic và push lên môi trường ảo?"*
2. Nhảy vào một File Test (vd: `UserControllerTest.java`), chủ động làm sai mã xác nhận (vd: `assertEquals("Đúng", "Sai")`) và lưu file, đẩy code lên Git.
3. Chờ Jenkins kích hoạt. Lúc này ở bước **Unit Test**, nó sẽ Bị Dừng và hiển viền Đỏ (Failed).
4. Nhấn rà log test để cả lớp thấy báo cáo test failed. 
5. Cười và giải thích: *"Mọi sự triển khai dừng lại! Đoạn mã bị tiêu diệt ngay từ giai đoạn CI để đảm bảo code sinh lỗi không bao giờ tới tay được máy chủ chạy thực nghiệm. Không có bất cứ gián đoạn dịch vụ nào cả! Môi trường tự động ngăn chặn."*

### Kịch bản 3: Thao tác Rollback 
1. *"Nếu quá trình Test mượt mà, nhưng triển khai lên gặp vấn đề server rớt mạng/thiếu biến DB thì sao?"* (Mô phỏng trường hợp thủ công dùng rollback bằng phiên bản).
2. Chúng ta quay lại giao diện trang chủ Jenkins chọn chức năng Pipeline hiện diện => Click **"Build with Parameters"**.
3. *"Giả sử bản cập nhật mới nhất (số 30) đang bị lỗi khẩn cấp, thay vì lao vào check code gỡ bug trong áp lực lớn, em sẽ sửa biến `DEPLOY_VERSION` chỗ form input này. Thay vì chữ `latest`, em gõ trực tiếp phiên bản ổn định cách đó 1 giờ là `29`."*
4. Thực thi Build. Tiến trình bỏ qua hết công đoạn check, nhảy chớp nhoáng sang quá trình kéo Docker ID #29 đắp đè lại.
5. *"Chỉ mất vài chục giây hệ thống khôi phục hoàn nguyên. Thời gian còn lại team dev từ từ sửa bug và deploy bình thường."*

---

## 🏁 Tổng kết ngắn gọn (1 Phút)
Tóm tắt chốt lại, qua việc tích cực cài đặt tích hợp tự động hóa qua Pipeline CI/CD Jenkins, cấu trúc dự án này đáp ứng được các tiêu chuẩn Công nghiệp phần mềm chuyên nghiệp thời công nghệ Đám mây (DevOps):
- **Kiểm soát chất lượng bằng máy móc (Testing):** Hạn chế bug lên production.
- **Giảm thiểu Human Error tối đa:** Cắt bỏ 100% thời gian gõ lệnh rườm rà dễ nhầm lẫn. Cả nhóm team dev rảnh tay làm task khác.
- **Hệ thống An Toàn - Trọn đời Ổn Định (High Availability):** Cơ chế quản lý bản Build và tự động cấp cứu (Rollback) mang lại môi trường ổn định cao.

*(Xin cảm ơn thầy/cô và các bạn đã lắng nghe ạ!)*
