# 🔥 Stress Test Demo – Direct vs Kafka

Demo so sánh trực tiếp **Direct Call** và **Kafka** bằng cùng 1 frontend.

## 📁 Cấu trúc

```
stress-test-demo/
├── frontend/          # FE dùng chung (index.html)
├── backend/
│   ├── direct-service/ # Port 8081 – Blocking
│   └── kafka-service/  # Port 8082 – Async Kafka
└── docker/
    └── docker-compose.yml
```

## 🚀 Cách chạy

### Bước 1 – Khởi động Kafka

```bash
cd docker
docker-compose up -d
```

> Kafka UI: http://localhost:9090

### Bước 2 – Chạy backend

```bash
# Terminal 1 – Direct Service (port 8081)
cd backend/direct-service
mvn spring-boot:run
```

```bash
# Terminal 2 – Kafka Service (port 8082)
cd backend/kafka-service
mvn spring-boot:run
```

### Bước 3 – Mở Frontend

```
Mở file: frontend/index.html
```

> Không cần web server, mở thẳng bằng browser

## 🧪 Cách test

### Test 1 – Direct (port 8081)

1. Chọn **⚡ Direct :8081** trong FE
2. Bấm **💣 Spam Requests** (50 req)
3. Quan sát: Response **chậm**, log ra **từng cái một**, cảm giác **"kẹt"**

### Test 2 – Kafka (port 8082)

1. Chọn **🚀 Kafka :8082** trong FE
2. Bấm **💣 Spam Requests** (50 req)
3. Quan sát: Response **ngay lập tức**, log **song song**, **Email** và **Log** chạy **cùng lúc**

## 📊 So sánh

| Tiêu chí | ⚡ Direct :8081 | 🚀 Kafka :8082 |
|---|---|---|
| Response time | ~3.5s / request | ~10ms |
| Spam 50 req | ❌ Lag, thread bị block | ✅ Return ngay |
| Email + Log | Tuần tự (3.5s) | Song song (async) |
| Scalable | ❌ Khó | ✅ Dễ scale |

## 🧠 Architecture

### Direct Flow
```
POST /register → UserService → sleep(500) → EmailService (2s) → LogService (1s) → return
                                            ↑ BLOCKING 3.5s
```

### Kafka Flow
```
POST /register → UserProducer.send("user-topic") → return 200 (~10ms)
                     ↓ async
              EmailConsumer (2s) ←→ LogConsumer (1s)  ← chạy SONG SONG
```

## 💡 Insight phỏng vấn

> "Em đã build 2 hệ thống Direct và Kafka, dùng cùng 1 frontend để stress test. Kafka xử lý tốt hơn do bất đồng bộ và event-driven architecture. Khi spam 50 request, Direct bị block ~175 giây, còn Kafka return trong vòng chưa đầy 1 giây."

## 🔧 Yêu cầu

- Java 17+
- Maven 3.8+
- Docker Desktop
