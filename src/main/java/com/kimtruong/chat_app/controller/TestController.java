package com.kimtruong.chat_app.controller;

import com.kimtruong.chat_app.util.JwtUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final JwtUtil jwtUtil;

    public TestController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/test-jwt")
    public String testJwt() {
        String token = jwtUtil.generateToken("Kim");
        String username = jwtUtil.extractUsername(token);
        boolean valid = jwtUtil.isValid(token);

        return "Token: " + token +
               "<br><br>Username: " + username +
               "<br>Valid: " + valid;
    }
}