package bgu.spl.net.impl.tftp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.net.api.MessagingProtocol;

public class TftpClientProtocol implements MessagingProtocol<byte[]> {
    private volatile boolean shouldTerminate = false;
    private Queue<byte[]> fileChunksToSend;
    private Queue<byte[]> fileChunksReceived;
    private String readFileName;
    private String writeFileName;
    private final String dirPath = "./";
    public OpCode currentOperation = OpCode.UNKOWN;

    public byte[] process(byte[] message) {
        // TODO implement this
        OpCode opcode = OpCode.fromOrdinal(message[1]);
        System.out.println("client protocol proccess, before switch, opcode: " + opcode);

        switch (opcode) {
            case DATA:
                return processData(message);

            case DISC:

            case ACK:
                return processACK(message);
            case ERROR:
                processError(message);
                return null;
            case BCAST:
                processBcast(message);
                return null;
            default:
                return null;
        }
    }

    public boolean shouldTerminate() {
        return this.shouldTerminate;
    }

    public byte[] processUserInput(String input) {
        // process user input and return the appopriate byte array
        String opcodeStr = "";
        try {
            opcodeStr = input.split(" ")[0];
        } catch (Exception ex) {
            System.out.println("invalid command1");
            return null;
        }

        switch (opcodeStr) {
            case "RRQ":
                return processRRQPacket(input);
            case "WRQ":
                return processWRQPacket(input);
            case "DIRQ":
                return processDIRQPacket(input);
            case "LOGRQ":
                return processLOGRQPacket(input);
            case "DELRQ":
                return processDELRQPacket(input);
            case "DISC":
                return processDISCPacket(input);
            default:
                System.out.println("invalid command");
                return null;
        }
    }

    public byte[] processACK(byte[] message) {
        System.out.println("ACK " + getACKBlockNum(message));
        switch (this.currentOperation) {
            case WRQ:
                if (this.fileChunksToSend != null && !this.fileChunksToSend.isEmpty()) {
                    return this.fileChunksToSend.remove();
                } else if (this.fileChunksToSend != null && this.fileChunksToSend.isEmpty()) {
                    System.out.println("WRQ " + this.writeFileName + " complete");
                    this.currentOperation = OpCode.UNKOWN;
                }
            case LOGRQ:
                this.currentOperation = OpCode.UNKOWN;
                System.out.println("connection successfull");
                return null;
            case DELRQ:
                this.currentOperation = OpCode.UNKOWN;
                return null;
            case DISC:
                this.currentOperation = OpCode.UNKOWN;
                System.out.println("disconnection successfull");
                this.shouldTerminate = true;
                return null;
            default:
                this.currentOperation = OpCode.UNKOWN;
                return null;
        }
    }

