package com.fbp.engine.integration;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.MqttPublisherNode;
import com.fbp.engine.node.MqttSubscriberNode;
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
class MqttIntegrationTest {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final List<Thread> threads = new ArrayList<>();
    private final List<AutoCloseable> closeables = new ArrayList<>();

    @AfterEach
    void tearDown() throws Exception {
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }
        TestNodeWorkerSupport.stopWorkers(running, threads.toArray(new Thread[0]));
    }

    @Test
    @DisplayName("Subscriber -> Publisher 파이프라인")
    void PipelineTest() throws Exception {
        MqttTestSupport.assumeBrokerAvailable();

        String inputTopic = "sensor/temp/" + UUID.randomUUID();
        String outputTopic = "alert/temp/" + UUID.randomUUID();
        LinkedBlockingQueue<MqttTestSupport.ReceivedMessage> messages = new LinkedBlockingQueue<>();
        MqttAsyncClient outputSubscriber = MqttTestSupport.createSubscriber(outputTopic, 1, messages);
        closeables.add(() -> MqttTestSupport.close(outputSubscriber));

        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-pipeline-sub-" + UUID.randomUUID(),
                        "topic", inputTopic,
                        "qos", 1
                )
        );
        MqttPublisherNode publisherNode = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-pipeline-pub-" + UUID.randomUUID(),
                        "topic", outputTopic,
                        "qos", 1,
                        "retained", false
                )
        );
        Connection subscriberToPublisher = new LocalConnection();

        subscriberNode.getOutputPort("out").connect(subscriberToPublisher);
        // MqttSubscriberNode가 메시지에 'topic' 필드를 넣으므로, publisher가 config topic 대신
        // 그 값을 사용하지 않도록 워커에서 topic 키를 제거한다.
        threads.add(new Thread(() -> {
            while (running.get()) {
                try {
                    Message msg = subscriberToPublisher.poll();
                    publisherNode.process(msg.withoutKey("topic").withoutKey("mqttTimestamp"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "mqtt-pipeline-thread"));
        threads.get(threads.size() - 1).start();

        subscriberNode.initialize();
        publisherNode.initialize();
        closeables.add(subscriberNode::shutdown);
        closeables.add(publisherNode::shutdown);

        Thread.sleep(500); // 구독 준비 및 publisher 연결 대기
        MqttTestSupport.publish(inputTopic, "{\"value\":35.0}");

        MqttTestSupport.ReceivedMessage received = messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(received);
        assertTrue(received.getPayload().contains("\"value\":35.0"));
    }

    @Test
    @DisplayName("다중 토픽 구독")
    void WildcardTopicTest() throws Exception {
        MqttTestSupport.assumeBrokerAvailable();

        String prefix = "sensor/" + UUID.randomUUID();
        String wildcardTopic = prefix + "/+";
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-wildcard-sub-" + UUID.randomUUID(),
                        "topic", wildcardTopic,
                        "qos", 1
                )
        );
        Connection outputConnection = new LocalConnection();
        subscriberNode.getOutputPort("out").connect(outputConnection);

        subscriberNode.initialize();
        closeables.add(subscriberNode::shutdown);

        Thread.sleep(500); // 구독 준비 대기
        MqttTestSupport.publish(prefix + "/temp", "{\"value\":25.0}");
        MqttTestSupport.publish(prefix + "/humidity", "{\"value\":60.0}");

        Message first = outputConnection.poll();
        Message second = outputConnection.poll();

        String firstTopic = first.get("topic");
        String secondTopic = second.get("topic");
        assertNotNull(firstTopic);
        assertNotNull(secondTopic);
        assertNotEquals(firstTopic, secondTopic);
    }

    @Test
    @DisplayName("QoS 1 전달 보장")
    void QosOneTest() throws Exception {
        MqttTestSupport.assumeBrokerAvailable();

        String topic = "sensor/temp/" + UUID.randomUUID();
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-qos-sub-" + UUID.randomUUID(),
                        "topic", topic,
                        "qos", 1
                )
        );
        Connection outputConnection = new LocalConnection();
        subscriberNode.getOutputPort("out").connect(outputConnection);

        subscriberNode.initialize();
        closeables.add(subscriberNode::shutdown);

        for (int i = 0; i < 5; i++) {
            MqttTestSupport.publish(topic, "{\"seq\":" + i + "}", 1);
        }

        int receivedCount = 0;
        for (int i = 0; i < 5; i++) {
            Message received = outputConnection.poll();
            assertNotNull(received.get("seq"));
            receivedCount++;
        }

        assertEquals(5, receivedCount);
    }

    @Test
    @Disabled("Requires manual Mosquitto broker restart during test")
    @DisplayName("재연결 테스트")
    void ReconnectTest() throws Exception {
        MqttTestSupport.assumeBrokerAvailable();

        String topic = "sensor/temp/" + UUID.randomUUID();
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-reconnect-sub-" + UUID.randomUUID(),
                        "topic", topic,
                        "qos", 1
                )
        );
        Connection outputConnection = new LocalConnection();
        subscriberNode.getOutputPort("out").connect(outputConnection);

        subscriberNode.initialize();
        closeables.add(subscriberNode::shutdown);

        MqttTestSupport.publish(topic, "{\"value\":35.0}");
        assertNotNull(outputConnection.poll());
    }
}
