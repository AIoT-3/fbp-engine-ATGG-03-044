package com.fbp.engine.protocol;

import com.fbp.engine.exception.ModbusException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class ModbusTcpClientTest {
    private ModbusTcpSimulator simulator;
    private ModbusTcpClient client;

    @AfterEach
    void tearDown() throws Exception {
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @DisplayName("FC 03 요청 프레임 조립")
    void ReadFrameTest() throws Exception {
        client = new ModbusTcpClient("localhost", 5020);

        byte[] frame = client.buildReadHoldingRegistersRequest(1, 10, 5);

        assertEquals(0x01, frame[6] & 0xFF);
        assertEquals(0x03, frame[7] & 0xFF);
        assertEquals(0x00, frame[8] & 0xFF);
        assertEquals(0x0A, frame[9] & 0xFF);
        assertEquals(0x00, frame[10] & 0xFF);
        assertEquals(0x05, frame[11] & 0xFF);
    }

    @Test
    @DisplayName("FC 06 요청 프레임 조립")
    void WriteFrameTest() throws Exception {
        client = new ModbusTcpClient("localhost", 5020);

        byte[] frame = client.buildWriteSingleRegisterRequest(1, 5, 1234);

        assertEquals(0x01, frame[6] & 0xFF);
        assertEquals(0x06, frame[7] & 0xFF);
        assertEquals(0x00, frame[8] & 0xFF);
        assertEquals(0x05, frame[9] & 0xFF);
        assertEquals(0x04, frame[10] & 0xFF);
        assertEquals(0xD2, frame[11] & 0xFF);
    }

    @Test
    @DisplayName("MBAP 헤더 구조")
    void HeaderTest() throws Exception {
        client = new ModbusTcpClient("localhost", 5020);

        byte[] frame = client.buildReadHoldingRegistersRequest(1, 0, 1);

        assertEquals(0x00, frame[0] & 0xFF);
        assertEquals(0x01, frame[1] & 0xFF);
        assertEquals(0x00, frame[2] & 0xFF);
        assertEquals(0x00, frame[3] & 0xFF);
        assertEquals(0x00, frame[4] & 0xFF);
        assertEquals(0x06, frame[5] & 0xFF);
        assertEquals(0x01, frame[6] & 0xFF);
    }

    @Test
    @DisplayName("Transaction ID 증가")
    void TransactionTest() throws Exception {
        client = new ModbusTcpClient("localhost", 5020);

        byte[] first = client.buildReadHoldingRegistersRequest(1, 0, 1);
        byte[] second = client.buildReadHoldingRegistersRequest(1, 1, 1);

        assertEquals(1, toUnsignedShort(first[0], first[1]));
        assertEquals(2, toUnsignedShort(second[0], second[1]));
    }

    @Test
    @DisplayName("초기 상태")
    void StateTest() {
        client = new ModbusTcpClient("localhost", TestPorts.nextPort());
        assertFalse(client.isConnected());
    }

    @Test
    @Tag("integration")
    @DisplayName("연결/해제")
    void ConnectTest() throws Exception {
        startSimulatorWithValues(250, 600, 1, 0, 0);

        assertTrue(client.isConnected());

        client.disconnect();

        assertFalse(client.isConnected());
    }

    @Test
    @Tag("integration")
    @DisplayName("Holding Register 읽기")
    void ReadTest() throws Exception {
        startSimulatorWithValues(250, 600, 1, 0, 0);

        int[] values = client.readHoldingRegisters(1, 0, 3);

        assertArrayEquals(new int[]{250, 600, 1}, values);
    }

    @Test
    @Tag("integration")
    @DisplayName("다수 레지스터 읽기")
    void MultiReadTest() throws Exception {
        startSimulatorWithValues(100, 200, 300, 400, 500);

        int[] values = client.readHoldingRegisters(1, 0, 5);

        assertEquals(5, values.length);
        assertArrayEquals(new int[]{100, 200, 300, 400, 500}, values);
    }

    @Test
    @Tag("integration")
    @DisplayName("Single Register 쓰기")
    void WriteTest() throws Exception {
        startSimulatorWithValues(250, 600, 1, 0, 0);

        client.writeSingleRegister(1, 2, 100);

        assertEquals(100, simulator.getRegister(2));
    }

    @Test
    @Tag("integration")
    @DisplayName("쓰기 후 읽기")
    void ReadAfterWriteTest() throws Exception {
        startSimulatorWithValues(250, 600, 1, 0, 0);

        client.writeSingleRegister(1, 2, 100);
        int[] values = client.readHoldingRegisters(1, 0, 3);

        assertArrayEquals(new int[]{250, 600, 100}, values);
    }

    @Test
    @Tag("integration")
    @DisplayName("에러 응답 처리")
    void ErrorResponseTest() throws Exception {
        startSimulatorWithValues(250, 600, 1, 0, 0);

        ModbusException exception = assertThrows(
                ModbusException.class,
                () -> client.readHoldingRegisters(1, 99, 1)
        );

        assertEquals(ModbusException.ILLEGAL_DATA_ADDRESS, exception.getExceptionCode());
    }

    @Test
    @Tag("integration")
    @DisplayName("소켓 타임아웃")
    void TimeoutTest() throws Exception {
        int port = TestPorts.nextPort();
        ServerSocket serverSocket = new ServerSocket(port);

        Thread silentServer = new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                Thread.sleep(5000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        silentServer.start();

        client = new ModbusTcpClient("localhost", port);
        client.connect();

        assertThrows(SocketTimeoutException.class, () -> client.readHoldingRegisters(1, 0, 1));

        serverSocket.close();
        silentServer.join();
    }

    private void startSimulatorWithValues(int... values) throws Exception {
        int port = TestPorts.nextPort();
        simulator = new ModbusTcpSimulator(port, 10);
        for (int i = 0; i < values.length; i++) {
            simulator.setRegister(i, values[i]);
        }
        simulator.start();
        Thread.sleep(200);
        client = new ModbusTcpClient("localhost", port);
        client.connect();
    }

    private int toUnsignedShort(byte highByte, byte lowByte) {
        int high = highByte & 0xFF;
        int low = lowByte & 0xFF;
        return high * 256 + low;
    }
}
