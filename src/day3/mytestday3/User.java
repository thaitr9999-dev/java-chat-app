package day3.mytestday3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    
    // Collections: Map<String, List<User>> chatRooms
Map<String, List<User>> chatRooms = new HashMap<>();

// Khi 2 users cùng join 1 phòng chat cùng lúc
public void joinRoom(String roomId, User user) {
    List<User> users = chatRooms.getOrDefault(roomId, new ArrayList<>());
    users.add(user);  // RACE CONDITION xảy ra ở đây!
    chatRooms.put(roomId, users);
}
    
}
