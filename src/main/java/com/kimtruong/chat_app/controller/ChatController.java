package com.kimtruong.chat_app.controller;

import com.kimtruong.chat_app.model.ChatMessage;
import com.kimtruong.chat_app.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor; // day 9 
import java.util.List;

import com.kimtruong.chat_app.dto.PrivateMessage;
import java.security.Principal;

import com.kimtruong.chat_app.service.OnlineUserService;



/**
 * Controller xử lý các message đến qua WebSocket/STOMP.
 *
 * @MessageMapping("/chat.sendMessage") → nhận message từ client gửi lên /app/chat.sendMessage
 *
 * Luồng gửi tin nhắn:
 * 1. Client publish đến /app/chat.sendMessage
 * 2. sendMessage() lưu vào DB, rồi broadcast đến /topic/public
 * 3. Tất cả client đang subscribe /topic/public đều nhận được
 */

@Controller
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final MessageService messageService;
    private final OnlineUserService onlineUserService;

    public ChatController(SimpMessageSendingOperations messagingTemplate,
                          MessageService messageService,
                          OnlineUserService onlineUserService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.onlineUserService = onlineUserService;
    }


 /** Nhận tin nhắn CHAT, lưu DB, broadcast cho tất cả */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage message) {
         // Lớp 1: Chống null và empty
         if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            return; // im lặng bỏ qua, không throw exception
        }
            // Lớp 3: Giới hạn độ dài
        if (message.getContent().length() > 500) {
            message.setContent(message.getContent().substring(0, 500));
        }

            // Lớp 2: Escape HTML — chống XSS
        String safe = message.getContent()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
        message.setContent(safe);

        //Het day9 

        messageService.save(message);  // ← lưu vào DB
        messagingTemplate.convertAndSend("/topic/public", message);
    }

    
    /** Xử lý khi user mới vào phòng — broadcast thông báo JOIN */
    
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage message,
                        SimpMessageHeaderAccessor headerAccessor
    ) {
        // day 9 Ghi username vào WebSocket session  -- để sau này có thể lấy ra khi cần
        headerAccessor.getSessionAttributes().put("username", message.getSender());
        message.setType(ChatMessage.MessageType.JOIN);

        //day17 : tracking online users
        onlineUserService.addUser(headerAccessor.getSessionId(), message.getSender());
        messagingTemplate.convertAndSend("/topic/online-users", onlineUserService.getOnlineUsers());

        messagingTemplate.convertAndSend("/topic/public", message);
    }

@MessageMapping("/private")
public void sendPrivateMessage(@Payload PrivateMessage message, Principal principal) {
    if (principal == null) {
        return;
    }
    
    message.setSender(principal.getName());
    messagingTemplate.convertAndSendToUser(
        message.getRecipient(),
        "/queue/private",
        message
    );
}


/*  ## Giải thích Bước 3 — `sendPrivateMessage()` trong `ChatController`

--
**🎯 Nó làm gì và như thế nào?**
Nhận tin nhắn private từ client, lấy `sender` từ JWT Principal thay vì tin client tự khai báo, rồi dùng `convertAndSendToUser()` gửi đến đúng 1 người nhận — không ai khác thấy được.

---
**1. Keyword lạ**
`Principal` — interface Java đại diện cho "danh tính đang đăng nhập". Spring tự inject vào method khi có `Authentication` trong context. `principal.getName()` trả về username từ JWT — không thể giả mạo vì lấy từ token đã verify.
`@MessageMapping("/private")` — client publish đến `/app/private` → Spring route đến method này. Tương tự `@GetMapping` nhưng cho WebSocket/STOMP.
`convertAndSendToUser(recipient, "/queue/private", message)` — khác với `convertAndSend()` broadcast cho tất cả, method này Spring tự map thành `/user/{recipient}/queue/private` → chỉ deliver đúng session của `recipient`.
---

**⚙️ Nó chạy như nào?**
```
Client A gửi:
{
    "recipient": "nam",
    "content": "hello riêng"

}
        ↓
@MessageMapping("/private") nhận
        ↓
principal.getName() → lấy "kim" từ JWT (không tin client)
message.setSender("kim")
        ↓
convertAndSendToUser("nam", "/queue/private", message)
        ↓
Spring map → /user/nam/queue/private
        ↓
Chỉ session của "nam" nhận được — kim và người khác không thấy
```
---
**🔥 So sánh với cách khác**
Cách này — lấy sender từ `Principal` (JWT). 
Client không thể giả mạo sender vì server tự điền từ token đã verify.
Cách khác: tin `message.getSender()` từ client tự gửi lên — đơn giản hơn nhưng ai cũng có thể giả danh người khác bằng cách set `sender = "admin"` trong JSON.
Đây là lỗ hổng bảo mật nghiêm trọng, không dùng được trong thực tế. */







/* Day 17 _ Bước 2  : Lưu user vào danh sách online 
**🎯 Nó làm gì?**

Khi user vào phòng chat, ngoài việc broadcast thông báo JOIN như cũ, giờ còn ghi username vào danh sách online và broadcast danh sách đó cho tất cả mọi người.

---

**1. Keyword lạ**

`headerAccessor.getSessionId()` — lấy ID của WebSocket session hiện tại. Mỗi browser tab kết nối vào sẽ được Spring cấp 1 sessionId duy nhất (ví dụ `"abc-123"`). Dùng làm key trong map thay vì username vì cùng 1 username có thể mở nhiều tab.

`onlineUserService.addUser(...)` — gọi vào service vừa tạo ở Bước 1, truyền vào 2 thứ: sessionId (để biết tab nào) và username (để hiển thị).

`onlineUserService.getOnlineUsers()` — lấy danh sách username hiện tại dưới dạng `List<String>` để broadcast.

---

**2. ⚙️ Nó chạy như nào?**

```
kim mở tab, nhập tên, bấm Kết nối
        ↓
Browser gửi lên /app/chat.addUser
        ↓
addUser() chạy — theo thứ tự:
  1. set "username" vào session (để dùng khi disconnect)
  2. set type = JOIN
  3. onlineUserService.addUser("session-abc", "kim")  ← ghi vào map
  4. broadcast ["kim"] đến /topic/online-users         ← sidebar update
  5. broadcast JOIN message đến /topic/public           ← chat hiện "kim đã vào"

nam mở tab, kết nối
        ↓
  3. onlineUserService.addUser("session-xyz", "nam")
  4. broadcast ["kim", "nam"] đến /topic/online-users  ← cả 2 tab update sidebar
  5. broadcast JOIN đến /topic/public
```

---

**3. 🔥 So sánh với cách khác**

**Cách này — broadcast trong `addUser()` controller:**
- Đơn giản, rõ ràng, đúng thời điểm — chắc chắn username đã có trước khi broadcast
- Đây là cách được dùng ở đây

**Cách khác — broadcast trong `SessionConnectedEvent`:**
- Event này fire ngay khi WebSocket handshake xong, **trước** khi client gửi `chat.addUser`
- Tại thời điểm đó username chưa có trong session → broadcast ra danh sách trống hoặc thiếu
- Bẫy hay gặp, sai về timing

**Cách khác — không dùng service, để thẳng `ConcurrentHashMap` trong controller:**
- Vi phạm Single Responsibility — controller không nên vừa xử lý request vừa quản lý state
- Khó test, khó tái sử dụng ở `WebSocketEventListener` (bước 3 cần dùng lại service này khi disconnect)*/

}
