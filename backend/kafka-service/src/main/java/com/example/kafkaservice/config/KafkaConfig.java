package com.example.kafkaservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String USER_TOPIC = "user-topic-turbo";

    @Bean
    public NewTopic userTopic() {
        return TopicBuilder.name(USER_TOPIC)
                .partitions(30)
                .replicas(1)
                .build();
    }
}
