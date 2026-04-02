package com.example.kafkaservice.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KafkaWebSocketHandler implements WebSocketHandler {

    // Lưu ý: Sử dụng Set không static để quản lý session trong nội tại Bean này.
    // Nếu scale nhiều instance xử lý, cần sử dụng Redis Pub/Sub hoặc Kafka để đồng bộ tin nhắn WS.
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("✅ [Kafka] WS connected: " + session.getId());
        sendMessage("🔗 Connected to Kafka Service (port 8082)");
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        // Không xử lý tin nhắn đến trong bản demo này
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        sessions.remove(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Gửi tin nhắn tới toàn bộ client đang kết nối.
     */
    public void sendMessage(String msg) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(msg));
                    }
                } catch (IOException | IllegalStateException e) {
                    System.err.println("⚠️ WS send failed: " + e.getMessage());
                }
            }
        }
    }
}
