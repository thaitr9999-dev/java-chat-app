package day1;

public class User {
    private String id;
    private String username;
    private String email;
    private boolean online;
    
    // Constructor
    public User(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.online = false;
    }
    
    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isOnline() { return online; }
    
    // Setters
    public void setOnline(boolean online) { 
        this.online = online; 
        System.out.println(username + (online ? " dang online" : " da offline"));
    }
    
    // toString 
    @Override
    public String toString() {
        return username + " [" + (online ? "ONLINE nè đó " : "OFFLINE rồi") + "]";
    }
    
    // Test
    public static void main(String[] args) {
        User user1 = new User("1", "Alice", "alice@email.com");
        user1.setOnline(true);
        System.out.println("User: " + user1);
    }
}