package com.kimtruong.chat_app.filter;

import com.kimtruong.chat_app.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List; 



@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil; 

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        // Bỏ qua WebSocket — đã có JwtHandshakeInterceptor xử lý
        String path = request.getRequestURI();
        if (path.startsWith("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }
        // Bước 1: Đọc header Authorization
        String authHeader = request.getHeader("Authorization");

        // Bước 2: Không có header → cho đi tiếp
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bước 3: Tách token ra khỏi "Bearer "
        String token = authHeader.substring(7);

        // Bước 4: Kiểm tra token hợp lệ
        if (jwtUtil.isValid(token)) {
            String username = jwtUtil.extractUsername(token);

            // Bước 5: Set SecurityContext — Spring biết user là ai
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // Bước 6: Cho request đi tiếp dù hợp lệ hay không
        filterChain.doFilter(request, response);
/*  1. Keyword lạ
OncePerRequestFilter — Spring đảm bảo filter này chỉ chạy đúng 1 lần mỗi request, tránh bị gọi 2 lần do redirect nội bộ.
FilterChain — chuỗi các filter nối tiếp nhau. filterChain.doFilter() nghĩa là "tao xong rồi, mày tiếp đi" — chuyển request sang filter tiếp theo hoặc vào controller.
SecurityContextHolder — nơi Spring Security lưu thông tin "ai đang đăng nhập" trong request hiện tại. Set vào đây thì mọi chỗ trong app đều biết user là ai.
UsernamePasswordAuthenticationToken — object đại diện cho "user đã xác thực". Truyền username vào, Spring Security hiểu đây là user hợp lệ.
authHeader.substring(7) — cắt bỏ chữ "Bearer " (7 ký tự) để lấy token thuần.

Request đến
    ↓
JwtFilter chặn lại
    ↓
Có header "Authorization: Bearer xxx" không?
    ├── Không → cho đi tiếp (Spring Security sẽ chặn nếu endpoint cần auth)
    └── Có → isValid(token)?
              ├── Hợp lệ → set SecurityContext → cho đi tiếp → vào Controller
              └── Không hợp lệ → context rỗng → cho đi tiếp → Spring chặn 403

*/
    
    }
}
