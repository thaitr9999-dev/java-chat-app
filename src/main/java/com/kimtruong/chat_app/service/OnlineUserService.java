package com.kimtruong.chat_app.service;
import org.springframework.stereotype.Service;
import java.util.ArrayList ; 
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/* 
**🎯 Nó làm gì?**
Lưu trữ danh sách user đang online trong memory. Mỗi khi có connect/disconnect, các class khác gọi vào đây để cập nhật.

---
**1. Keyword lạ**
`ConcurrentHashMap<String, String>` — như `HashMap` bình thường nhưng thread-safe. WebSocket có nhiều thread chạy đồng thời (mỗi connection 1 thread), nếu dùng `HashMap` thường thì 2 thread ghi cùng lúc → data bị corrupt. `ConcurrentHashMap` xử lý điều này tự động.
`sessionId` — mỗi WebSocket connection được Spring cấp 1 ID duy nhất. Dùng làm key thay vì username vì 1 username có thể mở nhiều tab → nhiều sessionId khác nhau.


---
**⚙️ Nó chạy như nào?**
```
Tab 1 login "kim"  → addUser("abc", "kim")   → map: {abc=kim}
Tab 2 login "kim"  → addUser("xyz", "kim")   → map: {abc=kim, xyz=kim}
Tab 2 đóng         → removeUser("xyz")        → map: {abc=kim}
getOnlineUsers()   → ["kim"]                  ← trả list usernames
``

---
**🔥 So sánh với cách khác**
| | Cách này | Cách khác |
|---|---|---|
| **Key** | `sessionId → username` | `username → count` |
| **Multi-tab** | Tự xử lý đúng | Phải tự đếm, dễ bug |
| **Disconnect** | `removeUser(sessionId)` là đủ | Phải check count > 0 trước |

Dùng `sessionId` làm key là chuẩn vì Spring đảm bảo mỗi sessionId là unique và tự động hết hạn khi disconnect — không cần cleanup thủ công.
*/







@Service
public class OnlineUserService {
    //sessionID -> username
    private final ConcurrentHashMap<String, String> onlineUsers = new ConcurrentHashMap<>();

    public void addUser(String sessionId, String username) {
        onlineUsers.put(sessionId, username);
    }

    public void removeUser(String sessionId) {
        onlineUsers.remove(sessionId);
    }

    public List<String> getOnlineUsers() {
        return new ArrayList<>(onlineUsers.values());
    }

}
