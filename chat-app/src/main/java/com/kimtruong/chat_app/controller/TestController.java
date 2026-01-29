package com.kimtruong.chat_app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    
    @GetMapping("/test")
    public String test() {
        return """
            <h1>✅ Day 4: Project Setup COMPLETED!</h1>
            <h3>Dependencies Check:</h3>
            <ul>
                <li>Spring Web: ✅</li>
                <li>Spring WebSocket: ✅</li>
                <li>Java Version: %s</li>
            </ul>
            <p><strong>Next: Day 5 - WebSocket Configuration for Real-time Chat</strong></p>
            """.formatted(System.getProperty("java.version"));
    }
    
    @GetMapping("/check-websocket")
    public String checkWebSocket() {
        try {
            // Kiểm tra WebSocket class có tồn tại không
            Class.forName("org.springframework.web.socket.WebSocketHandler");
            return "{\"status\": \"SUCCESS\", \"message\": \"WebSocket dependency is loaded!\"}";
        } catch (ClassNotFoundException e) {
            return "{\"status\": \"ERROR\", \"message\": \"WebSocket NOT found! Check pom.xml\"}";
        }
    }
}