package com.kimtruong.chat_app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity JPA — ánh xạ tới bảng "messages" trong database.
 *
 * Phân biệt vai trò:
 * - ChatMessage: DTO, dùng để truyền dữ liệu qua WebSocket (không lưu DB)
 * - Message:     Entity, dùng để lưu vào DB (không gửi qua WebSocket trực tiếp)
 *
 * Hai phương thức static/instance đảm nhận việc chuyển đổi giữa hai lớp:
 * - Message.from(ChatMessage)  :chuyển DTO thành Entity để lưu
 * - message.toChatMessage()    :chuyển Entity thành DTO để gửi client
 */

@Entity
@Table(name = "messages")
public class Message {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    
    private Long id ; 
    
    @Enumerated(EnumType.STRING) // Lưu "CHAT"/"JOIN"/"LEAVE" thay vì số 0/1/2
    private ChatMessage.MessageType type ;

    @Column(length = 500)
    private String content ;
    private String sender ; 
    private LocalDateTime timestamp ;

    @Column(name = "is_read")
    private boolean isRead = false; 

    public Message() {
    
    }

   /** Chuyển ChatMessage (DTO) → Message (Entity) để lưu vào DB */
       public static Message from(ChatMessage chatMessage){
        Message msg = new Message( ) ;
        msg.type = chatMessage.getType() ; 
        msg.content = chatMessage.getContent() ;
        msg.sender = chatMessage.getSender() ;
        msg.timestamp = LocalDateTime.now() ;
        msg.isRead = false; // Default to unread
        return msg ;
    }

    /** Chuyển Message (Entity) → ChatMessage (DTO) để gửi lên client */
    public ChatMessage toChatMessage(){
        ChatMessage msg = new ChatMessage();
        msg.setType(this.type);
        msg.setContent(this.content);
        msg.setSender(this.sender);
        return msg ;

    }

    //Getters 
    public Long getId() {
        return id;
    }
    public ChatMessage.MessageType getType() {
        return type;
    }
    public String getContent() {
        return content;
    }   
    public String getSender() {
        return sender;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    // Setters for testing
    public void setType(ChatMessage.MessageType type) {
        this.type = type;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
