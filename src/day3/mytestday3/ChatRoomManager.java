package day3.mytestday3;

import java.util.* ;

// cơ bản về synchronized
public class ChatRoomManager {
    private Map<String, List<User>> chatRooms = new HashMap<>();
    
    // Cách 1: Synchronized method
    public synchronized void joinRoom(String roomId, User user) {
        List<User> users = chatRooms.getOrDefault(roomId, new ArrayList<>());
        users.add(user);
        chatRooms.put(roomId, users);
    }
    
    // Cách 2: Synchronized block (tốt hơn về performance)
    public void joinRoomBetter(String roomId, User user) {
        synchronized(this) {  // Chỉ synchronized phần cần thiết
            List<User> users = chatRooms.getOrDefault(roomId, new ArrayList<>());
            users.add(user);
            chatRooms.put(roomId, users);
        }
        // Code không cần sync có thể chạy bên ngoài
        //notifyUserJoined(user);
    }
}