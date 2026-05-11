package com.kimtruong.chat_app.controller;

import com.kimtruong.chat_app.dto.AuthResponse;
import com.kimtruong.chat_app.dto.LoginRequest;
import com.kimtruong.chat_app.dto.RegisterRequest;
import com.kimtruong.chat_app.exception.TooManyRequestsException;
import com.kimtruong.chat_app.security.LoginRateLimiter;
import com.kimtruong.chat_app.service.AuditLogService;
import com.kimtruong.chat_app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final LoginRateLimiter loginRateLimiter;
    private final AuditLogService auditLogService;

    public AuthController(UserService userService, LoginRateLimiter loginRateLimiter, AuditLogService auditLogService) {
        this.userService = userService;
        this.loginRateLimiter = loginRateLimiter;
        this.auditLogService = auditLogService;
    }

    /**
     * Register a new user account.
     *
     * @param request the user registration request containing username and password
     * @return the authentication response with JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    /**
     * Authenticate a user and return a JWT token.
     * Implements rate limiting to prevent brute force attacks.
     *
     * @param request the login request containing username and password
     * @param servletRequest the HTTP request to extract client IP address
     * @return the authentication response with JWT token
     * @throws TooManyRequestsException if rate limit is exceeded
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {

        String ip = servletRequest.getRemoteAddr();
        String key = loginRateLimiter.buildKey(request.getUsername(), ip);

        if (!loginRateLimiter.tryConsume(key)) {
            throw new TooManyRequestsException("Too many login attempts. Try again later.");
        }

        try {
            AuthResponse response = userService.login(request);
            auditLogService.log(request.getUsername(), "LOGIN_SUCCESS", ip, "Login successful");
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            loginRateLimiter.consume(key);
            auditLogService.log(request.getUsername(), "LOGIN_FAIL", ip, ex.getMessage());
            throw ex;
        }
    }
}