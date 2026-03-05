package com.kimtruong.chat_app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration 
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix cho cacs topic ma Sever sẽ gửi tin nhắn đến client
        // topic -> broadcat (1 nguoi gui -> nhieu nguoi nhan) topic tât cả mọi người đều nhận được tin nhắn 
        // queue -> private (1 nguoi gui -> 1 nguoi nhan) - queue chỉ có người nhận mới nhận được tin nhắn
        registry.enableSimpleBroker("/topic" , "/queue"); // Broker để gửi tin nhắn đến client

        // Prefix cho cac message mà client gửi lên server
        registry.setApplicationDestinationPrefixes("/app"); // Prefix cho các destination từ client gửi lên
    }

   @Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*");
    // BỎ .withSockJS() — dùng WebSocket thuần
}
}