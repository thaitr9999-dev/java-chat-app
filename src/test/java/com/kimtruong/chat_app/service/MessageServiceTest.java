package com.kimtruong.chat_app.service;

import com.kimtruong.chat_app.model.ChatMessage;
import com.kimtruong.chat_app.model.Message;
import com.kimtruong.chat_app.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageService class.
 * Uses Mockito to mock MessageRepository.
 * Tests business logic: only CHAT messages are persisted, JOIN/LEAVE are ignored.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageService messageService;

    private ChatMessage chatMessage;
    private ChatMessage joinMessage;
    private ChatMessage leaveMessage;

    @BeforeEach
    void setUp() {
        // Setup test data
        chatMessage = new ChatMessage();
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        chatMessage.setContent("Hello, World!");
        chatMessage.setSender("alice");

        joinMessage = new ChatMessage();
        joinMessage.setType(ChatMessage.MessageType.JOIN);
        joinMessage.setContent("");
        joinMessage.setSender("bob");

        leaveMessage = new ChatMessage();
        leaveMessage.setType(ChatMessage.MessageType.LEAVE);
        leaveMessage.setContent("");
        leaveMessage.setSender("charlie");
    }

    // ========== save() Tests ==========

    @Test
    void save_withChatTypeMessage_callsRepositorySave() {
        // Arrange
        when(messageRepository.save(any(Message.class))).thenReturn(new Message());

        // Act
        messageService.save(chatMessage);

        // Assert
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void save_withChatTypeMessage_savesCorrectData() {
        // Arrange
        Message expectedMessage = Message.from(chatMessage);
        when(messageRepository.save(any(Message.class))).thenReturn(expectedMessage);

        // Act
        messageService.save(chatMessage);

        // Assert
        verify(messageRepository).save(argThat(msg ->
                msg.getType() == ChatMessage.MessageType.CHAT &&
                msg.getContent().equals("Hello, World!") &&
                msg.getSender().equals("alice")
        ));
    }

    @Test
    void save_withJoinTypeMessage_doesNotCallRepositorySave() {
        // Arrange & Act
        messageService.save(joinMessage);

        // Assert
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void save_withLeaveTypeMessage_doesNotCallRepositorySave() {
        // Arrange & Act
        messageService.save(leaveMessage);

        // Assert
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void save_withMultipleChatMessages_savesAllOfThem() {
        // Arrange
        ChatMessage msg1 = createChatMessage("user1", "Message 1");
        ChatMessage msg2 = createChatMessage("user2", "Message 2");
        ChatMessage msg3 = createChatMessage("user3", "Message 3");

        when(messageRepository.save(any(Message.class))).thenReturn(new Message());

        // Act
        messageService.save(msg1);
        messageService.save(msg2);
        messageService.save(msg3);

        // Assert
        verify(messageRepository, times(3)).save(any(Message.class));
    }

    @Test
    void save_withMixedMessageTypes_savesOnlyChat() {
        // Arrange
        ChatMessage chatMsg = createChatMessage("alice", "Hello");
        ChatMessage joinMsg = createJoinMessage("bob");
        ChatMessage leaveMsg = createLeaveMessage("charlie");

        when(messageRepository.save(any(Message.class))).thenReturn(new Message());

        // Act
        messageService.save(chatMsg);
        messageService.save(joinMsg);
        messageService.save(leaveMsg);
        messageService.save(chatMsg); // Another chat message

        // Assert
        verify(messageRepository, times(2)).save(any(Message.class));
    }

    // ========== getLast50() Tests ==========

    @Test
    void getLast50_whenRepositoryReturnsMessages_returnsMappedChatMessages() {
        // Arrange
        List<Message> mockMessages = Arrays.asList(
                createMessageEntity("alice", "Message 1"),
                createMessageEntity("bob", "Message 2"),
                createMessageEntity("charlie", "Message 3")
        );
        when(messageRepository.findTop50ByOrderByTimestampAsc()).thenReturn(mockMessages);

        // Act
        List<ChatMessage> result = messageService.getLast50();

        // Assert
        assertEquals(3, result.size());
        assertEquals("alice", result.get(0).getSender());
        assertEquals("Message 1", result.get(0).getContent());
        assertEquals("bob", result.get(1).getSender());
        assertEquals("Message 2", result.get(1).getContent());
    }

    @Test
    void getLast50_whenRepositoryReturnsEmptyList_returnsEmptyList() {
        // Arrange
        when(messageRepository.findTop50ByOrderByTimestampAsc()).thenReturn(Arrays.asList());

        // Act
        List<ChatMessage> result = messageService.getLast50();

        // Assert
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void getLast50_callsRepositoryFindTop50ByOrderByTimestampAsc() {
        // Arrange
        when(messageRepository.findTop50ByOrderByTimestampAsc()).thenReturn(Arrays.asList());

        // Act
        messageService.getLast50();

        // Assert
        verify(messageRepository, times(1)).findTop50ByOrderByTimestampAsc();
    }

    @Test
    void getLast50_returnsMessagesMappedFromEntityToDTO() {
        // Arrange
        Message entity = createMessageEntity("testuser", "Test content");
        entity.setType(ChatMessage.MessageType.CHAT);
        when(messageRepository.findTop50ByOrderByTimestampAsc()).thenReturn(Arrays.asList(entity));

        // Act
        List<ChatMessage> result = messageService.getLast50();

        // Assert
        assertEquals(1, result.size());
        ChatMessage dto = result.get(0);
        assertEquals("testuser", dto.getSender());
        assertEquals("Test content", dto.getContent());
        assertEquals(ChatMessage.MessageType.CHAT, dto.getType());
    }

    @Test
    void getLast50_with50Messages_returnsAll50() {
        // Arrange
        List<Message> messages = new java.util.ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            messages.add(createMessageEntity("user" + i, "Message " + i));
        }
        when(messageRepository.findTop50ByOrderByTimestampAsc()).thenReturn(messages);

        // Act
        List<ChatMessage> result = messageService.getLast50();

        // Assert
        assertEquals(50, result.size());
    }

    // ========== markAllAsRead() Tests ==========

    @Test
    void markAllAsRead_fetchesUnreadMessages_marksThemAsRead() {
        // Arrange
        Message msg1 = createMessageEntity("alice", "Message 1");
        msg1.setRead(false);
        Message msg2 = createMessageEntity("bob", "Message 2");
        msg2.setRead(false);

        when(messageRepository.findByIsReadFalse()).thenReturn(Arrays.asList(msg1, msg2));

        // Act
        messageService.markAllAsRead();

        // Assert
        verify(messageRepository).findByIsReadFalse();
        verify(messageRepository).saveAll(any());
    }

    @Test
    void markAllAsRead_whenNoUnreadMessages_doesNothing() {
        // Arrange
        when(messageRepository.findByIsReadFalse()).thenReturn(Arrays.asList());

        // Act
        messageService.markAllAsRead();

        // Assert
        verify(messageRepository).findByIsReadFalse();
        verify(messageRepository).saveAll(any());
    }

    // ========== getUnreadCount() Tests ==========

    @Test
    void getUnreadCount_returnsCorrectCount() {
        // Arrange
        String username = "alice";
        long expectedCount = 5L;
        when(messageRepository.countByIsReadFalseAndSenderNot(username)).thenReturn(expectedCount);

        // Act
        long result = messageService.getUnreadCount(username);

        // Assert
        assertEquals(expectedCount, result);
        verify(messageRepository).countByIsReadFalseAndSenderNot(username);
    }

    @Test
    void getUnreadCount_withZeroUnreadMessages_returnsZero() {
        // Arrange
        String username = "bob";
        when(messageRepository.countByIsReadFalseAndSenderNot(username)).thenReturn(0L);

        // Act
        long result = messageService.getUnreadCount(username);

        // Assert
        assertEquals(0, result);
    }

    @Test
    void getUnreadCount_multipleUsers_returnsCorrectCounts() {
        // Arrange
        when(messageRepository.countByIsReadFalseAndSenderNot("alice")).thenReturn(5L);
        when(messageRepository.countByIsReadFalseAndSenderNot("bob")).thenReturn(3L);
        when(messageRepository.countByIsReadFalseAndSenderNot("charlie")).thenReturn(0L);

        // Act
        long countAlice = messageService.getUnreadCount("alice");
        long countBob = messageService.getUnreadCount("bob");
        long countCharlie = messageService.getUnreadCount("charlie");

        // Assert
        assertEquals(5L, countAlice);
        assertEquals(3L, countBob);
        assertEquals(0L, countCharlie);
    }

    // ========== Helper Methods ==========

    private ChatMessage createChatMessage(String sender, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setType(ChatMessage.MessageType.CHAT);
        msg.setSender(sender);
        msg.setContent(content);
        return msg;
    }

    private ChatMessage createJoinMessage(String sender) {
        ChatMessage msg = new ChatMessage();
        msg.setType(ChatMessage.MessageType.JOIN);
        msg.setSender(sender);
        msg.setContent("");
        return msg;
    }

    private ChatMessage createLeaveMessage(String sender) {
        ChatMessage msg = new ChatMessage();
        msg.setType(ChatMessage.MessageType.LEAVE);
        msg.setSender(sender);
        msg.setContent("");
        return msg;
    }

    private Message createMessageEntity(String sender, String content) {
        Message msg = new Message();
        msg.setType(ChatMessage.MessageType.CHAT);
        msg.setSender(sender);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        msg.setRead(false);
        return msg;
    }
}
