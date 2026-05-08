package com.fbp.engine.integration;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ModbusReaderNode;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.node.MqttPublisherNode;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.RuleNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import com.fbp.engine.protocol.TestPorts;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CrossProtocolIntegrationTest {
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
    @DisplayName("MQTT -> Rule -> MODBUS")
    void MqttToModbusTest() throws Exception {
        MqttTestSupport.assumeBrokerAvailable();
        int port = TestPorts.nextPort();
        simulator = new ModbusTcpSimulator(port, 10);
        simulator.setRegister(2, 0);
        simulator.start();

        String inputTopic = "sensor/temp/" + UUID.randomUUID();
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "cross-mqtt-modbus-sub-" + UUID.randomUUID(),
                        "topic", inputTopic,
                        "qos", 1
                )
        );
        RuleNode ruleNode = new RuleNode("rule-1", "temperature > 30");
        ModbusWriterNode writerNode = new ModbusWriterNode(
                "modbus-writer-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "registerAddress", 2,
                        "fixedValue", 1
                )
        );
        Connection subscriberToRule = new LocalConnection();
        Connection ruleToWriter = new LocalConnection();
        Connection writerResult = new LocalConnection();

        subscriberNode.getOutputPort("out").connect(subscriberToRule);
        ruleNode.getOutputPort("match").connect(ruleToWriter);
        writerNode.getOutputPort("result").connect(writerResult);

        threads.add(TestNodeWorkerSupport.startWorker("cross-mqtt-rule", subscriberToRule, ruleNode, running));
        threads.add(TestNodeWorkerSupport.startWorker("cross-mqtt-writer", ruleToWriter, writerNode, running));

        subscriberNode.initialize();
        writerNode.initialize();
        closeables.add(subscriberNode::shutdown);
        closeables.add(writerNode::shutdown);

        MqttTestSupport.publish(inputTopic, "{\"temperature\":35.0}");

        assertNotNull(writerResult.poll());
        assertEquals(1, simulator.getRegister(2));
    }

    @Test
    @DisplayName("MODBUS -> Rule -> MQTT")
    void ModbusToMqttTest() throws Exception {
        MqttTestSupport.assumeBrokerAvailable();
        int port = TestPorts.nextPort();
        simulator = new ModbusTcpSimulator(port, 10);
        simulator.setRegister(0, 350);
        simulator.start();

        String alertTopic = "alert/temp/" + UUID.randomUUID();
        LinkedBlockingQueue<MqttTestSupport.ReceivedMessage> messages = new LinkedBlockingQueue<>();
        MqttAsyncClient alertSubscriber = MqttTestSupport.createSubscriber(alertTopic, 1, messages);
        closeables.add(() -> MqttTestSupport.close(alertSubscriber));

        ModbusReaderNode readerNode = new ModbusReaderNode(
                "modbus-reader-1",
                Map.of(
                        "host", "localhost",
                        "port", port,
                        "slaveId", 1,
                        "startAddress", 0,
                        "count", 1,
                        "registerMapping", Map.of(
                                "0", Map.of(
                                        "name", "temperature",
                                        "scale", 0.1
                                )
                        )
                )
        );
        RuleNode ruleNode = new RuleNode("rule-1", "temperature > 30");
        MqttPublisherNode publisherNode = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "cross-modbus-mqtt-pub-" + UUID.randomUUID(),
                        "topic", alertTopic,
                        "qos", 1,
                        "retained", false
                )
        );
        Connection readerToRule = new LocalConnection();
        Connection ruleToPublisher = new LocalConnection();

        readerNode.getOutputPort("out").connect(readerToRule);
        ruleNode.getOutputPort("match").connect(ruleToPublisher);

        threads.add(TestNodeWorkerSupport.startWorker("cross-modbus-rule", readerToRule, ruleNode, running));
        threads.add(TestNodeWorkerSupport.startWorker("cross-modbus-publisher", ruleToPublisher, publisherNode, running));

        readerNode.initialize();
        publisherNode.initialize();
        closeables.add(readerNode::shutdown);
        closeables.add(publisherNode::shutdown);

        readerNode.process(new Message(Map.of("trigger", true)));

        MqttTestSupport.ReceivedMessage received = messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(alertTopic, received.getTopic());
        assertTrue(received.getPayload().contains("\"temperature\":35.0"));
    }

    @Test
    @Disabled("Long-running manual stability test")
    @DisplayName("복합 플로우 안정성")
    void StabilityTest() throws Exception {
        MqttTestSupport.assumeBrokerAvailable();
        assertTrue(true);
    }
}
