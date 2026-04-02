package com.example.directservice.controller;

import com.example.directservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private volatile boolean isPaused = false;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/pause")
    public ResponseEntity<String> pause() {
        this.isPaused = true;
        return ResponseEntity.ok("Direct Service Paused");
    }

    @PostMapping("/resume")
    public ResponseEntity<String> resume() {
        this.isPaused = false;
        return ResponseEntity.ok("Direct Service Resumed");
    }

    /**
     * POST /users/register?username=abc
     * ⚠️ BLOCKING ~3.5s – thread bị chiếm
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String username) throws InterruptedException {
        // GIẢ LẬP TREO LUỒNG (DEMO)
        while (isPaused) {
            Thread.sleep(500);
        }
        
        userService.register(username);
        return ResponseEntity.ok("Registered: " + username);
    }
}
