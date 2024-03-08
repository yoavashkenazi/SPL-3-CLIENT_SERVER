package bgu.spl.net.impl.tftp;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.crypto.dsig.keyinfo.KeyValue;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<byte[]> connections;
    private ConnectionHandler<byte[]> connectionHandler;
    // queues to aggregate the data packets
    private Queue<byte[]> fileChunksToSend;
    private Queue<byte[]> fileChunksReceived;

    @Override
    public void start(int connectionId, Connections<byte[]> connections, ConnectionHandler<byte[]> connectionHandler) {
        // TODO implement this
        this.shouldTerminate = false;
        this.connectionId = connectionId;
        this.connections = connections;
        this.connectionHandler = connectionHandler;
        this.fileChunksToSend = null;
        this.fileChunksReceived = null;
        // ConnectionsImpl.ids_login.put(connectionId, true);
    }

    @Override
    public void process(byte[] message) throws IOException {
        // TODO implement this
        OpCode opcode = OpCode.fromOrdinal(message[1]);
        // check if user logged in
        if (opcode != OpCode.LOGRQ && !this.connections.isClientConnected(this.connectionId)) {
            this.connections.send(this.connectionId, this.generateERROR(ERROR_TYPE.USER_NOT_LOGGED_IN));
            return;
        }
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

    // Proccessing of each packet type

    private void rrqPacketProccess(byte[] packet) throws IOException {
        String fileName = getNameFromMessage(packet);
        if (!isFileInFolder(fileName)) {
            this.connections.send(connectionId, generateERROR(ERROR_TYPE.FILE_NOT_FOUND));
        } else {
            this.fileChunksToSend = splitFileIntoPackets("./server/Flies/" + fileName, 512);
            if (this.fileChunksToSend != null && !this.fileChunksToSend.isEmpty()) {
                connections.send(connectionId, this.fileChunksToSend.remove());
            }
        }
    }

    private void wrqPacketProccess(byte[] packet) {
        String fileName = getNameFromMessage(packet);
        if (isFileInFolder(fileName)) {
            this.connections.send(connectionId, generateERROR(ERROR_TYPE.FILE_EXISTS));
        } else {
            this.connections.send(connectionId, generateACK(null));
            this.fileChunksReceived = new ConcurrentLinkedQueue<>();
        }
    }

    private void dataPacketProccess(byte[] packet) {
        this.fileChunksReceived.add(packet);
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) this.fileChunksReceived.size());
        this.connections.send(connectionId, generateACK(buffer.array()));
        if () {
            // make file from queue
        }

    }

    private void ackPacketProccess(byte[] packet) {
        if (getACKBlockNum(packet) > 0 && this.fileChunksToSend != null && !this.fileChunksToSend.isEmpty()) {
            connections.send(connectionId, this.fileChunksToSend.remove());
        }
    }

    private void dirqPacketProccess(byte[] packet) {

    }

    private void logrqPacketProccess(byte[] packet) {
        String username = getNameFromMessage(packet);
        if (this.connections.isUsernameLoggedIn(username)) {
            this.connections.send(connectionId, generateERROR(ERROR_TYPE.USER_ALREADY_LOGGED_IN));
        } else {
            this.connections.connect(connectionId, this.connectionHandler);
            this.connections.registerUsername(connectionId, username);
            this.connections.send(connectionId, generateACK(null));
        }
    }

    private void delrqPacketProccess(byte[] packet) {

    }

    private void discPacketProccess(byte[] packet) {
        // sending ACK
        this.connections.send(this.connectionId, generateACK(null));
        // removing the client from the maps
        this.connections.disconnect(this.connectionId);
        // terminating the Connection handler.
        this.shouldTerminate = true;
    }

    // returns the string represnatation of the byte array
    private String byteToString(byte[] message) {
        return new String(message, StandardCharsets.UTF_8);
    }

    // returns the block number.
    private short getACKBlockNum(byte[] message) {
        return (short) (((short) message[2]) << 8 | (short) (message[3]) & 0x00ff);
    }

    private String getNameFromMessage(byte[] message) {
        return byteToString(Arrays.copyOfRange(message, 2, message.length));
    }

    private byte[] generateACK(byte[] blockNum) {
        byte[] ackPacket = new byte[4];
        ackPacket[0] = 0;
        ackPacket[1] = 4;
        if (blockNum == null || blockNum.length < 2) {
            ackPacket[2] = 0;
            ackPacket[3] = 0;
        } else {
            ackPacket[2] = blockNum[0];
            ackPacket[3] = blockNum[1];
        }
        return ackPacket;
    }

    private byte[] generateERROR(ERROR_TYPE errorType) {
        String message = "";
        byte errorCode = 0;
        switch (errorType) {
            case NOT_DEFINED:
                message = "not defined";
                errorCode = 0;
                break;
            case FILE_NOT_FOUND:
                message = "file not found";
                errorCode = 1;
                break;
            case ACCESS_VIOLATION:
                message = "access violation";
                errorCode = 2;
                break;
            case DISK_FULL:
                message = "disk is full";
                errorCode = 3;
                break;
            case ILLEGAL_OPERATION:
                message = "illegal operation";
                errorCode = 4;
                break;
            case FILE_EXISTS:
                message = "file exists";
                errorCode = 5;
                break;
            case USER_NOT_LOGGED_IN:
                message = "user not logged in";
                errorCode = 6;
                break;
            case USER_ALREADY_LOGGED_IN:
                message = "user already logged in";
                errorCode = 7;
                break;
            default:
                message = "not defined";
                errorCode = 0;
                break;
        }

        byte[] message_bytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[5 + message_bytes.length];
        result[0] = 0;
        result[1] = (byte) 5;
        result[2] = 0;
        result[1] = errorCode;
        result[result.length - 1] = 0;
        return result;
    }

    public static boolean isFileInFolder(String fileName) {
        Path folder = Paths.get("/server/Flies");
        Path fileToSearch = folder.resolve(fileName);

        if (Files.exists(fileToSearch) && Files.isRegularFile(fileToSearch)) {
            return true;
        }

        return false;
    }

    public static Queue<byte[]> splitFileIntoPackets(String filePath, int packetSize) throws IOException {
        Queue<byte[]> packets = new LinkedList<>();
        Path file = Paths.get(filePath);

        byte[] fileContent = Files.readAllBytes(file);
        int blockNumber = 1;

        for (int i = 0; i < fileContent.length; i += packetSize) {
            int endIndex = Math.min(i + packetSize, fileContent.length);
            byte[] packetData = new byte[endIndex - i];
            System.arraycopy(fileContent, i, packetData, 0, packetData.length);

            // Create a packet with headers and add it to the queue
            byte[] packet = createPacket(blockNumber, packetData);
            packets.add(packet);

            blockNumber++;
        }

        // If the file size is divisible by 512, add an empty last packet
        if (fileContent.length % packetSize == 0) {
            byte[] emptyPacket = createPacket(blockNumber, new byte[0]);
            packets.add(emptyPacket);
        }

        return packets;
    }

    /**
     * Creates a packet with headers.
     *
     * @param blockNumber The block number for the packet.
     * @param packetData  The data to be included in the packet.
     * @return The packet with headers.
     */
    private static byte[] createPacket(int blockNumber, byte[] packetData) {
        int packetSize = packetData.length + 6; // 6 bytes for headers

        // Use ByteBuffer for efficient manipulation of byte arrays
        ByteBuffer buffer = ByteBuffer.allocate(packetSize);
        buffer.put((byte) 0); // Start byte
        buffer.putInt(3); // 3-byte header
        buffer.putShort((short) packetData.length); // Size of data
        buffer.putShort((short) blockNumber); // Block number
        buffer.put(packetData); // Data

        return buffer.array();
    }

    enum ERROR_TYPE {
        NOT_DEFINED,
        FILE_NOT_FOUND,
        ACCESS_VIOLATION,
        DISK_FULL,
        ILLEGAL_OPERATION,
        FILE_EXISTS,
        USER_NOT_LOGGED_IN,
        USER_ALREADY_LOGGED_IN;

        private static ERROR_TYPE[] allValues = values();

        public static ERROR_TYPE fromOrdinal(int n) {
            try {
                return allValues[n];
            } catch (Exception ex) {
                return allValues[0];
            }
        }
    }
}
