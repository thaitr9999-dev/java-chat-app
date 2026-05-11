package com.kimtruong.chat_app.controller;

import com.kimtruong.chat_app.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class ReadStatusController {

    private final MessageService messageService;

    public ReadStatusController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        long count = messageService.getUnreadCount(username);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/mark-read")
    public ResponseEntity<Void> markAllAsRead() {
        messageService.markAllAsRead();
        return ResponseEntity.ok().build();
    }
}