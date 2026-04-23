package com.fbp.engine.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ModbusTcpSimulator {
    private final int port;
    private final int[] registers;
    private final Set<Socket> clientSockets = ConcurrentHashMap.newKeySet();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public ModbusTcpSimulator(int port, int registerCount) {
        this.port = port;
        this.registers = new int[registerCount];
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSockets.add(clientSocket);
                    Thread clientThread = new Thread(() -> handleClient(clientSocket));
                    clientThread.start();
                } catch (IOException e) {
                    if (running) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, "modbus-simulator-accept");
        acceptThread.start();
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        for (Socket clientSocket : clientSockets) {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
        }
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
    }

    public void setRegister(int address, int value) {
        validateAddress(address);
        registers[address] = value;
    }

    public int getRegister(int address) {
        validateAddress(address);
        return registers[address];
    }

    public void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            while (running && !socket.isClosed()) {
                int transactionId = input.readUnsignedShort();
                int protocolId = input.readUnsignedShort();
                int length = input.readUnsignedShort();
                int unitId = input.readUnsignedByte();
                int functionCode = input.readUnsignedByte();

                if (protocolId != 0x0000) {
                    sendException(output, transactionId, unitId, functionCode, 0x04);
                    skipRemaining(input, length - 2);
                    continue;
                }

                if (functionCode == 0x03) {
                    int startAddress = input.readUnsignedShort();
                    int quantity = input.readUnsignedShort();
                    handleReadHoldingRegisters(output, transactionId, unitId, startAddress, quantity);
                } else if (functionCode == 0x06) {
                    int address = input.readUnsignedShort();
                    int value = input.readUnsignedShort();
                    handleWriteSingleRegister(output, transactionId, unitId, address, value);
                } else {
                    skipRemaining(input, length - 2);
                    sendException(output, transactionId, unitId, functionCode, 0x01);
                }
            }
        } catch (EOFException e) {
            // The client closed the socket after finishing its request.
        } catch (IOException e) {
            if (running) {
                throw new RuntimeException(e);
            }
        } finally {
            clientSockets.remove(clientSocket);
        }
    }

    private void handleReadHoldingRegisters(DataOutputStream output, int transactionId, int unitId,
                                            int startAddress, int quantity) throws IOException {
        if (quantity <= 0 || startAddress < 0 || startAddress + quantity > registers.length) {
            sendException(output, transactionId, unitId, 0x03, 0x02);
            return;
        }

        int byteCount = quantity * 2;

        output.writeShort(transactionId);
        output.writeShort(0x0000);
        output.writeShort(3 + byteCount);
        output.writeByte(unitId);
        output.writeByte(0x03);
        output.writeByte(byteCount);

        for (int i = 0; i < quantity; i++) {
            output.writeShort(registers[startAddress + i]);
        }
        output.flush();
    }

    private void handleWriteSingleRegister(DataOutputStream output, int transactionId, int unitId,
                                           int address, int value) throws IOException {
        if (address < 0 || address >= registers.length) {
            sendException(output, transactionId, unitId, 0x06, 0x02);
            return;
        }

        registers[address] = value;

        output.writeShort(transactionId);
        output.writeShort(0x0000);
        output.writeShort(6);
        output.writeByte(unitId);
        output.writeByte(0x06);
        output.writeShort(address);
        output.writeShort(value);
        output.flush();
    }

    private void sendException(DataOutputStream output, int transactionId, int unitId,
                               int functionCode, int exceptionCode) throws IOException {
        output.writeShort(transactionId);
        output.writeShort(0x0000);
        output.writeShort(3);
        output.writeByte(unitId);
        output.writeByte(functionCode | 0x80);
        output.writeByte(exceptionCode);
        output.flush();
    }

    private void skipRemaining(DataInputStream input, int bytesToSkip) throws IOException {
        for (int i = 0; i < bytesToSkip; i++) {
            input.readUnsignedByte();
        }
    }

    private void validateAddress(int address) {
        if (address < 0 || address >= registers.length) {
            throw new IllegalArgumentException("Invalid register address: " + address);
        }
    }
}
