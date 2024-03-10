package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;

public class TftpClient {
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here

    private static boolean terminate = false; 

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"localhost", "hello"};
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, message");
            System.exit(1);
        }
        try (Socket sock = new Socket(args[0], 7777);
                BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
                BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream())) {

            while(!terminate)

            System.out.println("sending message to server");
            out.write(args[1]);
            out.newLine();
            out.flush();

            System.out.println("awaiting response");
            String line = in.readLine();
            System.out.println("message from server: " + line);
    }

    public static boolean shouldTerminate(){
        return terminate;
    }

}

