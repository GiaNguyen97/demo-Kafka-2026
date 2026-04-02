package com.example.directservice.service;

import com.example.directservice.websocket.WebSocketHandler;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    /**
     * Giả lập ghi log – BLOCKING 1 giây
     */
    public void saveLog(String username) {
        try {
            WebSocketHandler.sendMessage("⏳ [DB] Đang xử lý: " + username);
            Thread.sleep(1000);
            WebSocketHandler.sendMessage("💾 [DB] Đã lưu xong: " + username);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            WebSocketHandler.sendMessage("❌ [LỖI] Hệ thống dính Exception: " + username);
        }
    }
}
