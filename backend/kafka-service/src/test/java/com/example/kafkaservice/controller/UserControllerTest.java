package com.example.kafkaservice.controller;

import com.example.kafkaservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private KafkaListenerEndpointRegistry registry;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        registry = mock(KafkaListenerEndpointRegistry.class);
        userController = new UserController(userService, registry);
    }

    @Test
    void testRegister() {
        ResponseEntity<String> response = userController.register("testKafkaUser");
        verify(userService, times(1)).register("testKafkaUser");
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Submitted to Kafka: testKafkaUser", response.getBody());
    }

    @Test
    void testReplay() {
        ResponseEntity<String> response = userController.replay();
        verify(userService, times(1)).replay();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Replay triggered!", response.getBody());
    }

    @Test
    void testScale() {
        ResponseEntity<String> response = userController.scale(10);
        verify(userService, times(1)).scaleWorkers(10);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Scale successfully!", response.getBody());
    }

    @Test
    void testClear() {
        ResponseEntity<String> response = userController.clear();
        verify(userService, times(1)).clearQueue();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Cleared!", response.getBody());
    }
}
