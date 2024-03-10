package bgu.spl.net.impl.tftp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class UsersHolder {
    private static int idCounter = 0;
    private static Object counterLock = new Object();
    private static ConcurrentHashMap<Integer, String> idToUsername = new ConcurrentHashMap<>();

    public int allocateId() {
        synchronized (counterLock) {
            return idCounter++;
        }
    }

    public static boolean isUsernameLoggedIn(String username) {
        return idToUsername.contains(username);
    }

    public static void registerUser(int connectionId, String username) {
        idToUsername.put(connectionId, username);
    }

    public static void removeUser(int connectionId) {
        idToUsername.remove(connectionId);
    }

    public static boolean isClientConnected(int connectionId) {
        return idToUsername.containsKey(connectionId);
    }

    public static ArrayList<Integer> getConnectedUsersIds() {
        return Collections.list(idToUsername.keys());
    }
}