    private void processError(byte[] message) {
        int errorNum = ((short) (message[3]) & 0x00ff);

        String description = "";
        try {
            description = new String(Arrays.copyOfRange(message, 4, message.length), "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }

        System.out.println("Error " + errorNum + " " + description);

        switch (this.currentOperation) {
            case RRQ:
                // if file not found delete the file created
                if (errorNum == 1 || errorNum == 2) {
                    Path filePath = Paths.get(this.dirPath, this.readFileName);
                    try {
                        Files.delete(filePath);
                    } catch (IOException e) {

                    }
                    this.readFileName = null;
                }
                break;
            case WRQ:
                // if file already exists in the server/access violation, clear the file chunks
                // to send
                if (errorNum == 2 || errorNum == 5) {
                    this.fileChunksToSend.clear();
                    this.writeFileName = null;
                }
                break;
            case DISC:
                this.shouldTerminate = true;
                break;
            default:
                break;
        }

        this.currentOperation = OpCode.UNKOWN;
    }

    public byte[] processData(byte[] message) {

        // adding the packet to the queue
        this.fileChunksReceived.add(message);
        byte[] blockNum = new byte[2];
        blockNum[0] = message[4];
        blockNum[1] = message[5];
        if (message.length < 518) {
            // making the file from the queue of DATA packets
            byte[] fileData = concatPacketsToByteArray(fileChunksReceived);

            switch (this.currentOperation) {
                case RRQ:
                    // saving the file
                    Path filePath = Paths.get(this.dirPath, this.readFileName);
                    try {
                        Files.write(filePath, fileData);
                    } catch (Exception e) {
                    }
                    break;
                case DIRQ:
                    int j = 0;
                    for (int i = 0; i < fileData.length; i++) {
                        if (fileData[i] == 0) {
                            try {
                                System.out.println(new String(Arrays.copyOfRange(fileData, j, i), "UTF-8"));
                            } catch (UnsupportedEncodingException ex) {

                            }
                            j = i + 1;
                        }
                    }

                    break;
                default:
                    break;

            }
            this.currentOperation = OpCode.UNKOWN;
        }
        // sending the ACK packet
        return generateACK(blockNum);

    }

    private byte[] processRRQPacket(String userInput) {
        String fileName = "";
        try {
            fileName = userInput.split(" ", 2)[1];
        } catch (Exception ex) {
            System.out.println("invalid command");
            return null;
        }
        if (isFileInFolder(fileName)) {
            System.out.println("File already exists");
            return null;
        }

        byte[] fileNameInBytes = fileName.getBytes(StandardCharsets.UTF_8);
        byte[] packet = new byte[2 + fileNameInBytes.length];
        packet[0] = 0;
        packet[1] = (byte) 1;
        System.arraycopy(fileNameInBytes, 0, packet, 2, fileNameInBytes.length);

        this.readFileName = fileName;
        this.fileChunksReceived = new ConcurrentLinkedQueue<>();

        this.currentOperation = OpCode.RRQ;
        return packet;
    }

    private byte[] processWRQPacket(String userInput) {
        String fileName = "";
        try {
            fileName = userInput.split(" ", 2)[1];
        } catch (Exception ex) {
            System.out.println("invalid command");
            return null;
        }

        if (isFileInFolder(fileName)) {
            // creates the packet
            byte[] fileNameInBytes = fileName.getBytes(StandardCharsets.UTF_8);
            byte[] packet = new byte[2 + fileNameInBytes.length];
            packet[0] = 0;
            packet[1] = (byte) 2;
            System.arraycopy(fileNameInBytes, 0, packet, 2, fileNameInBytes.length);
            this.fileChunksToSend = splitFileIntoPackets(this.dirPath + fileName, 512);
            this.currentOperation = OpCode.WRQ;
            this.writeFileName = fileName;
            return packet;
        } else {
            System.out.println("file does not exist");
            return null;
        }

    }

    private byte[] processDIRQPacket(String userInput) {
        try {
            String message = userInput.split(" ", 2)[1];
            System.out.println("invalid command");
            return null;
        } catch (Exception ex) {

        }
        this.currentOperation = OpCode.DIRQ;
        return new byte[] { 0, 6 };
    }

    private byte[] processLOGRQPacket(String userInput) {
        String username = "";
        try {
            username = userInput.split(" ", 2)[1];
        } catch (Exception ex) {
            System.out.println("invalid command");
            return null;
        }
        this.currentOperation = OpCode.LOGRQ;
        byte[] usernameInBytes = username.getBytes(StandardCharsets.UTF_8);
        byte[] packet = new byte[2 + usernameInBytes.length];
        packet[0] = 0;
        packet[1] = (byte) 7;
        System.arraycopy(usernameInBytes, 0, packet, 2, usernameInBytes.length);
        return packet;
    }

    private byte[] processDELRQPacket(String userInput) {
        String fileName = "";
        try {
            fileName = userInput.split(" ", 2)[1];
        } catch (Exception ex) {
            System.out.println("invalid command");
            return null;
        }

        this.currentOperation = OpCode.DELRQ;
        byte[] fileNameInBytes = fileName.getBytes(StandardCharsets.UTF_8);
        byte[] packet = new byte[2 + fileNameInBytes.length];
        packet[0] = 0;
        packet[1] = (byte) 8;
        System.arraycopy(fileNameInBytes, 0, packet, 2, fileNameInBytes.length);
        return packet;
    }

    private byte[] processDISCPacket(String userInput) {
        try {
            String message = userInput.split(" ", 2)[1];
            System.out.println("invalid command");
            return null;
        } catch (Exception ex) {
        }
        this.currentOperation = OpCode.DISC;
        return new byte[] { 0, 10 };
    }

    // returns the block number.
    private short getACKBlockNum(byte[] message) {
        return (short) (((short) message[2]) << 8 | (short) (message[3]) & 0x00ff);
    }

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

    public static Queue<byte[]> splitFileIntoPackets(String filePath, int packetSize) {
        Queue<byte[]> packets = new LinkedList<>();
        Path file = Paths.get(filePath);
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(file);
        } catch (Exception ex) {
            return null;
        }
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

    private static byte[] createPacket(int blockNumber, byte[] packetData) {
        int packetSize = packetData.length + 6; // 6 bytes for headers

        // Use ByteBuffer for efficient manipulation of byte arrays
        ByteBuffer buffer = ByteBuffer.allocate(packetSize);
        buffer.put((byte) 0); // Start byte
        buffer.put((byte) 3); // 3-byte header
        buffer.putShort((short) packetData.length); // Size of data
        buffer.putShort((short) blockNumber); // Block number
        buffer.put(packetData); // Data

        return buffer.array();
    }

    public boolean isFileInFolder(String fileName) {
        Path folder = Paths.get(this.dirPath);
        Path fileToSearch = folder.resolve(fileName);

        if (Files.exists(fileToSearch) && Files.isRegularFile(fileToSearch)) {
            return true;
        }

        return false;
    }

    private void processBcast(byte[] message) {
        String bcastType = "del";
        if (message[2] == (byte) 1) {
            bcastType = "add";
        }

        String fileName;
        try {
            fileName = new String(Arrays.copyOfRange(message, 3, message.length), "UTF-8");
            System.out.println("BCAST " + bcastType + " " + fileName);
        } catch (UnsupportedEncodingException e) {
        }

    }
}