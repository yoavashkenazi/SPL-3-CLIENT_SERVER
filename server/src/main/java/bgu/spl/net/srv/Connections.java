package bgu.spl.net.srv;

import java.io.IOException;

public interface Connections<T> {

    boolean connect(int connectionId, ConnectionHandler<T> handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);

    public int allocateId();

    public boolean isUsernameLoggedIn(String username);

    public void registerUsername(int connectionId, String username);

    public boolean isClientConnected(int connectionId);
}
