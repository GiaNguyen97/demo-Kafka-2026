package com.example.kafkaservice.controller;

import com.example.kafkaservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final KafkaListenerEndpointRegistry registry;

    public UserController(UserService userService, KafkaListenerEndpointRegistry registry) {
        this.userService = userService;
        this.registry = registry;
    }

    /**
     * POST /users/pause
     * Tạm dừng Consumer để demo Hàng chờ (Queue) tích tụ.
     */
    @PostMapping("/pause")
    public ResponseEntity<String> pause() {
        registry.getListenerContainers().forEach(container -> container.pause());
        return ResponseEntity.ok("All Kafka Consumers Paused");
    }

    /**
     * POST /users/resume
     * Tiếp tục xử lý các message đang chờ trong Queue.
     */
    @PostMapping("/resume")
    public ResponseEntity<String> resume() {
        registry.getListenerContainers().forEach(container -> container.resume());
        return ResponseEntity.ok("All Kafka Consumers Resumed");
    }

    /**
     * POST /users/register?username=abc
     * ✅ Return 200 OK NGAY sau khi publish Kafka (~10ms).
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String username) {
        userService.register(username);
        return ResponseEntity.ok("Submitted to Kafka: " + username);
    }

    /**
     * POST /users/replay
     * Tua lại toàn bộ lịch sử (Time Machine).
     */
    @PostMapping("/replay")
    public ResponseEntity<String> replay() {
        userService.replay();
        return ResponseEntity.ok("Replay triggered!");
    }

    /**
     * POST /users/scale?workers=10
     * TÍNH NĂNG VUA: Dynamic Scaling.
     */
    @PostMapping("/scale")
    public ResponseEntity<String> scale(@RequestParam int workers) {
        userService.scaleWorkers(workers);
        return ResponseEntity.ok("Scale successfully!");
    }

    /**
     * GET /users/lag?groupId=email-group
     */
    @GetMapping("/lag")
    public ResponseEntity<java.util.Map<String, Object>> getLag(@RequestParam String groupId) {
        return ResponseEntity.ok(userService.getLag(groupId));
    }

    /**
     * POST /users/clear
     * Dọn dẹp chiến trường.
     */
    @PostMapping("/clear")
    public ResponseEntity<String> clear() {
        userService.clearQueue();
        return ResponseEntity.ok("Cleared!");
    }
}
