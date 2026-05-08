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

class ModbusWriterNodeTest {
    private int port;
    private ModbusWriterNode modbusWriterNode;
    private ModbusTcpSimulator simulator;

    @BeforeEach
    void setUp() {
        port = TestPorts.nextPort();
        modbusWriterNode = new ModbusWriterNode(
                "modbus-writer-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "registerAddress", 2,
                        "valueField", "temperature",
                        "scale", 1.0
                )
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        modbusWriterNode.shutdown();
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @DisplayName("포트 구성")
    void PortTest() {
        assertNotNull(modbusWriterNode.getInputPort("in"));
    }

    @Test
    @DisplayName("초기 상태")
    void StateTest() {
        assertFalse(modbusWriterNode.isConnected());
    }

    @Test
    @DisplayName("config 확인")
    void ConfigTest() {
        assertEquals(2, modbusWriterNode.getConfig("registerAddress"));
        assertEquals("temperature", modbusWriterNode.getConfig("valueField"));
    }

    @Test
    @Tag("integration")
    @DisplayName("연결 성공")
    void ConnectTest() throws Exception {
        startSimulator();

        modbusWriterNode.initialize();

        assertTrue(modbusWriterNode.isConnected());
    }

    @Test
    @Tag("integration")
    @DisplayName("레지스터 쓰기")
    void WriteTest() throws Exception {
        startSimulator();

        modbusWriterNode.initialize();
        modbusWriterNode.process(new Message(Map.of("temperature", 100)));

        assertEquals(100, simulator.getRegister(2));
    }

    @Test
    @Tag("integration")
    @DisplayName("스케일 변환")
    void ScaleTest() throws Exception {
        modbusWriterNode = new ModbusWriterNode(
                "modbus-writer-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "registerAddress", 2,
                        "valueField", "temperature",
                        "scale", 10.0
                )
        );
        startSimulator();

        modbusWriterNode.initialize();
        modbusWriterNode.process(new Message(Map.of("temperature", 25.5)));

        assertEquals(255, simulator.getRegister(2));
    }

    @Test
    @Tag("integration")
    @DisplayName("shutdown 후 연결 해제")
    void ShutdownTest() throws Exception {
        startSimulator();

        modbusWriterNode.initialize();
        modbusWriterNode.shutdown();

        assertFalse(modbusWriterNode.isConnected());
    }

    @Test
    @Tag("integration")
    @DisplayName("결과 포트")
    void ResultTest() throws Exception {
        startSimulator();
        Connection resultConnection = new LocalConnection();
        modbusWriterNode.getOutputPort("result").connect(resultConnection);

        modbusWriterNode.initialize();
        modbusWriterNode.process(new Message(Map.of("temperature", 77)));

        Message received = resultConnection.poll();

        assertEquals(Integer.valueOf(77), received.get("writtenValue"));
    }

    private void startSimulator() throws Exception {
        simulator = new ModbusTcpSimulator(port, 10);
        simulator.start();
        Thread.sleep(200);
    }
}
