package bgu.spl.net.impl.tftp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
    private String fileRecivedName;

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
        // if user is logged in, proccesses the message by it type.
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
        // gets the name of the requested file
        String fileName = getNameFromMessage(packet);
        // if the file doesnt exist, sends error
        if (!isFileInFolder(fileName)) {
            this.connections.send(connectionId, generateERROR(ERROR_TYPE.FILE_NOT_FOUND));
        } else {
            // splits the file data into DATA packets and sends the first packet
            this.fileChunksToSend = splitFileIntoPackets("./server/Flies/" + fileName, 512);
            if (this.fileChunksToSend != null && !this.fileChunksToSend.isEmpty()) {
                connections.send(connectionId, this.fileChunksToSend.remove());
            }
        }
    }

    private void wrqPacketProccess(byte[] packet) {
        // gets the name of the requested file
        this.fileRecivedName = getNameFromMessage(packet);
        // if the file name already exist, sends error
        if (isFileInFolder(this.fileRecivedName)) {
            this.connections.send(connectionId, generateERROR(ERROR_TYPE.FILE_EXISTS));
        }
        // else, sends the ACK packet and creates the queue that will hold the incoming
        // DATA packets
        else {
            this.connections.send(connectionId, generateACK(null));
            this.fileChunksReceived = new ConcurrentLinkedQueue<>();
        }
    }

    private void dataPacketProccess(byte[] packet) throws IOException {
        // adding the packet to the queue
        this.fileChunksReceived.add(packet);
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) this.fileChunksReceived.size());
        // sending the ACK packet
        this.connections.send(connectionId, generateACK(buffer.array()));
        // if this is the last data packet of the file, saves it to the directory and
        // send BCAST.
        if (packet.length < 518) {
            // making the file from the queue of DATA packets
            byte[] fileData = concatPacketsToByteArray(fileChunksReceived);
            // saving the file
            Path filePath = Paths.get("./server/Flies/", this.fileRecivedName);
            Files.write(filePath, fileData);
            // sending BCAST packet to all connected clients.
            this.broadcast(this.generateBCAST(1, this.fileRecivedName));
        }

    }

    private void ackPacketProccess(byte[] packet) {
        // if there are more packets to send, sends the next one.
        if (getACKBlockNum(packet) > 0 && this.fileChunksToSend != null && !this.fileChunksToSend.isEmpty()) {
            connections.send(connectionId, this.fileChunksToSend.remove());
        }
    }

    private void dirqPacketProccess(byte[] packet) throws IOException {

        directoryIntoFile();
        // splits the file data into DATA packets and sends the first packet
        this.fileChunksToSend = splitFileIntoPackets("./server/Flies/" + "DIRQNAMES", 512);
        if (this.fileChunksToSend != null && !this.fileChunksToSend.isEmpty()) {
            connections.send(connectionId, this.fileChunksToSend.remove());
        }
        Path filePath = Paths.get("./server/Flies/", "DIRQNAMES");
        Files.delete(filePath);
    }

    private void logrqPacketProccess(byte[] packet) {
        String username = getNameFromMessage(packet);
        // if the user name is already taken, sends error
        if (UsersHolder.isUsernameLoggedIn(username)) {
            this.connections.send(connectionId, generateERROR(ERROR_TYPE.USER_ALREADY_LOGGED_IN));
        }
        // if the user name is available, connects the client and register the user name
        else {
            this.connections.connect(connectionId, this.connectionHandler);
            UsersHolder.registerUser(connectionId, username);
            // sends ACK packet
            this.connections.send(connectionId, generateACK(null));
        }
    }

    private void delrqPacketProccess(byte[] packet) throws IOException {
        // gets the name of the requested file
        String fileToDeleteName = getNameFromMessage(packet);
        // if the file doesnt exist, sends error
        if (!isFileInFolder(fileToDeleteName)) {
            this.connections.send(connectionId, generateERROR(ERROR_TYPE.FILE_NOT_FOUND));
        } else {
            // delets the file and sends ACK, BCAST
            Path filePath = Paths.get("./server/Flies/", fileToDeleteName);
            Files.delete(filePath);
            this.connections.send(connectionId, generateACK(null));
            this.broadcast(generateBCAST(1, fileToDeleteName));
        }

    }

    private void discPacketProccess(byte[] packet) {
        // sending ACK
        this.connections.send(this.connectionId, generateACK(null));
        // removing the client from the maps
        this.connections.disconnect(this.connectionId);
        // remove from the UserHolder
        UsersHolder.removeUser(connectionId);
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

    // returns the name of the file/username from the packet
    private String getNameFromMessage(byte[] message) {
        return byteToString(Arrays.copyOfRange(message, 2, message.length));
    }

    /**
     * generates the appropriate ACK packet
     */
    private byte[] generateACK(byte[] blockNum) {
        byte[] ackPacket = new byte[4];
        ackPacket[0] = 0;
        ackPacket[1] = 4;
        // if the ACK is not for DATA packet
        if (blockNum == null || blockNum.length < 2) {
            ackPacket[2] = 0;
            ackPacket[3] = 0;
        } else {
            ackPacket[2] = blockNum[0];
            ackPacket[3] = blockNum[1];
        }
        return ackPacket;
    }

    /**
     * generates the appropriate ERROR packet
     */
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

    /**
     * returns a queue of DATA packets of the file
     */
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

    /**
     * Concatenates data packets into a byte array.
     *
     * @param packets The queue of data packets.
     * @return The concatenated byte array.
     */
    public static byte[] concatPacketsToByteArray(Queue<byte[]> packets) {
        int totalSize = calculateTotalSize(packets);
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        while (!packets.isEmpty()) {
            byte[] packet = packets.poll();

            // Skip empty packets
            if (packet.length > 6) {
                extractDataFromPacket(packet, buffer);
            }
        }

        return buffer.array();
    }

    private static int calculateTotalSize(Queue<byte[]> packets) {
        int totalSize = 0;

        for (byte[] packet : packets) {
            // Exclude the 6 bytes for headers if the packet is not empty
            if (packet.length > 6) {
                totalSize += packet.length - 6;
            }
        }

        return totalSize;
    }

    private static void extractDataFromPacket(byte[] packet, ByteBuffer buffer) {
        // Skip the first 6 bytes (headers) and copy the rest to the buffer
        buffer.put(packet, 6, packet.length - 6);
    }

    /**
     * Generates a BCAST packet.
     *
     * @param indicator An indicator value.
     * @param fileName  The file name.
     * @return A BCAST packet as a byte array.
     */
    private byte[] generateBCAST(int indicator, String fileName) {
        // Convert the fileName to UTF-8 bytes
        byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        // Create a byte array to hold the BCAST packet
        byte[] bcastPacket = new byte[3 + fileNameBytes.length + 1];
        // Populate the BCAST packet
        bcastPacket[0] = 0;
        bcastPacket[1] = 9;
        bcastPacket[2] = (byte) indicator;
        // Copy the fileNameBytes to the packet
        System.arraycopy(fileNameBytes, 0, bcastPacket, 3, fileNameBytes.length);
        // Add a 0 byte after the fileName
        bcastPacket[bcastPacket.length - 1] = 0;
        return bcastPacket;
    }

    private void directoryIntoFile() throws IOException {
        // Create a File object representing the specified directory path
        File directory = new File("./server/Flies/");

        // Create a File object for the output file in the specified directory
        File outputFile = new File(directory, "DIRQNAMES");

        // Use try-with-resources to ensure proper resource management (closes the
        // writer automatically)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // Retrieve an array of File objects representing the files in the directory
            File[] files = directory.listFiles();
            // Check if any files were found in the directory
            if (files != null) {
                // Iterate through the files in the directory
                for (int i = 0; i < files.length; i++) {
                    // Get the name of the current file
                    String fileNameStr = files[i].getName();
                    // Write the file name to the output file
                    writer.write(fileNameStr);
                    // Separate file names with a 0 byte, except for the last file name
                    if (i < files.length - 1) {
                        writer.write((char) 0);
                    }
                }
            }
        }
    }

    // to protocol
    public void broadcast(byte[] BCASTPacket) {
        // sends bcast message to all connected clients
        for (Integer connectionId : UsersHolder.getConnectedUsersIds()) {
            connections.send(connectionId, BCASTPacket);
        }
    }
}
