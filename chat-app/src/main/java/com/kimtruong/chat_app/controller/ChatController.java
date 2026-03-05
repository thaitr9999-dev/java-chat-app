package com.kimtruong.chat_app.controller;

import com.kimtruong.chat_app.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;

    // Spring tự inject vào qua constructor
    public ChatController(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage message) {
        // Broadcast thủ công thay vì @SendTo
        messagingTemplate.convertAndSend("/topic/public", message);
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage message) {
        message.setType(ChatMessage.MessageType.JOIN);
        messagingTemplate.convertAndSend("/topic/public", message);
    }
}