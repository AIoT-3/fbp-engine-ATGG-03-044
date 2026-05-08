package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.DisconnectedBufferOptions;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MqttPublisherNodeTest {
    private MqttPublisherNode mqttPublisherNode;

    @BeforeEach
    void setUp() {
        mqttPublisherNode = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-publisher-test",
                        "topic", "sensor/temp",
                        "qos", 1,
                        "retained", false
                )
        );
    }

    @Test
    @DisplayName("포트 구성")
    void PortTest() {
        assertNotNull(mqttPublisherNode.getInputPort("in"));
    }

    @Test
    @DisplayName("초기 상태")
    void StateTest() {
        assertFalse(mqttPublisherNode.isConnected());
    }

    @Test
    @DisplayName("config 기본 토픽 조회")
    void ConfigTest() {
        assertEquals("sensor/temp", mqttPublisherNode.getConfig("topic"));
    }

    @Test
    @Tag("integration")
    @DisplayName("Broker 연결 성공")
    void ConnectTest() {
        MqttPublisherNode node = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-publisher-connect-" + UUID.randomUUID(),
                        "topic", "sensor/temp/" + UUID.randomUUID(),
                        "qos", 1,
                        "retained", false
                )
        );

        node.initialize();

        assertTrue(node.isConnected());

        node.shutdown();
    }

    @Test
    @Tag("integration")
    @DisplayName("메시지 발행")
    void PublishTest() throws Exception {
        String topic = "sensor/temp/" + UUID.randomUUID();
        MqttPublisherNode node = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-publisher-publish-" + UUID.randomUUID(),
                        "topic", topic,
                        "qos", 1,
                        "retained", false
                )
        );
        LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();
        MqttAsyncClient subscriber = createSubscriber(topic, messages);

        node.initialize();
        node.process(new Message(Map.of("temperature", 32.0, "unit", "C")));

        String received = messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(received);
        assertTrue(received.contains("\"temperature\":32.0"));

        subscriber.disconnect().waitForCompletion();
        subscriber.close();
        node.shutdown();
    }

    @Test
    @Tag("integration")
    @DisplayName("동적 토픽")
    void TopicTest() throws Exception {
        String defaultTopic = "sensor/temp/" + UUID.randomUUID();
        String dynamicTopic = "alert/temp/" + UUID.randomUUID();
        MqttPublisherNode node = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-publisher-topic-" + UUID.randomUUID(),
                        "topic", defaultTopic,
                        "qos", 1,
                        "retained", false
                )
        );
        LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();
        MqttAsyncClient subscriber = createSubscriber(dynamicTopic, messages);

        node.initialize();
        node.process(new Message(Map.of(
                "topic", dynamicTopic,
                "temperature", 35.0,
                "unit", "C"
        )));

        String received = messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(received);
        assertTrue(received.contains("\"temperature\":35.0"));

        subscriber.disconnect().waitForCompletion();
        subscriber.close();
        node.shutdown();
    }

    @Test
    @Tag("integration")
    @DisplayName("shutdown 후 연결 해제")
    void ShutdownTest() {
        String topic = "sensor/temp/" + UUID.randomUUID();
        MqttPublisherNode node = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-publisher-shutdown-" + UUID.randomUUID(),
                        "topic", topic,
                        "qos", 1,
                        "retained", false
                )
        );

        node.initialize();
        node.shutdown();

        assertFalse(node.isConnected());
    }

    private MqttAsyncClient createSubscriber(String topic, LinkedBlockingQueue<String> messages) throws Exception {
        MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:1883", "mqtt-test-sub-" + UUID.randomUUID(), new MemoryPersistence());
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);
        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(false);
        bufferOptions.setPersistBuffer(false);
        client.setBufferOpts(bufferOptions);

        client.connect(options).waitForCompletion();
        client.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
            }

            @Override
            public void messageArrived(String receivedTopic, MqttMessage message) {
                messages.offer(new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
            }
        });
        client.subscribe(topic, 1).waitForCompletion();
        return client;
    }
}
