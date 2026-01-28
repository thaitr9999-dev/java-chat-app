package myTestday2;

public class OnlineUsersDemo {
    // BIẾN CHIA SẺ - sẽ bị race condition
    private static int onlineCount = 0;
    
    static class UserThread extends Thread {
        private String userName;
        
        public UserThread(String name) {
            this.userName = name;
        }
        
        @Override
        public void run() {
            // User login - tăng counter
            onlineCount++;  // RACE CONDITION Ở ĐÂY!
            System.out.println(userName + " login. Online: " + onlineCount);
            
            try {
                Thread.sleep(100); // Giả lập user online
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // User logout - giảm counter
            onlineCount--;  // RACE CONDITION Ở ĐÂY!
            System.out.println(userName + " logout. Online: " + onlineCount);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== DEMO 1: ONLINE USERS COUNTER ===");
        System.out.println("10 users cùng login/logout...");
        
        UserThread[] users = new UserThread[10];
        
        // Tạo 10 users
        for (int i = 0; i < 10; i++) {
            users[i] = new UserThread("User-" + (i+1));
        }
        
        // Start tất cả users cùng lúc
        for (UserThread user : users) {
            user.start();
        }
        
        // Chờ tất cả hoàn thành
        for (UserThread user : users) {
            user.join();
        }
        
        System.out.println("\n=== KẾT QUẢ ===");
        System.out.println("Số users online cuối cùng: " + onlineCount);
        System.out.println("Lý tưởng: 0 (tất cả đã logout)");
        System.out.println("Vấn đề: Có thể ≠ 0 do race condition!");
    }
}