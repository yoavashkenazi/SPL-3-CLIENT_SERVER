package bgu.spl.net.impl.tftp;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class ConnectionsImpl<T> implements Connections<T> {
    int idCounter = 0;
    Object counterLock = new Object();
    ConcurrentHashMap<Integer, ConnectionHandler<byte[]>> idToConnectionHandler = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, String> idToUsername = new ConcurrentHashMap<>();
    // ConcurrentHashMap<String, Integer> usernameToID = new ConcurrentHashMap<>();

    @Override
    public boolean connect(int connectionId, ConnectionHandler<T> handler) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'connect'");
    }

    @Override
    public boolean send(int connectionId, T msg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'send'");
    }

    @Override
    public void disconnect(int connectionId) {
        // TODO Auto-generated method stub
        this.idToConnectionHandler.remove(connectionId);
        this.idToUsername.remove(connectionId);
    }

    public int allocateId() {
        synchronized (this.counterLock) {
            return this.idCounter++;
        }
    }

    public boolean isUsernameLoggedIn(String username) {
        return this.idToUsername.contains(username);
    }

    public void registerUsername(int connectionId, String username) {
        idToUsername.put(connectionId, username);
    }

    public boolean isClientConnected(int connectionId) {
        return this.idToConnectionHandler.containsKey(connectionId);
    }
}
