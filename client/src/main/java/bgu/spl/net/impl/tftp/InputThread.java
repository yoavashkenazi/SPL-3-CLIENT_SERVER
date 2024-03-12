package bgu.spl.net.impl.tftp;

import java.util.Queue;
import java.util.Scanner;

public class InputThread implements Runnable {
    private Queue<String> userInputToProcess;
    private TftpClientProtocol protocol;

    public InputThread(TftpClientProtocol protocol, Queue<String> userInputToProcess) {
        this.protocol = protocol;
        this.userInputToProcess = userInputToProcess;
    }

    public void run() {
        Scanner sc = new Scanner(System.in); // Create a Scanner object
        while (!protocol.shouldTerminate()) {
            String input = sc.nextLine();
            userInputToProcess.add(input);
        }
        sc.close();
    }

}
