package com.example.asyncservice.controller;

import com.example.asyncservice.service.EmailService;
import com.example.asyncservice.service.LogService;
import com.example.asyncservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private EmailService emailService;
    private LogService logService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        emailService = mock(EmailService.class);
        logService = mock(LogService.class);
        userController = new UserController(userService, emailService, logService);
    }

    @Test
    void testPause() {
        ResponseEntity<String> response = userController.pause();
        verify(emailService, times(1)).setPaused(true);
        verify(logService, times(1)).setPaused(true);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Async Service Paused", response.getBody());
    }

    @Test
    void testResume() {
        ResponseEntity<String> response = userController.resume();
        verify(emailService, times(1)).setPaused(false);
        verify(logService, times(1)).setPaused(false);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Async Service Resumed", response.getBody());
    }

    @Test
    void testRegister() {
        ResponseEntity<String> response = userController.register("testUser");
        verify(userService, times(1)).register("testUser");
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Async registered: testUser", response.getBody());
    }
}
