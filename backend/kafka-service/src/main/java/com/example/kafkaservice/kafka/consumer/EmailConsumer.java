package com.example.kafkaservice.kafka.consumer;

import com.example.kafkaservice.config.KafkaConfig;
import com.example.kafkaservice.websocket.KafkaWebSocketHandler;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer extends AbstractConsumerSeekAware {

    private final KafkaWebSocketHandler wsHandler;
    private volatile boolean replayRequested = false;

    public EmailConsumer(KafkaWebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    /**
     * TÍNH NĂNG TIME MACHINE (REPLAY DATA).
     */
    public void replay() {
        System.out.println("⏪ [EmailConsumer] HTTP Thread received replay request, delegating to Kafka Thread...");
        // Đặt cờ cho Kafka Thread thực hiện thay vì gọi trực tiếp (Cách 2)
        this.replayRequested = true;
        wsHandler.sendMessage("⏪ [Email] Đã gửi tín hiệu REPLAY, chờ Kafka xử lý...");
    }

    @RetryableTopic(attempts = "3", numPartitions = "30")
    @KafkaListener(id = "emailListener", topics = KafkaConfig.USER_TOPIC, groupId = "email-group")
    public void handleEmail(String username) throws InterruptedException {
        // Cách 2: Đẩy lệnh seek về đúng Kafka thread (chỉ thread này mới an toàn)
        if (replayRequested) {
            System.out.println("⏪ [EmailConsumer] Kafka Thread is executing Seek to beginning...");
            // Gọi seek trên Kafka Thread. Hàm từ thư viện Spring Kafka `AbstractConsumerSeekAware`.
            this.seekToBeginning();
            this.replayRequested = false;
            wsHandler.sendMessage("⏪ [Email] REPLAY DATA: Tua lại từ đầu Topic thành công!");
            return;
        }

        wsHandler.sendMessage("⏳ [Email] Đang xử lý: " + username);

        if (username.contains("error")) {
            wsHandler.sendMessage("❌ [LỖI] Hệ thống dính Exception: " + username);
            throw new RuntimeException("Simulated error in EmailConsumer");
        }

        long delay = 500 + (long) (Math.random() * 2500);
        Thread.sleep(delay);

        wsHandler.sendMessage("✉️ [Email] Đã gửi xong: " + username + " (delay " + delay + "ms)");
        wsHandler.sendMessage("✅ [Thành công] Hoàn tất quy trình: " + username);
    }

    @DltHandler
    public void handleDlt(String username, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        wsHandler.sendMessage("💀 [DltHandler] Tin nhắn vĩnh viễn thất bại: " + username + " (đã chuyển vào DLT)");
    }
}
