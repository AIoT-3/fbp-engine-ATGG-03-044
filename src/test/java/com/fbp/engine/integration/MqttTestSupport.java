package com.fbp.engine.integration;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.mqttv5.client.DisconnectedBufferOptions;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Assumptions;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public final class MqttTestSupport {
    @Data
    @RequiredArgsConstructor
    public static class ReceivedMessage {
        private final String topic;
        private final String payload;
    }

    private MqttTestSupport() {
    }

    public static void assumeBrokerAvailable() {
        try {
            MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:1883", "mqtt-broker-check-" + UUID.randomUUID(), new MemoryPersistence());
            MqttConnectionOptions options = baseOptions();
            client.connect(options).waitForCompletion();
            client.disconnect().waitForCompletion();
            client.close();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Mosquitto broker is not available");
        }
    }

    public static void publish(String topic, String payload) throws Exception {
        publish(topic, payload, 1);
    }

    public static void publish(String topic, String payload, int qos) throws Exception {
        MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:1883", "mqtt-test-pub-" + UUID.randomUUID(), new MemoryPersistence());
        client.setBufferOpts(bufferOptions());
        client.connect(baseOptions()).waitForCompletion();

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
        mqttMessage.setQos(qos);
        client.publish(topic, mqttMessage).waitForCompletion();

        client.disconnect().waitForCompletion();
        client.close();
    }

    public static MqttAsyncClient createSubscriber(String topic, int qos, LinkedBlockingQueue<ReceivedMessage> messages) throws Exception {
        MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:1883", "mqtt-test-sub-" + UUID.randomUUID(), new MemoryPersistence());
        client.setBufferOpts(bufferOptions());
        client.connect(baseOptions()).waitForCompletion();
        client.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
            }

            @Override
            public void messageArrived(String receivedTopic, MqttMessage message) {
                messages.offer(new ReceivedMessage(receivedTopic, new String(message.getPayload())));
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
        client.subscribe(topic, qos).waitForCompletion();
        return client;
    }

    public static void close(MqttAsyncClient client) throws Exception {
        if (client == null) {
            return;
        }
        if (client.isConnected()) {
            client.disconnect().waitForCompletion();
        }
        client.close();
    }

    private static MqttConnectionOptions baseOptions() {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);
        return options;
    }

    private static DisconnectedBufferOptions bufferOptions() {
        DisconnectedBufferOptions options = new DisconnectedBufferOptions();
        options.setBufferEnabled(false);
        options.setPersistBuffer(false);
        return options;
    }
}
