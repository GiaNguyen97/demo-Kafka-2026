package com.example.directservice.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("✅ [Direct] WS connected: " + session.getId());
        sendMessage("🔗 Connected to Direct Service (port 8081)");
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) { }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session);
        System.out.println("❌ [Direct] WS error: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        sessions.remove(session);
        System.out.println("🔌 [Direct] WS closed: " + session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public static void sendMessage(String msg) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized(session) {
                        session.sendMessage(new TextMessage(msg));
                    }
                } catch (IOException | IllegalStateException e) {
                    System.err.println("⚠️ WS send failed: " + e.getMessage());
                }
            }
        }
    }
}
