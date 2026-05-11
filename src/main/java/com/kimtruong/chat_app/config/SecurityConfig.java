package com.kimtruong.chat_app.config;

import com.kimtruong.chat_app.filter.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/messages/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*   
    addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) — chèn JwtFilter vào trước filter mặc định của Spring. Quan trọng vì nếu chèn sau thì Spring đã chặn request rồi, JwtFilter không có cơ hội chạy.
    anyRequest().authenticated() — thay vì permitAll, giờ mọi endpoint không được liệt kê rõ đều bắt buộc phải có auth. Đây là thay đổi lớn nhất của bước này.
    UsernamePasswordAuthenticationFilter — filter mặc định của Spring xử lý login form. Mình không dùng nó nhưng dùng nó làm mốc tham chiếu để chèn JwtFilter vào đúng chỗ.


    \Request đến
    ↓
JwtFilter  ← mình thêm vào đây
    ↓
UsernamePasswordAuthenticationFilter  ← mốc tham chiếu
    ↓
Spring Security check authorizeHttpRequests
    ├── /auth/**  → permitAll → đi thẳng vào controller
    ├── /admin/** → hasRole ADMIN → check role
    └── còn lại  → authenticated() → phải có SecurityContext
    */
}