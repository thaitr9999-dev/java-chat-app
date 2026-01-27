package day1;

import java.util.* ; 

public class UserManager {
    private Map<String , List<User>> groupUsers = new HashMap<>();

    public UserManager() {
        groupUsers = new HashMap<>();
        // Have some default groups
        groupUsers.put("General", new ArrayList<>());
        groupUsers.put ("Friends" , new ArrayList<>());
    }

    // Add user to group
    public void addUsertoGroup (String groupName , User user) {
       if(!groupUsers.containsKey(groupName)) {
           groupUsers.put (groupName , new ArrayList<>());
       }

       List<User> groupListuser = groupUsers.get (groupName) ;
        boolean userExists = false;
    for (User existingUser : groupListuser) {
        if (existingUser.getId().equals(user.getId())) {
            userExists = true;
            break;
        }
    }
    
    // Add user if not exists
    if (!userExists) {
        groupListuser.add(user);
        System.out.println("ADD " + user.getUsername() + " vào nhóm '" + groupName + "'");
    } else {
        System.out.println(" " + user.getUsername() + " đã có trong nhóm '" + groupName + "'");
    }

    }
    // Remove user from group
    public boolean removeUser(String groupName , User user){
        if (!groupUsers.containsKey(groupName)) {
            System.out.println("Nhóm '" + groupName + "' không tồn tại.");
            return false;
        }

        List<User> groupListuser = groupUsers.get(groupName);
        boolean removed = groupListuser.remove(user);
        if (removed) {
            System.out.println("Đã xóa " + user.getUsername() + " khỏi nhóm '" + groupName + "'");
        } else {
            System.out.println("Không tìm thấy người dùng " + user.getUsername() + " trong nhóm '" + groupName + "'");
        }

       return true;
    }


    //my List users in group
    public void listUsersInGroup(String groupName) {
        if (!groupUsers.containsKey(groupName)) {
            System.out.println("Nhóm '" + groupName + "' không tồn tại.");
            return;
        }

        List<User> groupListuser = groupUsers.get(groupName);
        System.out.println("Người dùng trong nhóm '" + groupName + "':");
        for (User user : groupListuser) {
            System.out.println("- " + user);
        }
    }
    //
    public static void main(String[] args) {
        UserManager userManager = new UserManager();

        User user1 = new User("1", "Alice", "alice@example.com");
        User user2 = new User("2", "Bob", "bob@example.com");

        userManager.addUsertoGroup("General", user1);
        userManager.addUsertoGroup("Friends", user2);

        userManager.listUsersInGroup("General");
        userManager.listUsersInGroup("Friends");
        
        userManager.removeUser("General", user1);
        userManager.listUsersInGroup("General");
    }
}