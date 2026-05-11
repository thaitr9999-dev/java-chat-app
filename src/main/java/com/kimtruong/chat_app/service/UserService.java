package com.kimtruong.chat_app.service;

import com.kimtruong.chat_app.dto.AuthResponse;
import com.kimtruong.chat_app.dto.LoginRequest;
import com.kimtruong.chat_app.dto.RegisterRequest;
import com.kimtruong.chat_app.model.User;
import com.kimtruong.chat_app.repository.UserRepository;
import com.kimtruong.chat_app.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

   public UserService(UserRepository userRepository, JwtUtil jwtUtil, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user account with the provided username and password.
     * Validates that the username is unique before creating the user.
     *
     * @param request the registration request containing username and password
     * @return an AuthResponse containing the JWT token and username
     * @throws ResponseStatusException with HTTP 409 if username already exists
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username đã tồn tại");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(User.Role.USER);//day13
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUserName());
        return new AuthResponse(token, user.getUserName());
    }

    /**
     * Authenticate a user with the provided username and password.
     * Validates credentials against the database and returns a JWT token if successful.
     *
     * @param request the login request containing username and password
     * @return an AuthResponse containing the JWT token and username
     * @throws ResponseStatusException with HTTP 401 if credentials are invalid
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai username hoặc password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai username hoặc password");
        }

        String token = jwtUtil.generateToken(user.getUserName());
        return new AuthResponse(token, user.getUserName());
    }
}