package myTestday2;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryDemo {
    // SHARED RESOURCE - danh sách tin nhắn
    private static List<String> chatHistory = new ArrayList<>();
    
    static class ChatUser implements Runnable {
        private String userName;
        private int messageCount;
        
        public ChatUser(String name, int count) {
            this.userName = name;
            this.messageCount = count;
        }
        
        @Override
        public void run() {
            for (int i = 1; i <= messageCount; i++) {
                String message = userName + ": Message " + i;
                
                // RACE CONDITION: nhiều thread cùng add vào list
                chatHistory.add(message);
                
                System.out.println("Gửi: " + message);
                
                try {
                    Thread.sleep(50); // Giả lập network delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== DEMO 2: CHAT HISTORY CORRUPTION ===");
        System.out.println("3 users, mỗi user gửi 5 tin nhắn");
        
        // Tạo 3 users
        Thread alice = new Thread(new ChatUser("Alice", 5));
        Thread bob = new Thread(new ChatUser("Bob", 5));
        Thread charlie = new Thread(new ChatUser("Charlie", 5));
        
        // Bắt đầu chat
        alice.start();
        bob.start();
        charlie.start();
        
        // Chờ tất cả hoàn thành
        alice.join();
        bob.join();
        charlie.join();
        
        // Kiểm tra kết quả
        System.out.println("\n=== KIỂM TRA CHAT HISTORY ===");
        System.out.println("Tổng số tin nhắn: " + chatHistory.size());
        System.out.println("Lý tưởng: 15 tin nhắn");
        
        // Đếm số tin nhắn của mỗi user
        int aliceCount = 0, bobCount = 0, charlieCount = 0;
        for (String msg : chatHistory) {
            if (msg.startsWith("Alice")) aliceCount++;
            else if (msg.startsWith("Bob")) bobCount++;
            else if (msg.startsWith("Charlie")) charlieCount++;
        }
        
        System.out.println("\nPhân tích:");
        System.out.println("Alice: " + aliceCount + "/5 messages");
        System.out.println("Bob: " + bobCount + "/5 messages");
        System.out.println("Charlie: " + charlieCount + "/5 messages");
        
        if (chatHistory.size() < 15) {
            System.out.println("\n❌ RACE CONDITION: MẤT TIN NHẮN!");
        }
        
        // Kiểm tra thứ tự
        System.out.println("\n10 tin nhắn đầu tiên:");
        for (int i = 0; i < Math.min(10, chatHistory.size()); i++) {
            System.out.println((i+1) + ". " + chatHistory.get(i));
        }
    }
}