package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TftpClient {
    // TODO: implement the main logic of the client, when using a thread per client
    // the main logic goes here

    public static void main(String[] args) throws UnknownHostException, IOException {
        if (args.length == 0) {
            args = new String[] { "localhost", "hello" };
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, message");
            System.exit(1);
        }
        Queue<String> userInputToProcess = new ConcurrentLinkedQueue<String>();
        try (Socket sock = new Socket(args[0], Integer.parseInt(args[1]));
                BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
                BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream())) {

            TftpClientProtocol protocol = new TftpClientProtocol();
            TftpClientEncoderDecoder encdec = new TftpClientEncoderDecoder();
            int read;
            System.out.println("connected to server!");
            // creates the inputThread and runs it
            (new Thread(new InputThread(protocol, userInputToProcess))).start();

            while (!protocol.shouldTerminate()) {
                // process messages from input queue (if there is packets to write)
                while (!protocol.shouldTerminate() && !userInputToProcess.isEmpty()
                        && protocol.currentOperation == OpCode.UNKOWN) {
                    byte[] response = protocol.processUserInput(userInputToProcess.poll());
                    if (response != null) {
                        // System.out.println("CH before write");
                        out.write(encdec.encode(response));
                        out.flush();
                        // System.out.println("CH after write");
                    }
                }

                // read
                if (in.available() > 0) {
                    // System.out.println("CH after avialable if");
                    read = in.read();
                    // System.out.println("CH after in.read");
                    byte[] nextMessage = encdec.decodeNextByte((byte) read);
                    if (nextMessage != null) {

                        byte[] response = protocol.process(nextMessage);
                        if (response != null) {
                            // System.out.println("CH before write");
                            out.write(encdec.encode(response));
                            out.flush();
                            // System.out.println("CH after write");
                        }
                    }
                }
            }
        }

    }
}
