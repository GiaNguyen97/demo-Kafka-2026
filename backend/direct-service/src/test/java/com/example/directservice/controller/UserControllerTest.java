package com.example.directservice.controller;

import com.example.directservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class UserControllerTest {

    private UserService userService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        userController = new UserController(userService);
    }

    @Test
    void testPause() {
        ResponseEntity<String> response = userController.pause();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Direct Service Paused", response.getBody());
    }

    @Test
    void testResume() {
        ResponseEntity<String> response = userController.resume();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Direct Service Resumed", response.getBody());
    }

    @Test
    void testRegister() throws InterruptedException {
        ResponseEntity<String> response = userController.register("testUser");
        verify(userService, times(1)).register("testUser");
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Registered: testUser", response.getBody());
    }
}
