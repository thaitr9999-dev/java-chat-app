package day3.Bulidseverthread_safe;
import java.util.*;
import java.util.concurrent.*;



public class ThreadSafeChatServer {
    // 1. Dùng Concurrent collections để thread-safe
    private ConcurrentMap<String, CopyOnWriteArrayList<User>> chatRooms;
    private ConcurrentMap<String, Queue<String>> messages;
/*
// ConcurrentMap: Là ConcurrentHashMap - thread-safe implementation của Map

//Giả sử CopyOnWriteArrayList có 2 elements: [User1, User2]
// Thread A: users.add(User3)
// 1. Tạo copy mới: [User1, User2, User3]
// 2. Tham chiếu đến array mới
// Thread B đang iteration vẫn đọc trên array cũ: [User1, User2]
// Không bao giờ bị ConcurrentModificationException */

    // 2. Lock object cho các operations phức tạp
    private final Object roomLock = new Object();
/* Mặc dù ConcurrentHashMap thread-safe, nhưng (chuỗi thao tác) vẫn cần sync
    Ví dụ vấn đề: Check-then-act pattern

//KHÔNG AN TOÀN - Race condition
    if (!map.containsKey(key)) {  // Thread A check
        map.put(key, value);      // Thread B có thể đã put giữa lúc này
    }

// GIẢI PHÁP: Dùng synchronized block
    synchronized(lock) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
    }
        
*/
    
    public ThreadSafeChatServer() {
        chatRooms = new ConcurrentHashMap<>();
        messages = new ConcurrentHashMap<>();
    }
    
    // 3. Thread-safe method để add user
    public boolean joinRoom(String roomId, User user) {
        // computeIfAbsent là thread-safe để khởi tạo phòng nếu chưa tồn tại
        CopyOnWriteArrayList<User> room = chatRooms.computeIfAbsent(
            roomId, 
            k -> new CopyOnWriteArrayList<>()
        );
/*computeIfAbsent(): Atomic (nguyên tử) - không bị thread khác xen vào giữa

Hoạt động:

Kiểm tra key roomId đã tồn tại chưa
Nếu chưa, tạo new CopyOnWriteArrayList<>()
Put vào map nếu cần
Luôn trả về List của room, dù mới tạo hoặc đã tồn tại */
        
        // Kiểm tra và add user một cách thread-safe
        synchronized(room) {
            if (!room.contains(user)) {
                return room.add(user);
            }
        }
        return false;
    }
/*// Race condition giữa contains() và add()
// Thread A: if (!room.contains(user1)) → true (user1 chưa có)
// Thread B: if (!room.contains(user1)) → true (cùng lúc)
// Thread A: room.add(user1) → thành công
// Thread B: room.add(user1) → TRÙNG USER!
//  */
    
    // 4. Thread-safe method để gửi message
    public void sendMessage(String roomId, String message) {
        synchronized(roomLock) {
            messages.computeIfAbsent(roomId, k -> new ConcurrentLinkedQueue<>())
                   .add(message);
            // Broadcast message đến tất cả users trong phòng
            broadcastMessage(roomId, message);
        }
    }
/*Dùng roomLock thay vì synchronized method:
    Chỉ block phần code cần thiết
    Các threads gửi message đến room khác vẫn chạy song song
ConcurrentLinkedQueue:
    Thread-safe queue implementation
    Không cần synchronized khi add/take
    Non-blocking algorithms (CAS operations)
Tại sao cần synchronized ở đây?
    Đảm bảo message được add và broadcast ngay lập tức
    Tránh tình huống: message add nhưng broadcast chưa kịp chạy */
    
    // 5. Broadcast message - cần đồng bộ hóa
    private void broadcastMessage(String roomId, String message) {
        CopyOnWriteArrayList<User> users = chatRooms.get(roomId);
        if (users != null) {
            // CopyOnWriteArrayList an toàn khi iteration
            for (User user : users) {
                // Gửi message đến từng user (giả lập)
                System.out.println("Send to " + user.getName() + ": " + message);
            }
        }
    }
/*
// Thread A: Đang broadcast (iteration)
// Thread B: Thêm user mới vào room
// → Thread A vẫn iteration trên danh sách cũ
// → Thread B tạo danh sách mới, không ảnh hưởng Thread A
 */

    // 6. Multi-thread test
    public static void main(String[] args) {
        ThreadSafeChatServer server = new ThreadSafeChatServer();
        String roomId = "Java-Coders";
        
        // Tạo 100 users join cùng lúc qua threads
        List<Thread> threads = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            final int userId = i;
            Thread t = new Thread(() -> {
                User user = new User("UID" + userId, "User" + userId);
                server.joinRoom(roomId, user);
                server.sendMessage(roomId, "Hello from User" + userId);
            });
            threads.add(t);
            t.start();
        }
        
        // Đợi tất cả threads hoàn thành
        threads.forEach(t -> {
            try { t.join(); } 
            catch (InterruptedException e) { e.printStackTrace(); }
        });
        
        System.out.println("Tất cả users đã join!");
    }
/*100 Threads chạy song song:

        Tạo 100 users khác nhau

        Cùng join room "Java-Coders"

        Cùng gửi message

Khả năng xảy ra race condition:

    Nhiều threads cùng gọi joinRoom() với cùng roomId

    Nhiều threads cùng gọi sendMessage()

t.join():

    Main thread đợi tất cả threads hoàn thành

    Đảm bảo kết quả cuối cùng đầy đủ
 */
}