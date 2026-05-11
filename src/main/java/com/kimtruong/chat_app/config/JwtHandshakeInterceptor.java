package com.kimtruong.chat_app.config;

import com.kimtruong.chat_app.util.JwtUtil;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        // Đọc token từ query param: /ws?token=eyJ...
        String query = request.getURI().getQuery();

        if (query != null && query.startsWith("token=")) {
            String token = query.substring(6); // cắt "token="

            if (jwtUtil.isValid(token)) {
                String username = jwtUtil.extractUsername(token);
                attributes.put("username", username); // lưu vào session
                return true; // cho connect
            }
        }

        return false; // reject connection
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // không cần làm gì sau handshake
    }


    /*  Client gọi ws://localhost:8080/ws?token=eyJ...
                ↓
        JwtHandshakeInterceptor.beforeHandshake()
                ↓
        Có query param "token=" không?
            ├── Không → return false → connection bị reject 403
            └── Có → isValid(token)?
                    ├── Không hợp lệ → return false → reject
                    └── Hợp lệ → lưu username vào attributes
                                → return true → connection được thiết lập
                                → vào chat bình thường
        
                                
        HandshakeInterceptor — interface của Spring WebSocket, có 2 method bắt buộc: beforeHandshake (trước khi kết nối) và afterHandshake (sau khi kết nối). Mình chỉ cần beforeHandshake.
        
        beforeHandshake trả về boolean — true nghĩa là "cho connect", false nghĩa là "reject ngay". Đây là điểm khác biệt với JwtFilter — filter chỉ set context, còn interceptor này có thể chặn hoàn toàn.
        
        request.getURI().getQuery() — đọc phần query string của URL. Ví dụ URL ws://localhost:8080/ws?token=eyJ... thì getQuery() trả về chuỗi "token=eyJ...".
        
        attributes — Map lưu dữ liệu gắn với WebSocket session. Put username vào đây thì WebSocketEventListener đọc được sau này qua headerAccessor.getSessionAttributes().get("username").
        
        query.substring(6) — cắt bỏ 6 ký tự "token=" để lấy token thuần.                    
    */
}