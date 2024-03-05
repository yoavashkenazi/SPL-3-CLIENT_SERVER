package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<byte[]> connections;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        this.shouldTerminate = false;
        this.connectionId = connectionId;
        this.connections = connections;
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
        
    } 
    private void delrqPacketProccess (byte[] packet){

    }
    private void discPacketProccess (byte[] packet){
        
    } 

    
}
