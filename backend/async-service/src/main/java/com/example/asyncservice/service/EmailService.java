package com.example.asyncservice.service;

import com.example.asyncservice.websocket.WebSocketHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private volatile boolean isPaused = false;

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    /**
     * @Async → chay tren thread rieng, KHONG block request chinh
     * Nhung van la thread cua JVM → khi restart app thi job dang chay bi mat!
     * (Day la diem yeu cua @Async so voi Kafka)
     */
    @Async("asyncExecutor")
    public void sendEmail(String username) {
        try {
            // KIỂM TRA TẠM DỪNG (DEMO)
            while (isPaused) {
                Thread.sleep(500);
            }

            // GIẢ LẬP LỖI NGẪU NHIÊN (DEMO)
            if (Math.random() < 0.2) {
                throw new RuntimeException("LỖI KẾT NỐI SERVER EMAIL");
            }
            
            WebSocketHandler.sendMessage("⏳ [Email] Đang xử lý: " + username);
            Thread.sleep(2000); // Simulate slow email
            WebSocketHandler.sendMessage("✉️ [Email] Đã gửi xong: " + username);
        } catch (Exception e) {
            WebSocketHandler.sendMessage("❌ [LỖI @Async] Mất Job - Không thể gửi email cho: " + username + " (" + e.getMessage() + ")");
        }
    }
}
