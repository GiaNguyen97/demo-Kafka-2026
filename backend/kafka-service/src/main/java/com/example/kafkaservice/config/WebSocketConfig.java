package com.example.kafkaservice.config;

import com.example.kafkaservice.websocket.KafkaWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final KafkaWebSocketHandler kafkaWebSocketHandler;

    public WebSocketConfig(KafkaWebSocketHandler kafkaWebSocketHandler) {
        this.kafkaWebSocketHandler = kafkaWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Sử dụng setAllowedOriginPatterns để tuân thủ bảo mật và hỗ trợ linh hoạt hơn
        registry.addHandler(kafkaWebSocketHandler, "/ws").setAllowedOriginPatterns("*");
    }
}
