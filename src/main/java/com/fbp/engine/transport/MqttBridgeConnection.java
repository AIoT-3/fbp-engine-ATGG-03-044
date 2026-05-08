package com.fbp.engine.transport;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MqttBridgeConnection implements Connection {
    private final String id;
    private final String topic;
    private final int qos;
    private final MessageSerializer serializer;
    private final LinkedBlockingQueue<Message> internalQueue;
    private final MqttAsyncClient publisher;
    private final MqttAsyncClient subscriber;

    public MqttBridgeConnection(String id, String broker, String topic, int qos, MessageSerializer serializer) {
        this(id, broker, topic, qos, serializer, 100);
    }

    /**
     * 이 생성자는 MQTT publisher/subscriber를 즉시 연결한다.
     * 브로커가 없으면 Flow 배포 단계에서 빠르게 실패하도록 둔 학습용 구현이다.
     */
    public MqttBridgeConnection(String id, String broker, String topic, int qos,
                                MessageSerializer serializer, int capacity) {
        this.id = id;
        this.topic = topic;
        this.qos = qos;
        this.serializer = serializer;
        this.internalQueue = new LinkedBlockingQueue<>(capacity);
        try {
            String suffix = UUID.randomUUID().toString();
            this.publisher = new MqttAsyncClient(broker, clientId(id, "pub", suffix), new MemoryPersistence());
            this.subscriber = new MqttAsyncClient(broker, clientId(id, "sub", suffix), new MemoryPersistence());
            connectClients();
        } catch (MqttException e) {
            throw new TransportException("MQTT bridge connection 생성 실패: " + id, e);
        }
    }

    private void connectClients() throws MqttException {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);

        subscriber.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
            }

            @Override
            public void messageArrived(String receivedTopic, MqttMessage mqttMessage) {
                try {
                    Message message = serializer.deserialize(mqttMessage.getPayload());
                    boolean added = internalQueue.offer(message);
                    if (!added) {
                        log.warn("MQTT bridge internal queue full; message dropped. connectionId={}, topic={}, queueSize={}",
                                id, receivedTopic, internalQueue.size());
                    }
                } catch (RuntimeException e) {
                    log.warn("MQTT bridge received message 처리 실패. connectionId={}, topic={}", id, receivedTopic, e);
                }
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    try {
                        subscriber.subscribe(topic, qos).waitForCompletion();
                    } catch (MqttException e) {
                        throw new TransportException("MQTT bridge 재구독 실패: " + topic, e);
                    }
                }
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
            }
        });

        publisher.connect(options).waitForCompletion();
        subscriber.connect(options).waitForCompletion();
        subscriber.subscribe(topic, qos).waitForCompletion();
    }

    private String clientId(String id, String role, String suffix) {
        String rawClientId = "fbp-" + role + "-" + id + "-" + suffix;
        String safeClientId = rawClientId.replaceAll("[^A-Za-z0-9_-]", "_");
        int maxClientIdLength = 120;
        if (safeClientId.length() <= maxClientIdLength) {
            return safeClientId;
        }
        return safeClientId.substring(0, maxClientIdLength);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public void deliver(Message message) {
        byte[] payload = serializer.serialize(message);
        MqttMessage mqttMessage = new MqttMessage(payload);
        mqttMessage.setQos(qos);
        mqttMessage.setRetained(false);
        try {
            publisher.publish(topic, mqttMessage).waitForCompletion();
        } catch (MqttException e) {
            String payloadText = new String(payload, StandardCharsets.UTF_8);
            throw new TransportException("MQTT bridge publish 실패: " + topic + ", payload=" + payloadText, e);
        }
    }

    @Override
    public Message poll() throws InterruptedException {
        return internalQueue.take();
    }

    @Override
    public int getBufferSize() {
        return internalQueue.size();
    }

    @Override
    public void close() {
        closeClient(subscriber);
        closeClient(publisher);
    }

    private void closeClient(MqttAsyncClient client) {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect().waitForCompletion();
            }
            client.close();
        } catch (MqttException e) {
            throw new TransportException("MQTT bridge client close 실패: " + id, e);
        }
    }
}
