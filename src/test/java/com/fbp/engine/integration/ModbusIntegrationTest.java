package com.fbp.engine.integration;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ModbusReaderNode;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import com.fbp.engine.protocol.TestPorts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ModbusIntegrationTest {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final List<Thread> threads = new ArrayList<>();
    private final List<AutoCloseable> closeables = new ArrayList<>();
    private ModbusTcpSimulator simulator;

    @AfterEach
    void tearDown() throws Exception {
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }
        TestNodeWorkerSupport.stopWorkers(running, threads.toArray(new Thread[0]));
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @DisplayName("Reader -> 레지스터 읽기")
    void ReaderTest() throws Exception {
        int port = TestPorts.nextPort();
        simulator = startSimulator(port, 250, 600, 1);

        ModbusReaderNode readerNode = new ModbusReaderNode(
                "modbus-reader-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "startAddress", 0,
                        "count", 3
                )
        );
        Connection outputConnection = new Connection();
        readerNode.getOutputPort("out").connect(outputConnection);

        readerNode.initialize();
        closeables.add(readerNode::shutdown);
        readerNode.process(new Message(Map.of("trigger", true)));

        Message received = outputConnection.poll();
        Map<String, Object> registers = received.get("registers");

        assertEquals(250, registers.get("0"));
        assertEquals(600, registers.get("1"));
        assertEquals(1, registers.get("2"));
    }

    @Test
    @DisplayName("Writer -> 레지스터 쓰기")
    void WriterTest() throws Exception {
        int port = TestPorts.nextPort();
        simulator = startSimulator(port, 250, 600, 0);

        ModbusWriterNode writerNode = new ModbusWriterNode(
                "modbus-writer-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "registerAddress", 2,
                        "valueField", "controlCode",
                        "scale", 1.0
                )
        );

        writerNode.initialize();
        closeables.add(writerNode::shutdown);
        writerNode.process(new Message(Map.of("controlCode", 1)));

        assertEquals(1, simulator.getRegister(2));
    }

    @Test
    @DisplayName("Reader -> Writer 파이프라인")
    void ReaderWriterPipelineTest() throws Exception {
        int port = TestPorts.nextPort();
        simulator = startSimulator(port, 250, 600, 0);

        ModbusReaderNode readerNode = new ModbusReaderNode(
                "modbus-reader-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "startAddress", 0,
                        "count", 1,
                        "registerMapping", Map.of(
                                "0", Map.of("name", "temperature")
                        )
                )
        );
        ModbusWriterNode writerNode = new ModbusWriterNode(
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
        Connection readerToWriter = new Connection();
        Connection resultConnection = new Connection();

        readerNode.getOutputPort("out").connect(readerToWriter);
        writerNode.getOutputPort("result").connect(resultConnection);
        threads.add(TestNodeWorkerSupport.startWorker("modbus-reader-writer-thread", readerToWriter, writerNode, running));

        readerNode.initialize();
        writerNode.initialize();
        closeables.add(readerNode::shutdown);
        closeables.add(writerNode::shutdown);

        readerNode.process(new Message(Map.of("trigger", true)));

        assertNotNull(resultConnection.poll());
        assertEquals(250, simulator.getRegister(2));
    }

    @Test
    @DisplayName("연결 끊김 처리")
    void DisconnectHandlingTest() throws Exception {
        int port = TestPorts.nextPort();
        simulator = startSimulator(port, 250, 600, 0);

        ModbusReaderNode readerNode = new ModbusReaderNode(
                "modbus-reader-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "startAddress", 0,
                        "count", 1
                )
        );
        Connection errorConnection = new Connection();
        readerNode.getOutputPort("error").connect(errorConnection);

        readerNode.initialize();
        closeables.add(readerNode::shutdown);
        simulator.stop();
        simulator = null;

        readerNode.process(new Message(Map.of("trigger", true)));

        Message error = errorConnection.poll();

        assertNotNull(error.get("error"));
    }

    private ModbusTcpSimulator startSimulator(int port, int... values) throws Exception {
        ModbusTcpSimulator started = new ModbusTcpSimulator(port, 10);
        for (int i = 0; i < values.length; i++) {
            started.setRegister(i, values[i]);
        }
        started.start();
        return started;
    }
}
