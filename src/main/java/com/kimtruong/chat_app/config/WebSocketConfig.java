package com.kimtruong.chat_app.config;

import com.kimtruong.chat_app.util.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket + STOMP cho ứng dụng.
 *
 * Tại sao dùng STOMP thay vì raw WebSocket?
 * Raw WebSocket chỉ là kênh truyền bytes — không có khái niệm destination hay routing.
 * STOMP thêm lớp protocol lên trên, cho phép subscribe topic, publish message — giống pub/sub.
 *
 * Luồng hoạt động:
 * Client connect → /ws → publish đến /app/... → controller xử lý → broker gửi đến /topic/...
 */

@Configuration 
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtUtil jwtUtil;
    private final JwtChannelInterceptor jwtChannelInterceptor;

    public WebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil ;
        this.jwtChannelInterceptor = new JwtChannelInterceptor(jwtUtil);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        
        // Broker nội bộ (in-memory) xử lý các topic mà server gửi xuống client
        // /topic  → broadcast (1 gửi, nhiều nhận) — dùng cho public chat
        // /queue  → private (1 gửi, 1 nhận) — chuẩn bị cho private chat ở Phase 2
        registry.enableSimpleBroker("/topic" , "/queue"); // Broker để gửi tin nhắn đến client

   // Prefix cho message từ client gửi lên server (controller)
        // Ví dụ: client publish đến /app/chat.sendMessage → ChatController xử lý
        registry.setApplicationDestinationPrefixes("/app"); // Prefix cho các destination từ client gửi lên
        registry.setUserDestinationPrefix("/user"); // ← Day 16 thêm dòng này
    }
    

   @Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket thuần (không dùng SockJS)
        // Client kết nối qua: ws://localhost:8080/ws
    registry.addEndpoint("/ws")
            .addInterceptors(new JwtHandshakeInterceptor(jwtUtil)) //Đăng kí Interceptor để kiểm tra JWT : không phải ai cũng vào đc phải đi qua JwtHandshakeInterceptor
            .setAllowedOriginPatterns("*");


    /*  .addInterceptors(new JwtHandshakeInterceptor(jwtUtil)) — đăng ký interceptor vào endpoint /ws. Có thể thêm nhiều interceptor nối tiếp nhau, chạy theo thứ tự được thêm vào.
        WebSocketMessageBrokerConfigurer — interface Spring yêu cầu implement để cấu hình WebSocket. registerStompEndpoints là nơi khai báo endpoint và interceptor. configureMessageBroker là nơi khai báo /topic, /queue, /app — đã có từ Day 5, không đổi.
        Inject JwtUtil vào WebSocketConfig qua constructor — vì JwtHandshakeInterceptor cần JwtUtil để verify token, nhưng interceptor không phải Spring bean nên không tự inject được, phải truyền thủ công qua new. 
        
        Luồng hoạt động khi client kết nối:

        Client gọi ws: //localhost 
        -> websoket nhaan connect request 
        -> chạy qua JwtHandshakeInterceptor (đăng kí ở registerStompEndpoints)
        -> nếu token hợp lệ -> lưu username vào attributes -> connect thanhg công ->Stomp sesion dduocj thiết lập 
        -> ngược lại không kết nối đc 
    
        WebSocketConfig là file cấu hình WebSocket duy nhất — đã có từ Day 5. Bước này chỉ thêm 1 dòng .addInterceptors() nhưng tác động lớn: toàn bộ WebSocket connection từ giờ đều phải có JWT hợp lệ.
        Kết hợp Day 14 + Day 15 — app đã có bảo vệ hoàn chỉnh cả 2 tầng: REST qua JwtFilter, WebSocket qua JwtHandshakeInterceptor.
        */



        /* Day 16 bước 1 ## Giải thích Bước 1 — `setUserDestinationPrefix("/user")`

        **🎯 Nó làm gì và như thế nào?**
        Bật tính năng **user destination** trong STOMP broker — cho phép server gửi tin nhắn đến **đúng 1 user cụ thể** thay vì broadcast cho tất cả. Khi thêm dòng này, Spring hiểu prefix `/user` có ý nghĩa đặc biệt: `/user/{username}/queue/private` chỉ deliver đến session của `username` đó.
        ---

        **1. Keyword lạ**

        `setUserDestinationPrefix("/user")` — khai báo prefix đặc biệt cho user destination. Spring dùng prefix này để map:

        ```
        server gửi:  convertAndSendToUser("kim", "/queue/private", msg)
                ↓ Spring tự động map thành
        broker gửi:  /user/kim/queue/private
                ↓ chỉ deliver đến session của "kim"
        ```

        `/queue` vs `/topic` — queue là point-to-point (1→1), topic là broadcast (1→nhiều). Private messaging dùng queue vì chỉ muốn 1 người nhận.

        ---

        **⚙️ Nó chạy như nào?**

        ```
        Không có setUserDestinationPrefix:
            server gửi /queue/private → broadcast tất cả đang subscribe → không private

        Có setUserDestinationPrefix("/user"):
            server gửi convertAndSendToUser("kim", "/queue/private", msg)
                ↓
            Spring map → /user/kim/queue/private
                ↓
            Broker tìm session nào của "kim" đang subscribe
                ↓
            Deliver đúng session đó → chỉ kim nhận được
        ```

        ---

        **🔥 So sánh với cách khác**

        Cách này — prefix `/user`, Spring tự handle routing đến đúng session. Đơn giản, không cần biết session ID.

        Cách khác: tự quản lý `Map<String, String> userSessions` — lưu `username → sessionId`, gửi thẳng theo sessionId. Hoạt động được nhưng phải tự handle khi user có nhiều tab mở (nhiều session), Spring đã làm điều này tự động với `setUserDestinationPrefix`. */

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors( jwtChannelInterceptor); // Đăng ký ChannelInterceptor để set Principal từ JWT
    }


}