package com.kimtruong.chat_app.model;
 
import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private LocalDateTime createdAt;

    public User() {
    }

    //Getters and Setters
    public Long getId() {
        return id;
    }
    public String getUserName() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // 1.Enum ngay trong class
    public enum Role {USER , ADMIN}

    //2.Field 
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER; // Mặc định là USER

    // 3. Getter và Setter cho role
    public Role getRole() {
        return role;
    }
    public void setRole(Role role) {
        this.role = role;
    }
    
}