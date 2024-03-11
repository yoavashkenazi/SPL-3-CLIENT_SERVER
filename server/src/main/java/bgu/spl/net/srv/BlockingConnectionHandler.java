package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.tftp.UsersHolder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private Connections<T> connections;
    private int connectionId;
    private Queue<byte[]> packetsToWrite;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol,
            Connections<T> connections) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.connections = connections;
        this.connectionId = -1;
        this.packetsToWrite = new ConcurrentLinkedQueue<byte[]>();
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { // just for automatic closing
            int read;
            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());
            this.connectionId = UsersHolder.allocateId();
            this.protocol.start(this.connectionId, connections, this);
            this.connections.connect(connectionId, this);
            //System.out.println("CH before while");
            while (!protocol.shouldTerminate() && connected) {
                //System.out.println("CH after first while");
                // write if there is packets to write
                while (!protocol.shouldTerminate() && connected && !packetsToWrite.isEmpty()) {
                    System.out.println("CH before write: ");
                    System.out.println(Arrays.toString(packetsToWrite.peek()));
                    out.write(packetsToWrite.poll());
                    out.flush();
                    System.out.println("CH after write");
                }
               //System.out.println("CH after while");
                if (in.available() > 0) {
                    //System.out.println("CH after avialable if");
                    read = in.read();
                    //System.out.println("CH after in.read");
                    T nextMessage = encdec.decodeNextByte((byte) read);
                    if (nextMessage != null) {
                        System.out.println(Arrays.toString((byte[]) nextMessage));
                        protocol.process(nextMessage);
                        // if DISC Packet was received
                        if (this.protocol.shouldTerminate()){
                            byte[] discAckPacket = new byte[]{0,4,0,0};
                            out.write(discAckPacket);
                            out.flush();
                        }
                    }
                }
            }
            System.out.println("after big while");

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        this.packetsToWrite.add(encdec.encode(msg));
    }

    // פלו: הפרוטוקול מפעיל את סנד של קונקשיין, והוא מפעיל את סנד של קונקשן הנדלר,
    // שהוא מוסיף לתור של השליחה - השערה שלי.
}
