package com.example.kafkaservice.kafka.consumer;

import com.example.kafkaservice.config.KafkaConfig;
import com.example.kafkaservice.websocket.KafkaWebSocketHandler;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LogConsumer extends AbstractConsumerSeekAware {

    private final Map<TopicPartition, ConsumerSeekCallback> callbacks = new ConcurrentHashMap<>();
    private final KafkaWebSocketHandler wsHandler;

    public LogConsumer(KafkaWebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        assignments.keySet().forEach(tp -> callbacks.put(tp, callback));
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        partitions.forEach(callbacks::remove);
    }

    public void replay() {
        if (callbacks.isEmpty()) {
            System.out.println("⚠️ [LogConsumer] No active callbacks found for replay.");
            wsHandler.sendMessage("⚠️ [Log] Không tìm thấy luồng xử lý hoạt động để Replay!");
            return;
        }

        System.out.println("⏪ [LogConsumer] Manual Seek to beginning...");
        callbacks.forEach((tp, callback) -> {
            System.out.println("   -> Partition: " + tp.partition());
            callback.seekToBeginning(tp.topic(), tp.partition());
        });
        wsHandler.sendMessage("⏪ [Log] REPLAY DATA: Tua lại từ đầu Topic!");
    }

    @KafkaListener(id = "logListener", topics = KafkaConfig.USER_TOPIC, groupId = "log-group")
    public void handleLog(String username) throws InterruptedException {
        wsHandler.sendMessage("⏳ [DB] Đang xử lý: " + username);

        long delay = 300 + (long) (Math.random() * 1500);
        Thread.sleep(delay);

        wsHandler.sendMessage("💾 [DB] Đã lưu xong: " + username + " (delay " + delay + "ms)");
    }
}
