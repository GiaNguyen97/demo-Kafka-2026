package com.example.directservice.service;

import com.example.directservice.websocket.WebSocketHandler;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final EmailService emailService;
    private final LogService logService;

    public UserService(EmailService emailService, LogService logService) {
        this.emailService = emailService;
        this.logService = logService;
    }

    /**
     * ❌ BLOCKING – xử lý tuần tự, mỗi bước phải đợi bước trước
     *
     * Flow:
     *   sleep(500ms)  → Save DB
     *   sleep(2000ms) → Send Email
     *   sleep(1000ms) → Save Log
     * Tổng: ~3.5s / request → 50 request = UI bị kẹt!
     */
    public void register(String username) {
        System.out.println("▶ [Direct] Register: " + username);
        WebSocketHandler.sendMessage("📥 [API] Nhận Request: " + username);

        // Simulate DB save
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WebSocketHandler.sendMessage("💾 [DB] Đã lưu xong: " + username);

        // Blocking email (2s)
        emailService.sendEmail(username);

        // Blocking log (1s)
        logService.saveLog(username);

        WebSocketHandler.sendMessage("✅ [Thành công] Hoàn tất quy trình: " + username);
    }
}
