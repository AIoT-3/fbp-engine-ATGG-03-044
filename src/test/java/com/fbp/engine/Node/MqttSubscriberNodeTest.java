package com.fbp.engine.Node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.CollectorNode;
import com.fbp.engine.node.MqttSubscriberNode;
import org.eclipse.paho.mqttv5.client.DisconnectedBufferOptions;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MqttSubscriberNodeTest {
    private TestMqttSubscriberNode mqttSubscriberNode;

    static class TestMqttSubscriberNode extends MqttSubscriberNode {
        public TestMqttSubscriberNode(String id, Map<String, Object> config) {
            super(id, config);
        }

        public Map<String, Object> parse(String payload) {
            return parsePayload(payload);
        }

        public Message convert(String topic, String payload) {
            return toMessage(topic, payload);
        }
    }

    @BeforeEach
    void setUp() {
        mqttSubscriberNode = new TestMqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-subscriber-test",
                        "topic", "sensor/temp",
                        "qos", 1
                )
        );
    }

    @Test
    @DisplayName("포트 구성")
    void PortTest() {
        assertNotNull(mqttSubscriberNode.getOutputPort("out"));
    }

    @Test
    @DisplayName("초기 상태")
    void StateTest() {
        assertFalse(mqttSubscriberNode.isConnected());
    }

    @Test
    @DisplayName("config 조회")
    void ConfigTest() {
        assertEquals("tcp://localhost:1883", mqttSubscriberNode.getConfig("brokerUrl"));
    }

    @Test
    @DisplayName("JSON -> Message 변환")
    void JsonTest() {
        Map<String, Object> payload = mqttSubscriberNode.parse("{\"value\": 28.5, \"unit\": \"C\"}");

        assertEquals(28.5, payload.get("value"));
        assertEquals("C", payload.get("unit"));
    }

    @Test
    @DisplayName("JSON 파싱 실패 처리")
    void RawPayloadTest() {
        Map<String, Object> payload = mqttSubscriberNode.parse("{invalid json}");

        assertEquals("{invalid json}", payload.get("rawPayload"));
    }

    @Test
    @Tag("integration")
    @DisplayName("Broker 연결 성공")
    void ConnectTest() {
        String topic = "sensor/temp/" + UUID.randomUUID();
        TestMqttSubscriberNode node = new TestMqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-subscriber-connect-" + UUID.randomUUID(),
                        "topic", topic,
                        "qos", 1
                )
        );

        node.initialize();

        assertTrue(node.isConnected());

        node.shutdown();
    }

    @Test
    @Tag("integration")
    @DisplayName("메시지 수신")
    void ReceiveTest() throws Exception {
        String topic = "sensor/temp/" + UUID.randomUUID();
        TestMqttSubscriberNode node = new TestMqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-subscriber-receive-" + UUID.randomUUID(),
                        "topic", topic,
                        "qos", 1
                )
        );
        CollectorNode collectorNode = new CollectorNode("collector-1");
        Connection connection = new Connection();

        node.getOutputPort("out").connect(connection);
        node.initialize();

        publish(topic, "{\"temperature\": 31.5, \"unit\": \"C\"}");

        Message received = connection.poll();
        collectorNode.process(received);

        assertEquals(1, collectorNode.getCollected().size());
        assertEquals(31.5, collectorNode.getCollected().get(0).get("temperature"));

        node.shutdown();
    }

    @Test
    @Tag("integration")
    @DisplayName("토픽 정보 포함")
    void TopicTest() throws Exception {
        String topic = "sensor/temp/" + UUID.randomUUID();
        TestMqttSubscriberNode node = new TestMqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-subscriber-topic-" + UUID.randomUUID(),
                        "topic", topic,
                        "qos", 1
                )
        );
        Connection connection = new Connection();

        node.getOutputPort("out").connect(connection);
        node.initialize();

        publish(topic, "{\"temperature\": 29.0}");

        Message received = connection.poll();

        assertEquals(topic, received.get("topic"));
        assertTrue(received.hasKey("mqttTimestamp"));

        node.shutdown();
    }

    @Test
    @Tag("integration")
    @DisplayName("shutdown 후 연결 해제")
    void ShutdownTest() {
        String topic = "sensor/temp/" + UUID.randomUUID();
        TestMqttSubscriberNode node = new TestMqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-subscriber-shutdown-" + UUID.randomUUID(),
                        "topic", topic,
                        "qos", 1
                )
        );

        node.initialize();
        node.shutdown();

        assertFalse(node.isConnected());
    }

    private void publish(String topic, String payload) throws Exception {
        MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:1883", "mqtt-test-pub-" + UUID.randomUUID(), new MemoryPersistence());
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);
        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(false);
        bufferOptions.setPersistBuffer(false);
        client.setBufferOpts(bufferOptions);

        client.connect(options).waitForCompletion();

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
        mqttMessage.setQos(1);
        client.publish(topic, mqttMessage).waitForCompletion();

        client.disconnect().waitForCompletion();
        client.close();
    }
}
