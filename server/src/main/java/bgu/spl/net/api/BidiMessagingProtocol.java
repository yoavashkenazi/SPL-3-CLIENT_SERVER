package bgu.spl.net.api;

import java.io.IOException;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public interface BidiMessagingProtocol<T> {
    /**
     * Used to initiate the current client protocol with it's personal connection ID
     * and the connections implementation
     **/
    void start(int connectionId, Connections<T> connections);

    void process(T message) throws IOException;

    /**
     * @return true if the connection should be terminated
     */
    boolean shouldTerminate();
}
