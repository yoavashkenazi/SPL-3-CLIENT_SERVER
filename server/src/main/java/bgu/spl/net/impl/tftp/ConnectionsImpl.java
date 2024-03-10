package bgu.spl.net.impl.tftp;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class ConnectionsImpl<T> implements Connections<T> {
    public ConcurrentHashMap<Integer, ConnectionHandler<T>> idToConnectionHandler = new ConcurrentHashMap<>();

    @Override
    public boolean connect(int connectionId, ConnectionHandler<T> handler) {
        // TODO Auto-generated method stub
        return idToConnectionHandler.put(connectionId, (ConnectionHandler<T>) handler) != null;
    }

    @Override
    public boolean send(int connectionId, T msg) {
        // TODO Auto-generated method stub
        ConnectionHandler<T> ch = idToConnectionHandler.get(connectionId);
        if (ch == null) {
            return false;
        }
        ch.send(msg);
        return true;
    }

    @Override
    public void disconnect(int connectionId) {
        // TODO Auto-generated method stub
        idToConnectionHandler.remove(connectionId);
    }

}
