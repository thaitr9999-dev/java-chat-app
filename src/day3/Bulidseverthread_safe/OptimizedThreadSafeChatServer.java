package day3.Bulidseverthread_safe;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class User {
    private final String id;
    private final String name;
    
    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

public class OptimizedThreadSafeChatServer {
    
    // ========== PHẦN 1: COLLECTIONS THREAD-SAFE TỐI ƯU ==========
    
    // 1.1. Thay vì CopyOnWriteArrayList, dùng ConcurrentHashMap cho users trong room
    // → O(1) cho contains() thay vì O(n) của CopyOnWriteArrayList
    // → putIfAbsent() atomic thay vì cần synchronized block
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, User>> roomUsersMap;
    
    // 1.2. Message queues: ConcurrentLinkedQueue không cần synchronized
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> messageQueues;
    
    // 1.3. Room metadata với AtomicInteger cho thống kê
    private final ConcurrentHashMap<String, RoomStats> roomStats;
    
    static class RoomStats {
        AtomicInteger messageCount = new AtomicInteger();
        AtomicInteger totalUsers = new AtomicInteger();
        long createdTime = System.currentTimeMillis();
    }
    
    static class Message {
        final String senderId;
        final String content;
        final long timestamp;
        
        Message(String senderId, String content) {
            this.senderId = senderId;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    
    // ========== PHẦN 2: LOCK OPTIMIZATION ==========
    
    // 2.1. Striped locks thay vì single lock
    // → Mỗi room hash đến 1 lock trong 64 locks → giảm contention
    private static final int STRIPE_COUNT = 64;
    private final ReentrantLock[] roomCreationLocks;
    
    // 2.2. ReadWriteLock cho thống kê (nhiều read, ít write)
    private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
    
    
    // ========== PHẦN 3: THREAD-SAFE METHOD OPTIMIZED ==========
    
    public OptimizedThreadSafeChatServer() {
        this.roomUsersMap = new ConcurrentHashMap<>();
        this.messageQueues = new ConcurrentHashMap<>();
        this.roomStats = new ConcurrentHashMap<>();
        
        // Khởi tạo striped locks
        this.roomCreationLocks = new ReentrantLock[STRIPE_COUNT];
        for (int i = 0; i < STRIPE_COUNT; i++) {
            roomCreationLocks[i] = new ReentrantLock();
        }
    }
    
    /**
     * PHẦN 3: Thread-safe method để add user - TỐI ƯU
     * 
     * Vấn đề với code cũ:
     * 1. computeIfAbsent có thể gọi hàm tạo nhiều lần
     * 2. synchronized(room) với CopyOnWriteArrayList chậm
     * 3. room.contains(user) là O(n)
     * 
     * Giải pháp tối ưu:
     * 1. Fast path: check room tồn tại trước
     * 2. Dùng ConcurrentHashMap.putIfAbsent() atomic
     * 3. Striped locks cho room creation
     */
    public boolean joinRoom(String roomId, User user) {
        // FAST PATH: Room đã tồn tại
        ConcurrentHashMap<String, User> roomUsers = roomUsersMap.get(roomId);
        if (roomUsers != null) {
            // Atomic operation: putIfAbsent = check và add trong 1 operation
            User existing = roomUsers.putIfAbsent(user.getId(), user);
            if (existing == null) {
                updateRoomStats(roomId, 1, 0); // Thêm user
                return true;
            }
            return false; // User đã tồn tại
        }
        
        // SLOW PATH: Cần tạo room mới - dùng striped lock
        int lockIndex = getLockIndex(roomId);
        ReentrantLock lock = roomCreationLocks[lockIndex];
        lock.lock();
        try {
            // Double-check sau khi có lock
            roomUsers = roomUsersMap.get(roomId);
            if (roomUsers == null) {
                roomUsers = new ConcurrentHashMap<>();
                roomUsersMap.put(roomId, roomUsers);
                roomStats.put(roomId, new RoomStats());
            }
            
            // Thêm user vào room đã tồn tại/mới tạo
            User existing = roomUsers.putIfAbsent(user.getId(), user);
            if (existing == null) {
                updateRoomStats(roomId, 1, 0);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    private int getLockIndex(String roomId) {
        return Math.abs(roomId.hashCode()) % STRIPE_COUNT;
    }
    
    private void updateRoomStats(String roomId, int userDelta, int messageDelta) {
        RoomStats stats = roomStats.get(roomId);
        if (stats != null) {
            if (userDelta != 0) stats.totalUsers.addAndGet(userDelta);
            if (messageDelta != 0) stats.messageCount.addAndGet(messageDelta);
        }
    }
    
    
    // ========== PHẦN 4: GỬI MESSAGE TỐI ƯU ==========
    
    /**
     * PHẦN 4: Thread-safe method gửi message - TỐI ƯU
     * 
     * Vấn đề với code cũ:
     * 1. synchronized(roomLock) block toàn bộ server
     * 2. Broadcast chạy sync trong lock
     * 
     * Giải pháp tối ưu:
     * 1. Chỉ lock khi cần atomicity cho nhiều operations
     * 2. Broadcast chạy async
     * 3. Message queue tự thread-safe
     */
    public void sendMessage(String roomId, String senderId, String content) {
        // 1. Tạo message object
        Message message = new Message(senderId, content);
        
        // 2. Thêm vào queue - atomic operation
        ConcurrentLinkedQueue<Message> queue = messageQueues
            .computeIfAbsent(roomId, k -> new ConcurrentLinkedQueue<>());
        queue.add(message);
        
        // 3. Update stats - atomic
        updateRoomStats(roomId, 0, 1);
        
        // 4. Broadcast ASYNC - không block sender
        CompletableFuture.runAsync(() -> {
            broadcastMessage(roomId, message);
        });
        
        // 5. Cleanup old messages (background task)
        cleanupOldMessages(roomId, queue);
    }
    
    private void cleanupOldMessages(String roomId, ConcurrentLinkedQueue<Message> queue) {
        // Giữ tối đa 1000 messages mỗi room
        while (queue.size() > 1000) {
            queue.poll(); // Remove oldest
        }
    }
    
    
    // ========== PHẦN 5: BROADCAST TỐI ƯU ==========
    
    /**
     * PHẦN 5: Broadcast message - TỐI ƯU
     * 
     * Vấn đề với code cũ:
     * 1. Dùng CopyOnWriteArrayList.iterator() tạo snapshot
     * 2. Sequential sending
     * 
     * Giải pháp tối ưu:
     * 1. Dùng ConcurrentHashMap.values() không cần snapshot
     * 2. Parallel sending
     * 3. Error handling
     */
    private void broadcastMessage(String roomId, Message message) {
        ConcurrentHashMap<String, User> roomUsers = roomUsersMap.get(roomId);
        if (roomUsers == null || roomUsers.isEmpty()) {
            return;
        }
        
        // Parallel streaming cho high performance
        roomUsers.values().parallelStream().forEach(user -> {
            try {
                // Simulate sending
                System.out.printf("[%s] Send to %s: %s%n",
                    Thread.currentThread().getName(),
                    user.getName(),
                    message.content);
                
                // Trong thực tế: socket.send(), WebSocket, etc.
                
            } catch (Exception e) {
                // Log error nhưng không fail toàn bộ broadcast
                System.err.println("Failed to send to user " + user.getId() + ": " + e.getMessage());
            }
        });
    }
    
    
    // ========== PHẦN 6: MULTI-THREAD TEST NÂNG CAO ==========
    
    /**
     * PHẦN 6: Multi-thread test - TỐI ƯU
     * 
     * Cải tiến:
     * 1. Dùng CountDownLatch thay vì Thread.join()
     * 2. Thread pool thay vì tạo thread trực tiếp
     * 3. Performance measurement
     */
    public static void main(String[] args) throws Exception {
        OptimizedThreadSafeChatServer server = new OptimizedThreadSafeChatServer();
        String roomId = "Java-Coders";
        
        final int USER_COUNT = 1000;
        final int THREAD_POOL_SIZE = 20;
        
        // 6.1. Thread pool thay vì tạo 1000 threads
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        // 6.2. CountDownLatch để đợi tất cả tasks hoàn thành
        CountDownLatch latch = new CountDownLatch(USER_COUNT);
        
        // 6.3. Performance measurement
        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        // 6.4. Submit tasks
        for (int i = 0; i < USER_COUNT; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    User user = new User("UID" + userId, "User" + userId);
                    
                    // Join room
                    boolean joined = server.joinRoom(roomId, user);
                    if (joined) {
                        successCount.incrementAndGet();
                        
                        // Send message
                        server.sendMessage(roomId, user.getId(), 
                            "Hello from " + user.getName());
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Error for user " + userId + ": " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 6.5. Đợi tất cả hoàn thành với timeout
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        // 6.6. Shutdown và thống kê
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        // 6.7. Kết quả
        System.out.println("\n=== TEST RESULTS ===");
        System.out.println("Total users: " + USER_COUNT);
        System.out.println("Success: " + successCount.get());
        System.out.println("Failed: " + failCount.get());
        System.out.println("Time: " + (endTime - startTime) + "ms");
        System.out.println("Completed: " + completed);
        
        // 6.8. Verify data consistency
        ConcurrentHashMap<String, User> room = server.roomUsersMap.get(roomId);
        if (room != null) {
            System.out.println("Room size: " + room.size());
            System.out.println("Unique users: " + 
                (room.size() == successCount.get() ? "✓" : "✗"));
        }
    }
    
    
    // ========== THÊM CÁC UTILITY METHODS ==========
    
    /**
     * Thread-safe method để lấy users trong room
     * → Không cần synchronized vì ConcurrentHashMap.values()
     *   trả về view an toàn
     */
    public Collection<User> getUsersInRoom(String roomId) {
        ConcurrentHashMap<String, User> room = roomUsersMap.get(roomId);
        if (room == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(room.values());
    }
    
    /**
     * Thread-safe method để remove user
     * → Atomic operation với ConcurrentHashMap.remove()
     */
    public boolean leaveRoom(String roomId, String userId) {
        ConcurrentHashMap<String, User> room = roomUsersMap.get(roomId);
        if (room != null) {
            User removed = room.remove(userId);
            if (removed != null) {
                updateRoomStats(roomId, -1, 0);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Thread-safe method để lấy messages
     * → Copy messages để tránh concurrent modification
     */
    public List<Message> getRecentMessages(String roomId, int limit) {
        ConcurrentLinkedQueue<Message> queue = messageQueues.get(roomId);
        if (queue == null) {
            return Collections.emptyList();
        }
        
        // Copy to array để thread-safe iteration
        Message[] messages = queue.toArray(new Message[0]);
        int start = Math.max(0, messages.length - limit);
        
        List<Message> result = new ArrayList<>(limit);
        for (int i = start; i < messages.length; i++) {
            result.add(messages[i]);
        }
        return result;
    }
}