package com.fbp.engine.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.core.ProtocolNode;
import com.fbp.engine.message.Message;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.LinkedHashMap;
import java.util.Map;

public class MqttSubscriberNode extends ProtocolNode {
    private MqttAsyncClient client;
    private final ObjectMapper objectMapper;
    private String subscribedTopic;
    private int subscribedQos;

    public MqttSubscriberNode(String id, Map<String, Object> config) {
        super(id, config);
        this.objectMapper = new ObjectMapper();
        addOutputPort("out");
    }

    @Override
    protected void connect() throws Exception {
        String brokerUrl = (String) getConfig("brokerUrl");
        String clientId = (String) getConfig("clientId");
        subscribedTopic = (String) getConfig("topic");
        subscribedQos = (int) getConfigOrDefault("qos", 1);

        client = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);

        client.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
            }

            @Override
            public void messageArrived(String receivedTopic, MqttMessage mqttMessage) {
                String payload = new String(mqttMessage.getPayload());
                send("out", toMessage(receivedTopic, payload));
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    try {
                        client.subscribe(subscribedTopic, subscribedQos).waitForCompletion();
                    } catch (MqttException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
            }
        });

        client.connect(options).waitForCompletion();
        client.subscribe(subscribedTopic, subscribedQos).waitForCompletion();
    }

    @Override
    protected void disconnect() throws Exception {
        if (client != null) {
            if (client.isConnected()) {
                client.disconnect().waitForCompletion();
            }
            client.close();
        }
    }

    private Object getConfigOrDefault(String key, Object defaultValue) {
        Object value = getConfig(key);
        return value != null ? value : defaultValue;
    }

    protected Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            Map<String, Object> fallbackPayload = new LinkedHashMap<>();
            fallbackPayload.put("rawPayload", payload);
            return fallbackPayload;
        }
    }

    protected Message toMessage(String topic, String payload) {
        Map<String, Object> parsedPayload = parsePayload(payload);
        parsedPayload.put("topic", topic);
        parsedPayload.putAll(parseIotTopic(topic));
        parsedPayload.put("mqttTimestamp", System.currentTimeMillis());
        return new Message(parsedPayload);
    }

    private Map<String, Object> parseIotTopic(String topic) {
        Map<String, Object> topicFields = new LinkedHashMap<>();
        if (topic == null || !topic.startsWith("iot/")) {
            return topicFields;
        }

        String[] parts = topic.split("/");
        if (parts.length != 5 && parts.length != 6) {
            return topicFields;
        }

        topicFields.put("location", parts[1]);
        if (parts.length == 6) {
            topicFields.put("point", parts[2]);
            topicFields.put("sensor_type", parts[3]);
            topicFields.put("dev_eui", parts[4]);
            topicFields.put("measurement_key", parts[5]);
        } else {
            topicFields.put("sensor_type", parts[2]);
            topicFields.put("dev_eui", parts[3]);
            topicFields.put("measurement_key", parts[4]);
        }
        return topicFields;
    }

    @Override
    protected void onProcess(Message message) {
        // MQTT subscriber is a source node. It emits messages from broker callbacks only.
    }
}
