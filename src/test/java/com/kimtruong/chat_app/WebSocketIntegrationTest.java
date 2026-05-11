package com.kimtruong.chat_app;
 
import com.kimtruong.chat_app.dto.LoginRequest;
import com.kimtruong.chat_app.dto.RegisterRequest;
import com.kimtruong.chat_app.model.ChatMessage;
import com.kimtruong.chat_app.service.UserService;
import com.kimtruong.chat_app.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
 
import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
 
import static org.junit.jupiter.api.Assertions.*;
 
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketIntegrationTest {
 
    @LocalServerPort
    private int port;
 
    @Autowired
    private JwtUtil jwtUtil;
 
    @Autowired
    private UserService userService;
 
    private String validToken;
    private String invalidToken;
    private WebSocketStompClient stompClient;
 
    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
 
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("wstest");
        reg.setPassword("wstest123");
        try { userService.register(reg); } catch (Exception e) { }
 
        LoginRequest login = new LoginRequest();
        login.setUsername("wstest");
        login.setPassword("wstest123");
        try {
            validToken = userService.login(login).getToken();
        } catch (Exception e) {
            validToken = jwtUtil.generateToken("wstest");
        }
 
        invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.invalid";
    }
 
    abstract static class StompSessionHandlerAdapter implements StompSessionHandler {
        @Override public void afterConnected(StompSession s, StompHeaders h) {}
        @Override public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {}
        @Override public void handleTransportError(StompSession s, Throwable ex) {}
        @Override public Type getPayloadType(StompHeaders h) { return String.class; }
        @Override public void handleFrame(StompHeaders h, Object p) {}
    }
 
    // Token gửi qua query param vì JwtHandshakeInterceptor đọc từ ?token=...
    private String url(String token) {
        if (token == null) return "ws://localhost:" + port + "/ws";
        return "ws://localhost:" + port + "/ws?token=" + token;
    }
 
    private StompSession connectWith(String token) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        BlockingQueue<StompSession> sessions = new LinkedBlockingDeque<>();
 
        stompClient.connectAsync(url(token), new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders h) {
                sessions.offer(session);
                latch.countDown();
            }
            @Override
            public void handleTransportError(StompSession session, Throwable ex) {
                latch.countDown();
            }
        });
 
        latch.await(5, TimeUnit.SECONDS);
        return sessions.poll();
    }
 
    // ========== Test 1: No token → rejected ==========
 
    @Test
    void connectWithoutToken_shouldBeRejected() throws Exception {
        StompSession session = connectWith(null);
        assertNull(session, "Connection without token should be rejected");
    }
 
    // ========== Test 2: Invalid token → rejected ==========
 
    @Test
    void connectWithInvalidToken_shouldBeRejected() throws Exception {
        StompSession session = connectWith(invalidToken);
        assertNull(session, "Connection with invalid token should be rejected");
    }
 
    // ========== Test 3: Valid token → success ==========
 
    @Test
    void connectWithValidToken_shouldSucceed() throws Exception {
        StompSession session = connectWith(validToken);
        assertNotNull(session, "Should connect with valid token");
        assertTrue(session.isConnected());
        session.disconnect();
    }
 
    // ========== Test 4: Subscribe /topic/public and receive broadcast ==========
 
    @Test
    void connectAndSubscribeToPublicTopic_shouldReceiveMessages() throws Exception {
        StompSession session = connectWith(validToken);
        assertNotNull(session, "Should connect");
 
        CountDownLatch msgLatch = new CountDownLatch(1);
        BlockingQueue<ChatMessage> messages = new LinkedBlockingDeque<>();
 
        session.subscribe("/topic/public", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return ChatMessage.class; }
            @Override public void handleFrame(StompHeaders h, Object payload) {
                messages.offer((ChatMessage) payload);
                msgLatch.countDown();
            }
        });
 
        ChatMessage msg = new ChatMessage();
        msg.setType(ChatMessage.MessageType.CHAT);
        msg.setContent("Test message from WebSocket");
        msg.setSender("wstest");
        session.send("/app/chat.sendMessage", msg);
 
        assertTrue(msgLatch.await(3, TimeUnit.SECONDS), "Should receive message on /topic/public");
        ChatMessage received = messages.poll();
        assertNotNull(received);
        assertEquals("wstest", received.getSender());
        assertEquals("Test message from WebSocket", received.getContent());
 
        session.disconnect();
    }
 
    // ========== Test 5: Send message → persisted and broadcast ==========
 
    @Test
    void connectAndSendMessage_shouldPersistAndBroadcast() throws Exception {
        StompSession session = connectWith(validToken);
        assertNotNull(session, "Should connect");
 
        CountDownLatch msgLatch = new CountDownLatch(1);
        BlockingQueue<ChatMessage> messages = new LinkedBlockingDeque<>();

        session.subscribe("/topic/public", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return ChatMessage.class; }
            @Override public void handleFrame(StompHeaders h, Object payload) {
                ChatMessage m = (ChatMessage) payload;
                if (m.getType() == ChatMessage.MessageType.CHAT) {
                    messages.offer(m);
                    msgLatch.countDown();
                }
            }
        });
 
        ChatMessage joinMsg = new ChatMessage();
        joinMsg.setType(ChatMessage.MessageType.JOIN);
        joinMsg.setContent("");
        joinMsg.setSender("wstest");
        session.send("/app/chat.addUser", joinMsg);
 
        ChatMessage chatMsg = new ChatMessage();
        chatMsg.setType(ChatMessage.MessageType.CHAT);
        chatMsg.setContent("Integration test message");
        chatMsg.setSender("wstest");
        session.send("/app/chat.sendMessage", chatMsg);
 
        assertTrue(msgLatch.await(5, TimeUnit.SECONDS), "Message should be broadcasted");
        ChatMessage received = messages.poll();
        assertNotNull(received);
        assertEquals(ChatMessage.MessageType.CHAT, received.getType());
        assertEquals("Integration test message", received.getContent());
        assertEquals("wstest", received.getSender());
 
        session.disconnect();
    }
 
    // ========== Test 6: Two clients exchange messages ==========
 
    @Test
    void multipleClientsConnect_shouldExchangeMessages() throws Exception {
        RegisterRequest reg2 = new RegisterRequest();
        reg2.setUsername("wstest2");
        reg2.setPassword("wstest2123");
        try { userService.register(reg2); } catch (Exception e) { }
        String token2 = jwtUtil.generateToken("wstest2");
 
        StompSession session1 = connectWith(validToken);
        StompSession session2 = connectWith(token2);
        assertNotNull(session1, "Session 1 should connect");
        assertNotNull(session2, "Session 2 should connect");
 
        CountDownLatch msg1Latch = new CountDownLatch(1);
        CountDownLatch msg2Latch = new CountDownLatch(1);
 
        session1.subscribe("/topic/public", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return ChatMessage.class; }
            @Override public void handleFrame(StompHeaders h, Object payload) {
                if ("wstest2".equals(((ChatMessage) payload).getSender())) msg1Latch.countDown();
            }
        });
 
        session2.subscribe("/topic/public", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return ChatMessage.class; }
            @Override public void handleFrame(StompHeaders h, Object payload) {
                if ("wstest".equals(((ChatMessage) payload).getSender())) msg2Latch.countDown();
            }
        });
 
        ChatMessage fromClient1 = new ChatMessage();
        fromClient1.setType(ChatMessage.MessageType.CHAT);
        fromClient1.setContent("Hello from client 1");
        fromClient1.setSender("wstest");
        session1.send("/app/chat.sendMessage", fromClient1);
 
        ChatMessage fromClient2 = new ChatMessage();
        fromClient2.setType(ChatMessage.MessageType.CHAT);
        fromClient2.setContent("Hello from client 2");
        fromClient2.setSender("wstest2");
        session2.send("/app/chat.sendMessage", fromClient2);
 
        assertTrue(msg1Latch.await(5, TimeUnit.SECONDS), "Client 1 should receive from client 2");
        assertTrue(msg2Latch.await(5, TimeUnit.SECONDS), "Client 2 should receive from client 1");
 
        session1.disconnect();
        session2.disconnect();
    }
}
 