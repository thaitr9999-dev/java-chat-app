package com.kimtruong.chat_app.config;

import com.kimtruong.chat_app.model.ChatMessage;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.kimtruong.chat_app.service.OnlineUserService; // day 17


/**
 * Lắng nghe sự kiện WebSocket từ Spring — cụ thể là khi một client ngắt kết nối.
 *
 * Tại sao cần listener này?
 * Khi user đóng tab hoặc mất mạng, không có cách nào gọi controller thông thường.
 * Spring tự động bắn SessionDisconnectEvent → chúng ta dùng để gửi thông báo LEAVE.
 *
 * Username được lưu vào session lúc JOIN (trong ChatController),
 * và được lấy lại ở đây khi DISCONNECT.
 */

@Component
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final OnlineUserService onlineUserService; // day 17


    public WebSocketEventListener(SimpMessageSendingOperations messagingTemplate , OnlineUserService onlineUserService) {
        this.messagingTemplate = messagingTemplate;
        this.onlineUserService = onlineUserService;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // Lấy username đã lưu trong session lúc JOIN
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
                // Day 17 _ Bước 3 : Xóa user khỏi danh sách online
            onlineUserService.removeUser(headerAccessor.getSessionId());
            messagingTemplate.convertAndSend("/topic/online-users", onlineUserService.getOnlineUsers());

            ChatMessage leaveMessage = new ChatMessage();
            leaveMessage.setType(ChatMessage.MessageType.LEAVE);
            leaveMessage.setSender(username);
            messagingTemplate.convertAndSend("/topic/public", leaveMessage);
        }
    }


    

/* Day17 _ Bước 3 : Xóa user khỏi danh sách online 
**🎯 Nó làm gì?**
Khi user đóng tab hoặc mất mạng, ngoài việc broadcast thông báo LEAVE như cũ, giờ còn xóa user khỏi danh sách online và broadcast danh sách mới cho tất cả — sidebar tự cập nhật.
---

**1. Keyword lạ**

Không có keyword mới — toàn bộ đều dùng lại những thứ đã biết từ Bước 1 và 2: `headerAccessor.getSessionId()`, `onlineUserService.removeUser()`, `onlineUserService.getOnlineUsers()`.

Điểm cần chú ý: thứ tự 2 dòng trong `if`:
```java
onlineUserService.removeUser(headerAccessor.getSessionId()); // xóa trước
messagingTemplate.convertAndSend("/topic/online-users", onlineUserService.getOnlineUsers()); // rồi mới broadcast
```
Phải xóa xong rồi mới lấy danh sách — nếu đảo ngược thì broadcast xong mới xóa → client nhận danh sách vẫn còn tên user vừa disconnect.

---

**2. ⚙️ Nó chạy như nào?**

```
nam đóng tab
        ↓
Spring tự bắn SessionDisconnectEvent
        ↓
handleWebSocketDisconnectListener() chạy:
  1. lấy sessionId từ headerAccessor
  2. lấy username "nam" từ session attributes (đã lưu lúc JOIN)
  3. onlineUserService.removeUser("session-xyz") → map: {session-abc: kim}
  4. broadcast ["kim"] đến /topic/online-users  ← sidebar tab kim update
  5. broadcast LEAVE message đến /topic/public  ← chat hiện "nam đã rời phòng"
```

---

**3. 🔥 So sánh với cách khác**

**Cách này — dùng `SessionDisconnectEvent`:**
- Server tự detect disconnect, không cần client làm gì
- Hoạt động cả khi user đóng tab đột ngột, mất mạng, crash browser
- Đây là cách đúng duy nhất cho WebSocket

**Cách khác — client tự gửi "goodbye" message trước khi đóng:**
- Dùng `window.onbeforeunload` trong JS để gửi message LEAVE
- Không đáng tin: nếu mất mạng hoặc crash thì event này không bao giờ fire
- Server sẽ không biết user đã offline → danh sách online sai mãi mãi

**Tại sao `WebSocketEventListener` là `@Component` chứ không phải `@Controller`?**
- Nó không xử lý request từ client gửi lên — nó lắng nghe **event nội bộ của Spring**
- `@Controller` + `@MessageMapping` = nhận message từ client
- `@Component` + `@EventListener` = lắng nghe event từ Spring container
- Hai cơ chế hoàn toàn khác nhau, không thể thay thế cho nhau
    */
    
}

