package com.example.asyncservice.service;

import com.example.asyncservice.websocket.WebSocketHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    /**
     * @Async → chay song song voi EmailService
     * Khac @Async voi Blocking: ca Email va Log deu duoc goi ngay,
     * chay DONG THOI tren 2 thread rieng biet
     */
    private volatile boolean isPaused = false;

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    @Async("asyncExecutor")

    public void saveLog(String username) {
        try {
            // KIỂM TRA TẠM DỪNG (DEMO)
            while (isPaused) {
                Thread.sleep(500);
            }

            WebSocketHandler.sendMessage("⏳ [DB] Đang xử lý: " + username);

            Thread.sleep(1000); // Simulate log write
            WebSocketHandler.sendMessage("💾 [DB] Đã lưu xong: " + username);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            WebSocketHandler.sendMessage("❌ [LỖI] Hệ thống dính Exception: " + username);
        }
    }
}
