package com.fbp.engine.protocol;

import com.fbp.engine.exception.ModbusException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ModbusTcpSimulatorTest {
    private ModbusTcpSimulator simulator;

    @AfterEach
    void tearDown() throws Exception {
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @Tag("integration")
    @DisplayName("시작/종료")
    void StartStopTest() throws Exception {
        int port = TestPorts.nextPort();
        simulator = new ModbusTcpSimulator(port, 10);
        simulator.start();

        ModbusTcpClient client = new ModbusTcpClient("localhost", port);
        client.connect();
        assertTrue(client.isConnected());
        client.disconnect();

        simulator.stop();

        ModbusTcpClient secondClient = new ModbusTcpClient("localhost", port);
        assertThrows(IOException.class, secondClient::connect);
    }

    @Test
    @DisplayName("레지스터 초기값")
    void RegisterTest() {
        simulator = new ModbusTcpSimulator(TestPorts.nextPort(), 10);
        simulator.setRegister(0, 250);

        assertEquals(250, simulator.getRegister(0));
    }

    @Test
    @Tag("integration")
    @DisplayName("FC 03 응답")
    void ReadResponseTest() throws Exception {
        int port = TestPorts.nextPort();
        simulator = new ModbusTcpSimulator(port, 10);
        simulator.setRegister(0, 250);
        simulator.setRegister(1, 600);
        simulator.start();
        Thread.sleep(200);

        ModbusTcpClient client = new ModbusTcpClient("localhost", port);
        client.connect();
        int[] values = client.readHoldingRegisters(1, 0, 2);

        assertArrayEquals(new int[]{250, 600}, values);
        client.disconnect();
    }

    @Test
    @Tag("integration")
    @DisplayName("FC 06 응답")
    void WriteResponseTest() throws Exception {
        int port = TestPorts.nextPort();
        simulator = new ModbusTcpSimulator(port, 10);
        simulator.start();
        Thread.sleep(200);

        ModbusTcpClient client = new ModbusTcpClient("localhost", port);
        client.connect();
        client.writeSingleRegister(1, 2, 100);

        assertEquals(100, simulator.getRegister(2));
        client.disconnect();
    }

    @Test
    @Tag("integration")
    @DisplayName("잘못된 주소 에러")
    void InvalidAddressTest() throws Exception {
        int port = TestPorts.nextPort();
        simulator = new ModbusTcpSimulator(port, 10);
        simulator.start();
        Thread.sleep(200);

        ModbusTcpClient client = new ModbusTcpClient("localhost", port);
        client.connect();

        ModbusException exception = assertThrows(
                ModbusException.class,
                () -> client.readHoldingRegisters(1, 99, 1)
        );

        assertEquals(ModbusException.ILLEGAL_DATA_ADDRESS, exception.getExceptionCode());
        client.disconnect();
    }

    @Test
    @Tag("integration")
    @DisplayName("다중 클라이언트")
    void MultiClientTest() throws Exception {
        int port = TestPorts.nextPort();
        simulator = new ModbusTcpSimulator(port, 10);
        simulator.setRegister(0, 250);
        simulator.setRegister(1, 600);
        simulator.start();
        Thread.sleep(200);

        int[][] results = new int[2][];

        Thread first = new Thread(() -> {
            try {
                ModbusTcpClient client = new ModbusTcpClient("localhost", port);
                client.connect();
                results[0] = client.readHoldingRegisters(1, 0, 2);
                client.disconnect();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread second = new Thread(() -> {
            try {
                ModbusTcpClient client = new ModbusTcpClient("localhost", port);
                client.connect();
                results[1] = client.readHoldingRegisters(1, 0, 2);
                client.disconnect();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        first.start();
        second.start();
        first.join(2000);
        second.join(2000);

        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
        assertArrayEquals(new int[]{250, 600}, results[0]);
        assertArrayEquals(new int[]{250, 600}, results[1]);
    }
}
