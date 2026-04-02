package com.example.kafkaservice.kafka.producer;

import com.example.kafkaservice.config.KafkaConfig;
import com.example.kafkaservice.websocket.KafkaWebSocketHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaWebSocketHandler wsHandler;

    public UserProducer(KafkaTemplate<String, String> kafkaTemplate, KafkaWebSocketHandler wsHandler) {
        this.kafkaTemplate = kafkaTemplate;
        this.wsHandler = wsHandler;
    }

    /**
     * ✅ NON-BLOCKING – gửi vào Kafka rồi return NGAY (~10ms).
     * SỬ DỤNG KEY (username) ĐỂ ĐẢM BẢO THỨ TỰ TRONG PARTITION.
     */
    public void publish(String username) {
        // Cách gửi: Topic, Key, Value -> Đảm bảo cùng user sẽ vào cùng partition -> Giữ thứ tự!
        kafkaTemplate.send(KafkaConfig.USER_TOPIC, username, username);
        wsHandler.sendMessage("🚀 [Kafka] Đã đẩy vào Broker: " + username + " (Key: " + username + ")");
        System.out.println("📤 [Kafka] Published: " + username);
    }
}
