package myTestday2;

import java.util.*;

public class ConcurrentModificationDemo {
    private static List<String> connectedUsers = new ArrayList<>();
    
    public static void main(String[] args) {
        System.out.println("=== DEMO 3: ConcurrentModificationException ===");
        
        // Thread 1: Thêm user mới
        Thread addUserThread = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                connectedUsers.add("User-" + i);
                System.out.println("Thêm User-" + i);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        
        // Thread 2: Broadcast tin nhắn (duyệt list)
        Thread broadcastThread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(150);
                    System.out.println("Broadcast đến " + connectedUsers.size() + " users:");
                    
                    // SẼ BỊ ConcurrentModificationException!
                    for (String user : connectedUsers) {
                        System.out.println("  → Gửi đến: " + user);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ConcurrentModificationException e) {
                    System.out.println("❌ LỖI: " + e.getClass().getSimpleName());
                    System.out.println("   Không thể duyệt list khi đang bị sửa!");
                }
            }
        });
        
        addUserThread.start();
        broadcastThread.start();
        
        try {
            addUserThread.join();
            broadcastThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}