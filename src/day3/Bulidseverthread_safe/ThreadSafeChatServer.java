// package day3.Bulidseverthread_safe;
// import java.util.*;
// import java.util.concurrent.*;

// public class ThreadSafeChatApp {
//     // 1. Dùng Concurrent collections để thread-safe
//     private ConcurrentMap<String, CopyOnWriteArrayList<User>> chatRooms ;
//     private ConcurrentMap<String , Queue<String>> messageQueues ;

//     //2.Lock cho các operations phức tạp hơn
//     private final Object roomlock = new Object();

//     public void ThreadSafeChatServer() {
//         chatRooms = new ConcurrentHashMap<>();
//         messageQueues = new ConcurrentHashMap<>();
//     }
//     //3.Thread-safe method them user vao phong chat
//     public boolean joinRoom(String roomId, User user) {
//         // computerIfAbsent tra ve gia tri hien co neu co, neu khong thi tao moi

//         CopyOnWriteArrayList<User> room = chatRooms.computeIfAbsent(
//             roomId, k -> new CopyOnWriteArrayList<>());

//             // Kiểm tra và add user một cách thread-safe
//             synchronized(room){
//                 if(!room.contains(user)) {
//                     room.add(user);
//                     return true;
//                 } else {
//                     return false; // User da co trong phong
//                 }

//             }
//     }
//     //4.Thread-safe method gui tin nhan
//     public void sendMessage(String roomId, String message){
//         synchronized(roomlock){
//             messageQueues.computeIfAbsent(roomId,k 
//                 -> new ConcurrentLinkedQueue<>()).add(message);

//                 //Broadcast message ddeen tat ca user trong phong 
//                 broadcastMassage(roomId, message);
                
//         }   
//     }

//     //5.Broadcast message den tat ca user trong phong - dong bo hoa
//     private void broadcastMassage(String roomId , String message ){
//         CopyOnWriteArrayList<User> room = chatRooms.get(roomId);
//         if(room != null) {
//             for(User user : room) {
//                 // Giả sử có phương thức gửi tin nhắn tới user
//                 // sendToUser(user, message);
//                 System.out.println("Sending message to " + user.getName() + ": " + message);
//             }
//         }
//     }
//     // 6.Multithreaded testing method
//     public static void main(String[] args) {
//             ThreadSafeChatApp server = new ThreadSafeChatApp();
//         String roomId = "Java-Coders";
        
//         // Tạo 100 users join cùng lúc qua threads
//         List<Thread> threads = new ArrayList<>();
        
//         for (int i = 0; i < 100; i++) {
//             final int userId = i;
//             Thread t = new Thread(() -> {
//                 User user = new User("UID" + userId, "User" + userId);
//                 server.joinRoom(roomId, user);
//                 server.sendMessage(roomId, "Hello from User" + userId);
//             });
//             threads.add(t);
//             t.start();
//         }
        
//         // Đợi tất cả threads hoàn thành
//         threads.forEach(t -> {
//             try { t.join(); } 
//             catch (InterruptedException e) { e.printStackTrace(); }
//         });
        
//         System.out.println("Tất cả users đã join!");
//     }
// }
package day3.Bulidseverthread_safe;
import java.util.*;
import java.util.concurrent.*;



public class ThreadSafeChatServer {
    // 1. Dùng Concurrent collections để thread-safe
    private ConcurrentMap<String, CopyOnWriteArrayList<User>> chatRooms;
    private ConcurrentMap<String, Queue<String>> messages;
    
    // 2. Lock object cho các operations phức tạp
    private final Object roomLock = new Object();
    
    public ThreadSafeChatServer() {
        chatRooms = new ConcurrentHashMap<>();
        messages = new ConcurrentHashMap<>();
    }
    
    // 3. Thread-safe method để add user
    public boolean joinRoom(String roomId, User user) {
        // computeIfAbsent là atomic operation
        CopyOnWriteArrayList<User> room = chatRooms.computeIfAbsent(
            roomId, 
            k -> new CopyOnWriteArrayList<>()
        );
        
        // Kiểm tra và add user một cách thread-safe
        synchronized(room) {
            if (!room.contains(user)) {
                return room.add(user);
            }
        }
        return false;
    }
    
    // 4. Thread-safe method để gửi message
    public void sendMessage(String roomId, String message) {
        synchronized(roomLock) {
            messages.computeIfAbsent(roomId, k -> new ConcurrentLinkedQueue<>())
                   .add(message);
            // Broadcast message đến tất cả users trong phòng
            broadcastMessage(roomId, message);
        }
    }
    
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
}