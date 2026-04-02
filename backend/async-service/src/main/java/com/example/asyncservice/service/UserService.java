package com.example.asyncservice.service;

import com.example.asyncservice.websocket.WebSocketHandler;
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
     * Luong xu ly cua Async Service:
     *
     * 1. Nhan request
     * 2. sleep(500ms) -> Simulate DB save (BLOCKING - vat truoc)
     * 3. Goi emailService.sendEmail() -> return NGAY, chay tren thread E
     * 4. Goi logService.saveLog()    -> return NGAY, chay tren thread L
     * 5. Return 200 OK (~500ms)
     *
     * => Thread E va Thread L chay SONG SONG ngoai background
     *
     * DIEM YEU so voi Kafka:
     * - Neu app crash o buoc 3/4 → Email/Log mat luon (khong luu lai)
     * - Thread pool co gioi han → Spam 500 req → co the bi RejectedExecutionException
     */
    public void register(String username) {
        WebSocketHandler.sendMessage("📥 [API] Nhận Request: " + username);

        // DB save (blocking nhu thuong)
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WebSocketHandler.sendMessage("💾 [DB] Đã lưu xong: " + username);

        // Fire and forget - 2 @Async chay song song, khong can doi
        emailService.sendEmail(username);
        logService.saveLog(username);

        // Return ngay, khong doi Email hay Log xong
        WebSocketHandler.sendMessage("✅ [Thành công] Hoàn tất quy trình: " + username);
    }
}
