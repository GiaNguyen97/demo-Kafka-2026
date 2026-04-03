package com.example.asyncservice.controller;

import com.example.asyncservice.service.EmailService;
import com.example.asyncservice.service.UserService;
import com.example.asyncservice.service.LogService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final LogService logService;

    public UserController(UserService userService, EmailService emailService, LogService logService) {
        this.userService = userService;
        this.emailService = emailService;
        this.logService = logService;
    }

    @PostMapping("/pause")
    public ResponseEntity<String> pause() {
        emailService.setPaused(true);
        logService.setPaused(true);
        com.example.asyncservice.websocket.WebSocketHandler
                .sendMessage("🚥 [Hệ thống] Đã TẠM DỪNG xử lý background workers (Email & Logs).");
        return ResponseEntity.ok("Async Service s");
    }

    @PostMapping("/resume")
    public ResponseEntity<String> resume() {
        emailService.setPaused(false);
        logService.setPaused(false);
        com.example.asyncservice.websocket.WebSocketHandler
                .sendMessage("🚦 [Hệ thống] Tiếp tục xử lý các background tasks.");
        return ResponseEntity.ok("Async Service Resumed");
    }

    /**
     * POST /users/register?username=abc
     * Return ~500ms (chi doi DB save, khong doi Email/Log)
     * Email va Log chay nen qua @Async thread pool
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String username) {
        userService.register(username);
        return ResponseEntity.ok("Async registered: " + username);
    }
}
