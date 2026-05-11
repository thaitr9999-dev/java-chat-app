package com.kimtruong.chat_app.service;

import java.util.stream.Collectors; // thêm dòng này vào đầu file
import com.kimtruong.chat_app.model.ChatMessage;
import com.kimtruong.chat_app.model.Message;
import com.kimtruong.chat_app.repository.MessageRepository;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service xử lý logic nghiệp vụ liên quan đến tin nhắn.
 *
 * Tại sao cần Service thay vì gọi Repository thẳng từ Controller?
 * Service là nơi chứa business logic — ví dụ: chỉ lưu CHAT, bỏ qua JOIN/LEAVE.
 * Controller chỉ nên xử lý routing; Repository chỉ nên xử lý DB.
 */

@Service
public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Save a chat message to the database.
     * Only CHAT type messages are persisted; JOIN and LEAVE events are temporary.
     *
     * @param chatMessage the chat message to save
     */
    public void save(ChatMessage chatMessage) {
        if (chatMessage.getType() == ChatMessage.MessageType.CHAT) {
            messageRepository.save(Message.from(chatMessage));
        }
    }

    /**
     * Retrieve the last 50 messages from the database.
     * Converts Message entities to ChatMessage DTOs.
     *
     * @return list of the last 50 messages in ascending order by timestamp
     */
    public List<ChatMessage> getLast50() {
        return messageRepository.findTop50ByOrderByTimestampAsc()
                .stream()
                .map(Message::toChatMessage)
                .collect(Collectors.toList());
    }

    /**
     * Mark all unread messages as read.
     */
    public void markAllAsRead() {
        List<Message> unreadMessages = messageRepository.findByIsReadFalse();
        unreadMessages.forEach(msg -> msg.setRead(true));
        messageRepository.saveAll(unreadMessages);
    }

    /**
     * Get the count of unread messages for a specific user.
     *
     * @param username the username to count unread messages for
     * @return the number of unread messages not from the specified user
     */
    public long getUnreadCount(String username) {
        return messageRepository.countByIsReadFalseAndSenderNot(username);
    }
}