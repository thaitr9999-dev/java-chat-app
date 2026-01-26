package day1;

import java.util.*;

public class CollectionsReview {
    public static void main(String[] args) {
        System.out.println("=== DAY 1: ÔN TẬP JAVA COLLECTIONS ===");
        System.out.println("Mục tiêu: Hiểu Collections để xây dựng Chat App");
        System.out.println("Thời gian: 2 giờ (19:00 - 21:00)");
        System.out.println("");
        
        // ======================
        // PHẦN 1: LIST
        // ======================
        System.out.println("📌 PHẦN 1: LIST - Danh sách có thứ tự");
        System.out.println("--------------------------------------");
        
        System.out.println("\n1. ArrayList - Tốt cho đọc tin nhắn:");
        List<String> messages = new ArrayList<>();
        messages.add("Xin chào!");
        messages.add("Chào mừng đến với chat app");
        messages.add("Hãy bắt đầu học Java");
        
        System.out.println("   Tin nhắn: " + messages);
        System.out.println("   Tin nhắn thứ 2: " + messages.get(1));
        
        System.out.println("\n2. LinkedList - Tốt cho thêm/xóa tin nhắn:");
        LinkedList<String> chatHistory = new LinkedList<>();
        chatHistory.add("User1: Hello");
        chatHistory.addFirst("System: Welcome!");
        chatHistory.addLast("User2: Hi everyone");
        
        System.out.println("   Lịch sử chat: " + chatHistory);
        System.out.println("   Tin nhắn đầu: " + chatHistory.getFirst());
        
        // ======================
        // PHẦN 2: SET
        // ======================
        System.out.println("\n\n📌 PHẦN 2: SET - Tập hợp không trùng");
        System.out.println("--------------------------------------");
        
        System.out.println("\n3. HashSet - Danh sách bạn bè:");
        Set<String> friends = new HashSet<>();
        friends.add("Alice");
        friends.add("Bob");
        friends.add("Charlie");
        friends.add("Alice"); // Không thêm trùng
        
        System.out.println("   Bạn bè: " + friends);
        System.out.println("   Alice có trong danh sách? " + friends.contains("Alice"));
        
        // ======================
        // PHẦN 3: MAP
        // ======================
        System.out.println("\n\n📌 PHẦN 3: MAP - Ánh xạ key-value");
        System.out.println("--------------------------------------");
        
        System.out.println("\n4. HashMap - Phòng chat:");
        Map<String, Integer> chatRooms = new HashMap<>();
        chatRooms.put("general", 15);
        chatRooms.put("java-help", 8);
        chatRooms.put("off-topic", 23);
        
        System.out.println("   Các phòng: " + chatRooms);
        System.out.println("   Số người trong phòng general: " + chatRooms.get("general"));
        
        // Duyệt qua Map
        System.out.println("\n   Chi tiết từng phòng:");
        for (Map.Entry<String, Integer> room : chatRooms.entrySet()) {
            System.out.println("   - #" + room.getKey() + ": " + room.getValue() + " người");
        }
        
        System.out.println("\n\n✅ KẾT THÚC PHẦN LÝ THUYẾT");
        System.out.println("⏰ Thời gian: 19:00 - 20:00");
        System.out.println("📚 Đã học: ArrayList, LinkedList, HashSet, HashMap");
    }
}
