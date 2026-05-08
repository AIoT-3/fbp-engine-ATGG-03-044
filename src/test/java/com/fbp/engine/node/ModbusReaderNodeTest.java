package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import com.fbp.engine.protocol.TestPorts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModbusReaderNodeTest {
    private int port;
    private ModbusReaderNode modbusReaderNode;
    private ModbusTcpSimulator simulator;

    @BeforeEach
    void setUp() {
        port = TestPorts.nextPort();
        modbusReaderNode = new ModbusReaderNode(
                "modbus-reader-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "startAddress", 0,
                        "count", 3
                )
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        modbusReaderNode.shutdown();
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @DisplayName("포트 구성")
    void PortTest() {
        assertNotNull(modbusReaderNode.getInputPort("trigger"));
        assertNotNull(modbusReaderNode.getOutputPort("out"));
        assertNotNull(modbusReaderNode.getOutputPort("error"));
    }

    @Test
    @DisplayName("초기 상태")
    void StateTest() {
        assertFalse(modbusReaderNode.isConnected());
    }

    @Test
    @DisplayName("config 확인")
    void ConfigTest() {
        assertEquals("localhost", modbusReaderNode.getConfig("host"));
        assertEquals(port, modbusReaderNode.getConfig("port"));
        assertEquals(1, modbusReaderNode.getConfig("slaveId"));
    }

    @Test
    @Tag("integration")
    @DisplayName("연결 성공")
    void ConnectTest() throws Exception {
        startSimulator(250, 600, 1);

        modbusReaderNode.initialize();

        assertTrue(modbusReaderNode.isConnected());
    }

    @Test
    @Tag("integration")
    @DisplayName("레지스터 읽기")
    void ReadTest() throws Exception {
        startSimulator(250, 600, 1);
        Connection outConnection = new LocalConnection();
        modbusReaderNode.getOutputPort("out").connect(outConnection);

        modbusReaderNode.initialize();
        modbusReaderNode.process(new Message());

        Message received = outConnection.poll();
        Map<String, Object> registers = received.get("registers");

        assertEquals(250, registers.get("0"));
        assertEquals(600, registers.get("1"));
        assertEquals(1, registers.get("2"));
    }

    @Test
    @Tag("integration")
    @DisplayName("registerMapping 적용")
    void MappingTest() throws Exception {
        modbusReaderNode = new ModbusReaderNode(
                "modbus-reader-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "startAddress", 0,
                        "count", 2,
                        "registerMapping", Map.of(
                                "0", Map.of("name", "temperature", "scale", 0.1),
                                "1", Map.of("name", "humidity", "scale", 0.1)
                        )
                )
        );
        startSimulator(250, 600, 1);
        Connection outConnection = new LocalConnection();
        modbusReaderNode.getOutputPort("out").connect(outConnection);

        modbusReaderNode.initialize();
        modbusReaderNode.process(new Message());

        Message received = outConnection.poll();

        assertEquals(25.0, received.get("temperature"));
        assertEquals(60.0, received.get("humidity"));
    }

    @Test
    @Tag("integration")
    @DisplayName("읽기 실패 시 에러 포트")
    void ErrorTest() throws Exception {
        modbusReaderNode = new ModbusReaderNode(
                "modbus-reader-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "startAddress", 99,
                        "count", 1
                )
        );
        startSimulator(250, 600, 1);
        Connection errorConnection = new LocalConnection();
        modbusReaderNode.getOutputPort("error").connect(errorConnection);

        modbusReaderNode.initialize();
        modbusReaderNode.process(new Message());

        Message received = errorConnection.poll();

        assertTrue(received.hasKey("error"));
    }

    @Test
    @Tag("integration")
    @DisplayName("shutdown 후 연결 해제")
    void ShutdownTest() throws Exception {
        startSimulator(250, 600, 1);

        modbusReaderNode.initialize();
        modbusReaderNode.shutdown();

        assertFalse(modbusReaderNode.isConnected());
    }

    private void startSimulator(int... values) throws Exception {
        simulator = new ModbusTcpSimulator(port, 10);
        for (int i = 0; i < values.length; i++) {
            simulator.setRegister(i, values[i]);
        }
        simulator.start();
        Thread.sleep(200);
    }
}
