package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Dictionary;

import javax.xml.crypto.dsig.keyinfo.KeyValue;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<byte[]> connections;
    private ConnectionHandler<byte[]> connectionHandler;

    @Override
    public void start(int connectionId, Connections<byte[]> connections, ConnectionHandler<byte[]> connectionHandler) {
        // TODO implement this
        this.shouldTerminate = false;
        this.connectionId = connectionId;
        this.connections = connections;
        this.connectionHandler = connectionHandler;
        //ConnectionsImpl.ids_login.put(connectionId, true);
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        OpCode opcode = OpCode.fromOrdinal(message[1]);
        switch (opcode) {
            case RRQ:
                this.rrqPacketProccess(message);
                break;
            case WRQ:
                this.wrqPacketProccess(message);
                break;
            case DATA:
                this.dataPacketProccess(message);
                break;
            case ACK:
                this.ackPacketProccess(message);
                break;
            case DIRQ:
                this.dirqPacketProccess(message);
                break;
            case LOGRQ:
                this.logrqPacketProccess(message);
                break;
            case DELRQ:
                this.delrqPacketProccess(message);
                break;
            case DISC:
                this.discPacketProccess(message);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        // this.connections.disconnect(this.connectionId);
        // //holder stuff
        // return shouldTerminate;
        return false;
    } 

    private void rrqPacketProccess (byte[] packet){
        
    } 
    private void wrqPacketProccess (byte[] packet){
        
    } 
    private void dataPacketProccess (byte[] packet){

    }
    private void ackPacketProccess (byte[] packet){
        
    }
    private void dirqPacketProccess (byte[] packet){
        
    } 
    private void logrqPacketProccess (byte[] packet){
        String username = getNameFromMessage(packet);
        if (this.connections.isUsernameLoggedIn(username)){
            this.connections.send(connectionId, generateERROR(7));
        }
        else{
            this.connections.connect(connectionId, this.connectionHandler);
            this.connections.registerUsername(connectionId, username);
            this.connections.send(connectionId, generateACK(null));
        }
    } 
    private void delrqPacketProccess (byte[] packet){

    }
    private void discPacketProccess (byte[] packet){
        
    }

    private String byteToString(byte[] message){
        return new String(message, StandardCharsets.UTF_8);
    }
    private String getNameFromMessage(byte[] message){
        return byteToString(Arrays.copyOfRange(message, 2, message.length));
    }
    private byte[] generateACK(byte[] blockNum){
        byte[] ackPacket = new byte[4];
        ackPacket[0] = 0;
        ackPacket[1] = 4;
        if (blockNum==null || blockNum.length<2){
            ackPacket[2] = 0;
            ackPacket[3] = 0;
        }
        else{
            ackPacket[2] = blockNum[0];
            ackPacket[3] = blockNum[1];
        }
        return ackPacket;
    }
    private byte[] generateERROR(int errorCode){}
    
    
    
     
}
