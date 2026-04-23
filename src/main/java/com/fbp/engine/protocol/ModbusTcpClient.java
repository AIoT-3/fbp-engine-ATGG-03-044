package com.fbp.engine.protocol;

import com.fbp.engine.exception.ModbusException;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ModbusTcpClient {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final AtomicInteger transactionId = new AtomicInteger();
    private final String host;
    private final int port;

    public ModbusTcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(3000);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public void disconnect() throws IOException {
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public int[] readHoldingRegisters(int unitId, int startAddress, int quantity) throws IOException, ModbusException {
        int currentTransactionId = nextTransactionId();
        byte[] requestFrame = buildReadHoldingRegistersRequest(currentTransactionId, unitId, startAddress, quantity);
        out.write(requestFrame);
        out.flush();

        MbapHeader header = readMbapHeader();
        int respFunctionCode = in.readUnsignedByte();

        validateHeader(currentTransactionId, header.getTransactionId(), header.getProtocolId(), unitId, header.getUnitId());

        if ((respFunctionCode & 0x80) != 0) {
            int exceptionCode = in.readUnsignedByte();
            throw new ModbusException(respFunctionCode & 0x7F, exceptionCode);
        }

        if (respFunctionCode != 0x03) {
            throw new IOException("Unexpected function code: " + respFunctionCode);
        }

        int byteCount = in.readUnsignedByte();
        int registerCount = byteCount / 2;
        int[] registers = new int[registerCount];

        for (int i = 0; i < registerCount; i++) {
            registers[i] = in.readUnsignedShort();
        }

        return registers;
    }

    public void writeSingleRegister(int unitId, int address, int value) throws IOException, ModbusException {
        int currentTransactionId = nextTransactionId();
        byte[] requestFrame = buildWriteSingleRegisterRequest(currentTransactionId, unitId, address, value);
        out.write(requestFrame);
        out.flush();

        MbapHeader header = readMbapHeader();
        int respFunctionCode = in.readUnsignedByte();

        validateHeader(currentTransactionId, header.getTransactionId(), header.getProtocolId(), unitId, header.getUnitId());

        if ((respFunctionCode & 0x80) != 0) {
            int exceptionCode = in.readUnsignedByte();
            throw new ModbusException(respFunctionCode & 0x7F, exceptionCode);
        }

        if (respFunctionCode != 0x06) {
            throw new IOException("Unexpected function code: " + respFunctionCode);
        }

        int respAddress = in.readUnsignedShort();
        int respValue = in.readUnsignedShort();

        if (respAddress != address || respValue != value) {
            throw new IOException("Write response does not match request");
        }
    }

    byte[] buildReadHoldingRegistersRequest(int unitId, int startAddress, int quantity) throws IOException {
        int currentTransactionId = nextTransactionId();
        return buildReadHoldingRegistersRequest(currentTransactionId, unitId, startAddress, quantity);
    }

    byte[] buildWriteSingleRegisterRequest(int unitId, int address, int value) throws IOException {
        int currentTransactionId = nextTransactionId();
        return buildWriteSingleRegisterRequest(currentTransactionId, unitId, address, value);
    }

    private byte[] buildReadHoldingRegistersRequest(int transactionId, int unitId,
                                                    int startAddress, int quantity) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(buildMbapHeader(transactionId, 6, unitId));
        dos.writeByte(0x03);
        dos.writeShort(startAddress);
        dos.writeShort(quantity);

        return baos.toByteArray();
    }

    private byte[] buildWriteSingleRegisterRequest(int transactionId, int unitId,
                                                   int address, int value) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(buildMbapHeader(transactionId, 6, unitId));
        dos.writeByte(0x06);
        dos.writeShort(address);
        dos.writeShort(value);

        return baos.toByteArray();
    }

    private byte[] buildMbapHeader(int transactionId, int length, int unitId) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeShort(transactionId);
        dos.writeShort(0x0000);
        dos.writeShort(length);
        dos.writeByte(unitId);

        return baos.toByteArray();
    }

    private MbapHeader readMbapHeader() throws IOException {
        int respTransactionId = in.readUnsignedShort();
        int respProtocolId = in.readUnsignedShort();
        int respLength = in.readUnsignedShort();
        int respUnitId = in.readUnsignedByte();

        return new MbapHeader(respTransactionId, respProtocolId, respLength, respUnitId);
    }

    private int nextTransactionId() {
        return transactionId.incrementAndGet();
    }

    private void validateHeader(int expectedTransactionId, int respTransactionId,
                                int respProtocolId, int expectedUnitId, int respUnitId) throws IOException {
        if (respTransactionId != expectedTransactionId) {
            throw new IOException("Transaction ID mismatch");
        }
        if (respProtocolId != 0x0000) {
            throw new IOException("Invalid protocol ID");
        }
        if (respUnitId != expectedUnitId) {
            throw new IOException("Unit ID mismatch");
        }
    }

    @Data
    @RequiredArgsConstructor
    private static class MbapHeader {
        private final int transactionId;
        private final int protocolId;
        private final int length;
        private final int unitId;
    }
}
