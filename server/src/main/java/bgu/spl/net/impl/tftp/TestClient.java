package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.ByteBuffer;

public class TestClient {
    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            args = new String[] { "localhost", "hello" };
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, message");
            System.exit(1);
        }
        System.out.println("client started");
        // BufferedReader and BufferedWriter automatically using UTF-8 encoding
        try (Socket sock = new Socket("127.0.0.1", 7777);
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream())) {
            out.write(new byte[] { 0, 1, 9, 78, 10, 0 });
            out.write(new byte[] { 0, 2, 2, 3, 4, 0 });
            out.write(new byte[] { 0, 3, 0, 3, 0, 1, 99, 98, 97 });
            out.write(new byte[] { 0, 4, 55, 67 });
            // out.write(new byte[]{0,5,11,22,123,0});
            out.write(new byte[] { 0, 6 });
            out.write(new byte[] { 0, 7, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77, 77,
                    77, 77, 77, 77, 77, 77, 77, 77, 0 });
            out.write(new byte[] { 0, 8, 88, 88, 88, 88, 88, 88, 88, 88, 88, 88, 0 });
            out.write(new byte[] { 0, 10 });
        }
    }
}
