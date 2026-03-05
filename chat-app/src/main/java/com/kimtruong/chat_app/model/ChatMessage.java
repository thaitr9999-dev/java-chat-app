package com.kimtruong.chat_app.model;
/*  Khi browser gửi tin nhắn lên server, 
nó gửi dưới dạng JSON — ví dụ {"type":"CHAT","content":"Hello","sender":"Nam"}. 
Spring cần Jackson (thư viện đọc JSON) để chuyển chuỗi JSON đó thành object ChatMessage. 
Jackson làm việc này bằng cách: tạo object rỗng trước → rồi mới set từng field vào sau. 
Nếu không có constructor rỗng, bước "tạo object rỗng" này thất bại ngay.
Ghi nhớ công thức: POJO dùng để nhận JSON = luôn cần constructor rỗng + đủ getter/setter. */

public class ChatMessage {

    //Phân biệt 3 loại sự kiên messageType: CHAT, JOIN, LEAVE
    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }

    private MessageType type; // Loại tin nhắn
    private String content; // Nội dung tin nhắn
    private String sender; // Người gửi tin nhắn

    public ChatMessage() {
    }   

    public MessageType getType() {
        return type;
    }
    public String getContent() {
        return content;
    }   
    public String getSender() {
        return sender;
    }


    public void setType(MessageType type) {
        this.type = type;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }
    
}
