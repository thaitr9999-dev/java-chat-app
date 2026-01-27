package day2;

    public class RunnableExample{
       public static void main(String[] args) {
        System.out.println("=== DAY 2: TEST THREAD/RUNNABLE ===");
        System.out.println("Main thread ID: " + Thread.currentThread());
        
        // Tạo 3 Runnable instances
        Runnable user1 = new MyRunnable2("Alice");
        Runnable user2 = new MyRunnable2("Bob", 5); // Bob gửi 5 tin
        Runnable user3 = new MyRunnable2("Charlie");
        
        // Tạo Thread từ Runnable
        Thread thread1 = new Thread(user1);
        Thread thread2 = new Thread(user2);
        Thread thread3 = new Thread(user3);
        
        // Đặt tên cho thread (khác với threadName trong Runnable)
        thread1.setName("Thread-Alice");
        thread2.setName("Thread-Bob");
        thread3.setName("Thread-Charlie");
        
        System.out.println("\n=== BẮT ĐẦU TẤT CẢ THREAD ===");
        
        // Start các thread
        thread1.start();
        thread2.start();
        thread3.start();
        
        // Main thread tiếp tục làm việc khác
        System.out.println("\nMain thread tiếp tục làm việc...");
    }
}