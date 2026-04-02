package com.example.kafkaservice.service;

import com.example.kafkaservice.config.KafkaConfig;
import com.example.kafkaservice.kafka.consumer.EmailConsumer;
import com.example.kafkaservice.kafka.consumer.LogConsumer;
import com.example.kafkaservice.kafka.producer.UserProducer;
import com.example.kafkaservice.websocket.KafkaWebSocketHandler;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserProducer userProducer;
    private final EmailConsumer emailConsumer;
    private final LogConsumer logConsumer;
    private final KafkaListenerEndpointRegistry registry;
    private final KafkaAdmin kafkaAdmin;
    private final KafkaWebSocketHandler wsHandler;

    public UserService(UserProducer userProducer, EmailConsumer emailConsumer, LogConsumer logConsumer,
                       KafkaListenerEndpointRegistry registry, KafkaAdmin kafkaAdmin, KafkaWebSocketHandler wsHandler) {
        this.userProducer = userProducer;
        this.emailConsumer = emailConsumer;
        this.logConsumer = logConsumer;
        this.registry = registry;
        this.kafkaAdmin = kafkaAdmin;
        this.wsHandler = wsHandler;
    }

    /**
     * DYNAMIC PURGE: Xóa sạch Topic và tạo lại để dọn dẹp hàng chờ.
     */
    public void clearQueue() {
        String topic = KafkaConfig.USER_TOPIC;
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // 1. Xóa Topic
            adminClient.deleteTopics(Collections.singletonList(topic)).all().get();
            System.out.println("🗑️ [Kafka] Deleted topic: " + topic);

            // 2. Chờ một chút để Broker hoàn tất việc xóa (Kafka xóa async trên Broker)
            // Lưu ý: Trong thực tế nên polling kiểm tra trạng thái Topic thay vì sleep cứng 2s.
            Thread.sleep(2000);

            // 3. Tạo lại Topic với 50 partitions
            NewTopic newTopic = new NewTopic(topic, 50, (short) 1);
            adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
            System.out.println("✨ [Kafka] Re-created topic: " + topic + " with 50 partitions");

            wsHandler.sendMessage("🗑️ [KAFKA] ĐÃ XÓA SẠCH VÀ LÀM MỚI HÀNG CHỜ! (50 Partitions)");
        } catch (Exception e) {
            System.err.println("Error clearing Kafka queue: " + e.getMessage());
            wsHandler.sendMessage("⚠️ [KAFKA] Hàng chờ đang được làm mới, vui lòng chờ 2s và thử lại.");
        }
    }

    /**
     * Lấy Kafka Lag thực tế từ Broker (Chi tiết đến từng Partition).
     */
    public Map<String, Object> getLag(String groupId) {
        String topic = KafkaConfig.USER_TOPIC;
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // 1. Lấy thông tin tất cả các Partitions của Topic
            TopicDescription description;
            try {
                description = adminClient.describeTopics(Collections.singletonList(topic)).allTopicNames().get().get(topic);
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof UnknownTopicOrPartitionException) {
                    return Collections.singletonMap("total", 0L); // Topic chưa tồn tại
                }
                throw ee;
            }
            if (description == null) return Collections.singletonMap("total", 0L);

            List<TopicPartition> allPartitions = description.partitions().stream()
                    .map(p -> new TopicPartition(topic, p.partition()))
                    .collect(Collectors.toList());

            // 2. Lấy committed offsets của group
            Map<TopicPartition, OffsetAndMetadata> committedOffsets =
                    adminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();

            // 3. Lấy log end offsets (mới nhất) cho TẤT CẢ partitions
            Map<TopicPartition, OffsetSpec> requestLatestOffsets = new HashMap<>();
            allPartitions.forEach(tp -> requestLatestOffsets.put(tp, OffsetSpec.latest()));

            Map<TopicPartition, ListOffsetsResultInfo> latestOffsets =
                    adminClient.listOffsets(requestLatestOffsets).all().get();

            // 4. Tính toán lag từng partition và tổng lag
            long totalLag = 0;
            Map<Integer, Long> partitionLags = new HashMap<>();
            
            for (TopicPartition tp : allPartitions) {
                long committed = committedOffsets.containsKey(tp) ? committedOffsets.get(tp).offset() : 0;
                long latest = latestOffsets.containsKey(tp) ? latestOffsets.get(tp).offset() : 0;
                long lag = Math.max(0, latest - committed);
                totalLag += lag;
                if (lag > 0) {
                    partitionLags.put(tp.partition(), lag);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", totalLag);
            result.put("partitions", partitionLags);
            return result;
        } catch (Exception e) {
            System.err.println("Error fetching Kafka lag for " + groupId + ": " + e.getMessage());
            return Collections.singletonMap("total", 0L);
        }
    }

    /**
     * DYNAMIC SCALING: Chỉnh số lượng luồng Consumer Runtime.
     */
    public void scaleWorkers(int workers) {
        MessageListenerContainer container = registry.getListenerContainer("emailListener");
        if (container != null) {
            container.stop();
            if (container instanceof ConcurrentMessageListenerContainer) {
                ((ConcurrentMessageListenerContainer<?, ?>) container).setConcurrency(workers);
            }
            container.start();
            System.out.println("✅ [Kafka] Scaled EmailConsumer to " + workers + " threads!");
            wsHandler.sendMessage("⚡ [DIỆU KỲ] Đã Scale Kafka EmailConsumer lên " + workers + " Công Nhân!");
        }
    }

    /**
     * TÍNH NĂNG REPLAY DATA.
     */
    public void replay() {
        System.out.println("▶ [Kafka] Replay Data Triggered!");
        emailConsumer.replay();
        logConsumer.replay();
    }

    /**
     * ✅ NON-BLOCKING – publish Kafka rồi return NGAY.
     * Email + Log consumers chạy bất đồng bộ, song song.
     */
    public void register(String username) {
        System.out.println("▶ [Kafka] Register: " + username);

        // Simulate Delay (Eventual Consistency test)
        if (username.contains("delay")) {
            try {
                Thread.sleep(1500); // 1.5s delay (gây ngẽn API cố tình)
                wsHandler.sendMessage("⏳ [Kafka] Bị delay cố ý đoạn API: " + username);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        wsHandler.sendMessage("📥 [API] Nhận Request: " + username);
        userProducer.publish(username);
    }
}
