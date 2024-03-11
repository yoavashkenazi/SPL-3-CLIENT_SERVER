package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder

    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    /**
     * the opcode of the decoded message.
     */
    OpCode opcode;
    /**
     * the size of the data packet(if the )
     */
    int dataPacketSize = 0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // TODO: implement this
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        if (len == 1){
            opcode = OpCode.fromOrdinal(nextByte);
        }
        if (len>=1){
            switch (opcode) {
                case RRQ:
                case WRQ:
                case LOGRQ:
                case DELRQ:
                    if (nextByte == 0) {
                        return popPacket();
                    }
                    break;
                case DISC:
                case DIRQ:
                    pushByte(nextByte); 
                    return popPacket();
                case DATA:
                    if (len == 3){
                        dataPacketSize = 16*bytes[len-1]+nextByte;
                    }
                    else if(len == dataPacketSize+5){
                        pushByte(nextByte);
                        return popPacket();
                    }
                    break;
                case ACK:
                    if (len==3){
                        pushByte(nextByte);
                        return popPacket();
                    }
                    break;
                default:
                    break;
            }
        }
        pushByte(nextByte);
        return null; //not a line yet
    }

    @Override
    public byte[] encode(byte[] message) {
        //TODO: implement this
        OpCode encodeOpcode = OpCode.fromOrdinal(message[1]);
        switch (encodeOpcode) {
            case ERROR:
            case BCAST:
                byte[] output = Arrays.copyOf(message, message.length+1);
                output [output.length-1] = 0;
                return output;
            default:
                break;
        }
        return message;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }

    private byte[] popPacket() {
        //notice that we explicitly requesting that the string will be decoded from UTF-8
        //this is not actually required as it is the default encoding in java.
        byte[] slicedArray = Arrays.copyOfRange(bytes, 0, len);
        len = 0;
        return slicedArray;
    }
}

/**
 * enum representing the opcode of the packet
 */
enum OpCode {
    UNKOWN,
    RRQ,
    WRQ,
    DATA,
    ACK,
    ERROR,
    DIRQ,
    LOGRQ,
    DELRQ,
    BCAST,
    DISC;

    private static OpCode[] allValues = values();
    public static OpCode fromOrdinal(int n) {
        try{
            return allValues[n];
        } catch(Exception ex) {return allValues[0];}
  }
}
