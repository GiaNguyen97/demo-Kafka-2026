package com.example.directservice.service;

import com.example.directservice.websocket.WebSocketHandler;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    /**
     * Giả lập gửi email – BLOCKING 2 giây
     */
    public void sendEmail(String username) {
        try {
            WebSocketHandler.sendMessage("⏳ [Email] Đang xử lý: " + username);
            Thread.sleep(2000);
            WebSocketHandler.sendMessage("✉️ [Email] Đã gửi xong: " + username);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            WebSocketHandler.sendMessage("❌ [LỖI] Hệ thống dính Exception: " + username);
        }
    }
}
