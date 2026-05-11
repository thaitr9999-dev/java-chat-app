package com.kimtruong.chat_app.config;

import com.kimtruong.chat_app.util.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.List;


public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil ;
    
    public JwtChannelInterceptor(JwtUtil jwtUtil){
        this.jwtUtil = jwtUtil; 
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
         MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
         //Chỉ xưa lý khi cliner gửi Connect 
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("token");

            if(token !=null && jwtUtil.isValid(token)){
                String username = jwtUtil.extractUsername(token);
                UsernamePasswordAuthenticationToken auth =
                 new UsernamePasswordAuthenticationToken(username, null, List.of());
                 accessor.setUser(auth);
            }
        }
        return message;
    }

}

/* ## Giải thích Bước 4a — `JwtChannelInterceptor`

---
**🎯 Nó làm gì và như thế nào?**
Chặn STOMP CONNECT frame — frame đầu tiên client gửi khi kết nối WebSocket — đọc token từ STOMP header, verify bằng `JwtUtil`, rồi set `Principal` vào STOMP session. Từ đó mọi message sau đều có `Principal` đúng, `sendPrivateMessage()` mới lấy được username.
---

**1. Keyword lạ**

`ChannelInterceptor` — interface Spring WebSocket, chặn message trước/sau khi đi qua channel. `preSend()` chạy trước khi message được xử lý — đây là nơi mình inject Principal.

`StompHeaderAccessor` — đọc/ghi STOMP frame headers. STOMP frame có structure riêng khác HTTP header — phải dùng accessor này mới đọc được.

`StompCommand.CONNECT` — STOMP có nhiều loại frame: CONNECT, SEND, SUBSCRIBE, DISCONNECT. Mình chỉ xử lý CONNECT vì đó là lúc client vừa kết nối, cần set Principal một lần duy nhất.

`accessor.getFirstNativeHeader("token")` — đọc custom header `token` từ STOMP CONNECT frame. Client gửi token trong STOMP header, khác với query param lúc HTTP handshake.

`accessor.setUser(auth)` — gắn `Principal` vào STOMP session. Spring lưu lại suốt vòng đời connection — mọi message sau đều có thể lấy `Principal` từ đây.
---
**⚙️ Nó chạy như nào?**
```
Client kết nối WebSocket thành công
        ↓
Client gửi STOMP CONNECT frame:
    headers: { token: "eyJ..." }
        ↓
JwtChannelInterceptor.preSend() chặn lại
        ↓
Đây có phải CONNECT frame không?
    ├── Không → cho đi tiếp, không làm gì
    └── Có → đọc header "token"
              ↓
           isValid(token)?
              ├── Không → không set Principal → Principal = null
              └── Có → extractUsername() → setUser(auth)
                        ↓
                     Principal gắn vào session
                        ↓
                     sendPrivateMessage() gọi principal.getName() → đúng username
```

---
**🔥 So sánh với cách khác**
Cách này — set `Principal` tại STOMP CONNECT frame qua `ChannelInterceptor`. Principal tồn tại suốt session, mọi `@MessageMapping` method đều dùng được.
`JwtHandshakeInterceptor` ở Day 15 — verify token lúc HTTP handshake, lưu username vào session attributes. Hai cái làm việc khác nhau:
| | `JwtHandshakeInterceptor` | `JwtChannelInterceptor` |
|---|---|---|
| Chạy lúc | HTTP handshake | STOMP CONNECT frame |
| Lưu vào | session attributes | STOMP Principal |
| Dùng để | reject connection | inject Principal vào method |

Cần cả 2 — interceptor reject kẻ không có token, channel interceptor set danh tính cho những ai đã vào được. */
