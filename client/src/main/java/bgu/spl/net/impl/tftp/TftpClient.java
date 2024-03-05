package bgu.spl.net.impl.tftp;

import java.util.Arrays;

public class TftpClient {
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    public static void main(String[] args) {
        System.out.println("implement me!");
    }

    //use it to encode messeges
    // OpCode encodeOpcode = OpCode.valueOf(String.valueOf(message[1]));
    //     switch (encodeOpcode) {
    //         case RRQ:
    //         case WRQ:
    //         case LOGRQ:
    //         case DELRQ:
    //             byte[] output = Arrays.copyOf(message, message.length+1);
    //             output [output.length-1] = 0;
    //             return output;
    //         default:
    //             break;
    //     }
}
