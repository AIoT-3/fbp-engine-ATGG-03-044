package com.fbp.engine.integration;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.node.MqttPublisherNode;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.RuleNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import com.fbp.engine.protocol.TestPorts;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class MqttModbusIntegrationTest {
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
    @DisplayName("MQTT 수신 -> Rule 분기")
    void RuleBranchTest() throws Exception {
        MqttTestSupport.assumeBrokerAvailable();

        String inputTopic = "sensor/temp/" + UUID.randomUUID();
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-modbus-branch-sub-" + UUID.randomUUID(),
                        "topic", inputTopic,
                        "qos", 1
                )
        );
        RuleNode ruleNode = new RuleNode("rule-1", "temperature > 30");
        Connection subscriberToRule = new LocalConnection();
        Connection matchConnection = new LocalConnection();
        Connection mismatchConnection = new LocalConnection();

        subscriberNode.getOutputPort("out").connect(subscriberToRule);
        ruleNode.getOutputPort("match").connect(matchConnection);
        ruleNode.getOutputPort("mismatch").connect(mismatchConnection);

        threads.add(TestNodeWorkerSupport.startWorker("mqtt-modbus-branch-rule", subscriberToRule, ruleNode, running));
        subscriberNode.initialize();
        closeables.add(subscriberNode::shutdown);

        Thread.sleep(500); // 구독 준비 대기
        MqttTestSupport.publish(inputTopic, "{\"temperature\":35.0}");
        MqttTestSupport.publish(inputTopic, "{\"temperature\":25.0}");

        Message match = matchConnection.poll();
        Message mismatch = mismatchConnection.poll();

        assertEquals(35.0, match.get("temperature"));
        assertEquals(25.0, mismatch.get("temperature"));
    }

    @Test
    @DisplayName("Rule match -> MODBUS 쓰기")
    void RuleToModbusTest() throws Exception {
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
                        "clientId", "mqtt-modbus-writer-sub-" + UUID.randomUUID(),
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

        threads.add(TestNodeWorkerSupport.startWorker("mqtt-modbus-writer-rule", subscriberToRule, ruleNode, running));
        threads.add(TestNodeWorkerSupport.startWorker("mqtt-modbus-writer-node", ruleToWriter, writerNode, running));

        subscriberNode.initialize();
        writerNode.initialize();
        closeables.add(subscriberNode::shutdown);
        closeables.add(writerNode::shutdown);

        Thread.sleep(500); // 구독 준비 대기
        MqttTestSupport.publish(inputTopic, "{\"temperature\":35.0}");

        Message result = writerResult.poll();

        assertNotNull(result);
        assertEquals(1, simulator.getRegister(2));
    }

    @Test
    @DisplayName("Rule match -> MQTT 알림")
    void RuleToMqttTest() throws Exception {
        MqttTestSupport.assumeBrokerAvailable();

        String inputTopic = "sensor/temp/" + UUID.randomUUID();
        String alertTopic = "alert/temp/" + UUID.randomUUID();
        LinkedBlockingQueue<MqttTestSupport.ReceivedMessage> messages = new LinkedBlockingQueue<>();
        MqttAsyncClient alertSubscriber = MqttTestSupport.createSubscriber(alertTopic, 1, messages);
        closeables.add(() -> MqttTestSupport.close(alertSubscriber));

        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-alert-sub-" + UUID.randomUUID(),
                        "topic", inputTopic,
                        "qos", 1
                )
        );
        RuleNode ruleNode = new RuleNode("rule-1", "temperature > 30");
        MqttPublisherNode publisherNode = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-alert-pub-" + UUID.randomUUID(),
                        "topic", alertTopic,
                        "qos", 1,
                        "retained", false
                )
        );
        Connection subscriberToRule = new LocalConnection();
        Connection ruleToPublisher = new LocalConnection();

        subscriberNode.getOutputPort("out").connect(subscriberToRule);
        ruleNode.getOutputPort("match").connect(ruleToPublisher);

        threads.add(TestNodeWorkerSupport.startWorker("mqtt-alert-rule", subscriberToRule, ruleNode, running));
        // RuleNode가 message를 그대로 통과시키므로 'topic' 필드가 남아 publisher가 원래 topic으로 발행하게 됨.
        // topic 키를 제거한 후 publisherNode로 전달한다.
        threads.add(new Thread(() -> {
            while (running.get()) {
                try {
                    Message msg = ruleToPublisher.poll();
                    publisherNode.process(msg.withoutKey("topic").withoutKey("mqttTimestamp"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "mqtt-alert-publisher"));
        threads.get(threads.size() - 1).start();

        subscriberNode.initialize();
        publisherNode.initialize();
        closeables.add(subscriberNode::shutdown);
        closeables.add(publisherNode::shutdown);

        Thread.sleep(500); // 구독 준비 대기
        MqttTestSupport.publish(inputTopic, "{\"temperature\":35.0}");

        MqttTestSupport.ReceivedMessage received = messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(alertTopic, received.getTopic());
        assertTrue(received.getPayload().contains("\"temperature\":35.0"));
    }

    @Test
    @DisplayName("End-to-End 흐름")
    void EndToEndTest() throws Exception {
        MqttTestSupport.assumeBrokerAvailable();
        int port = TestPorts.nextPort();
        simulator = new ModbusTcpSimulator(port, 10);
        simulator.setRegister(2, 0);
        simulator.start();

        String inputTopic = "sensor/temp/" + UUID.randomUUID();
        String alertTopic = "alert/temp/" + UUID.randomUUID();
        LinkedBlockingQueue<MqttTestSupport.ReceivedMessage> messages = new LinkedBlockingQueue<>();
        MqttAsyncClient alertSubscriber = MqttTestSupport.createSubscriber(alertTopic, 1, messages);
        closeables.add(() -> MqttTestSupport.close(alertSubscriber));

        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-e2e-sub-" + UUID.randomUUID(),
                        "topic", inputTopic,
                        "qos", 1
                )
        );
        RuleNode ruleNode = new RuleNode("rule-1", "temperature > 30");
        MqttPublisherNode publisherNode = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-e2e-pub-" + UUID.randomUUID(),
                        "topic", alertTopic,
                        "qos", 1,
                        "retained", false
                )
        );
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
        Connection ruleToPublisher = new LocalConnection();
        Connection ruleToWriter = new LocalConnection();
        Connection writerResult = new LocalConnection();

        subscriberNode.getOutputPort("out").connect(subscriberToRule);
        ruleNode.getOutputPort("match").connect(ruleToPublisher);
        ruleNode.getOutputPort("match").connect(ruleToWriter);
        writerNode.getOutputPort("result").connect(writerResult);

        threads.add(TestNodeWorkerSupport.startWorker("mqtt-e2e-rule", subscriberToRule, ruleNode, running));
        // RuleNode가 message를 그대로 통과시키므로 'topic' 필드가 남아 publisher가 원래 topic으로 발행하게 됨.
        // topic 키를 제거한 후 publisherNode로 전달한다.
        threads.add(new Thread(() -> {
            while (running.get()) {
                try {
                    Message msg = ruleToPublisher.poll();
                    publisherNode.process(msg.withoutKey("topic").withoutKey("mqttTimestamp"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "mqtt-e2e-publisher"));
        threads.get(threads.size() - 1).start();
        threads.add(TestNodeWorkerSupport.startWorker("mqtt-e2e-writer", ruleToWriter, writerNode, running));

        subscriberNode.initialize();
        publisherNode.initialize();
        writerNode.initialize();
        closeables.add(subscriberNode::shutdown);
        closeables.add(publisherNode::shutdown);
        closeables.add(writerNode::shutdown);

        Thread.sleep(500); // 구독 준비 대기
        MqttTestSupport.publish(inputTopic, "{\"temperature\":35.0}");

        MqttTestSupport.ReceivedMessage received = messages.poll(5, TimeUnit.SECONDS);
        Message result = writerResult.poll();

        assertNotNull(received);
        assertNotNull(result);
        assertEquals(1, simulator.getRegister(2));
    }
}
