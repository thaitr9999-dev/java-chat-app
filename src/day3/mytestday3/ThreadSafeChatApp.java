package day3.mytestday3;

import java.util.*;
import java.util.concurrent.*;

// Sử dụng Collections thread-safe và Concurrent Collections
public class ThreadSafeChatApp {
    // Cách 1: Dùng Collections.synchronizedXXX()
    private Map<String, List<User>> chatRooms =
        Collections.synchronizedMap(new HashMap<>());
    
    // Cách 2: Dùng ConcurrentHashMap (TỐT NHẤT cho chat app)
    private Map<String, CopyOnWriteArrayList<User>> concurrentChatRooms = 
        new ConcurrentHashMap<>();
    
    public void addUserToRoom(String roomId, User user) {
        concurrentChatRooms
            .computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>())
            .add(user);
    }
}