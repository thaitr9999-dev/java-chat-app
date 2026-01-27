package day2;

public class MyRunnable2 implements Runnable {
    private String threadName;
    private int messageCount;
    
    // Constructor có thể tùy chỉnh số tin nhắn
    public MyRunnable2(String threadName) {
        this(threadName, 3); // Mặc định 3 tin nhắn
    }

    public MyRunnable2(String threadName, int messageCount) {
        this.threadName = threadName;
        this.messageCount = messageCount;
    }

    @Override
    public void run() {
        System.out.println("[" + threadName + "] Bắt đầu...");
        System.out.println("Thread ID: " + Thread.currentThread());
        
        for (int i = 1; i <= messageCount; i++) {
            System.out.println("[" + threadName + "] Tin nhắn #" + i);
            
            try {
                // Random sleep time giống thực tế
                int sleepTime = 500 + (int)(Math.random() * 500); // 500-1000ms
                Thread.sleep(sleepTime);
                
            } catch (InterruptedException e) {
                System.out.println("[" + threadName + "] Bị gián đoạn!");
                return; // Kết thúc thread khi bị interrupt
            }
        }
        
        System.out.println("[" + threadName + "] Đã hoàn thành.");
    }
}