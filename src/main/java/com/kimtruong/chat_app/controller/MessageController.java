package com.kimtruong.chat_app.controller;

import com.kimtruong.chat_app.model.ChatMessage;
import com.kimtruong.chat_app.service.MessageService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;


@RestController
@RequestMapping("/api")
public class MessageController {
    
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/messages")
    public  List<ChatMessage> getLast50() {
        return messageService.getLast50();
    }
    
}
