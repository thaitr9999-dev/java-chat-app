package com.kimtruong.chat_app.controller;

import com.kimtruong.chat_app.dto.AuthResponse;
import com.kimtruong.chat_app.dto.LoginRequest;
import com.kimtruong.chat_app.dto.RegisterRequest;
import com.kimtruong.chat_app.exception.TooManyRequestsException;
import com.kimtruong.chat_app.security.LoginRateLimiter;
import com.kimtruong.chat_app.service.AuditLogService;
import com.kimtruong.chat_app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController using pure Mockito (no Spring context).
 * Tests registration and login endpoints with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        authResponse = new AuthResponse("mock-jwt-token", "testuser");

        // Default mock behavior
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
    }

    // ========== Test 1: register_withValidData_returnsToken ==========

    @Test
    void register_withValidData_returnsToken() {
        // Arrange
        when(userService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mock-jwt-token", response.getBody().getToken());
        assertEquals("testuser", response.getBody().getUsername());
        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    // ========== Test 2: login_withValidCredentials_returnsToken ==========

    @Test
    void login_withValidCredentials_returnsToken() {
        // Arrange
        when(loginRateLimiter.buildKey(anyString(), anyString())).thenReturn("rate-limit-key");
        when(loginRateLimiter.tryConsume("rate-limit-key")).thenReturn(true);
        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.login(loginRequest, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mock-jwt-token", response.getBody().getToken());
        assertEquals("testuser", response.getBody().getUsername());
        verify(userService, times(1)).login(any(LoginRequest.class));
        verify(auditLogService, times(1)).log(eq("testuser"), eq("LOGIN_SUCCESS"), anyString(), anyString());
    }

    // ========== Test 3: login_withWrongPassword_returns401 ==========

    @Test
    void login_withWrongPassword_returns401() {
        // Arrange
        when(loginRateLimiter.buildKey(anyString(), anyString())).thenReturn("rate-limit-key");
        when(loginRateLimiter.tryConsume("rate-limit-key")).thenReturn(true);
        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai username hoặc password"));

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            authController.login(loginRequest, httpServletRequest);
        });

        verify(loginRateLimiter).consume("rate-limit-key");
        verify(auditLogService).log(eq("testuser"), eq("LOGIN_FAIL"), anyString(), anyString());
    }

    // ========== Test 4: login_whenBlocked_returns429 ==========

    @Test
    void login_whenBlocked_returns429() {
        // Arrange
        when(loginRateLimiter.buildKey(anyString(), anyString())).thenReturn("rate-limit-key");
        when(loginRateLimiter.tryConsume("rate-limit-key")).thenReturn(false);

        // Act & Assert
        assertThrows(TooManyRequestsException.class, () -> {
            authController.login(loginRequest, httpServletRequest);
        });

        verify(userService, never()).login(any());
    }

    // ========== Test 5: register_withDuplicateUsername_throws ==========

    @Test
    void register_withDuplicateUsername_throws() {
        // Arrange
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username đã tồn tại"));

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            authController.register(registerRequest);
        });

        verify(userService).register(any(RegisterRequest.class));
    }
}

