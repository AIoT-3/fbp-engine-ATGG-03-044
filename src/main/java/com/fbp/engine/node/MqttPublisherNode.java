package com.fbp.engine.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.core.ProtocolNode;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MqttPublisherNode extends ProtocolNode {
    private MqttAsyncClient client;
    private final ObjectMapper objectMapper;
    private final AtomicInteger errorCount;

    public MqttPublisherNode(String id, Map<String, Object> config){
        super(id, config);
        this.objectMapper = new ObjectMapper();
        this.errorCount = new AtomicInteger();
        addInputPort("in");
    }
    @Override
    protected void connect() throws Exception {
        String brokerUrl = (String) getConfig("brokerUrl");
        String clientId = (String) getConfig("clientId");

        client = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);

        client.connect(options).waitForCompletion();
        log.info("MQTT publisher connected: {}", clientId);
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
    @Override
    protected void onProcess(Message message) {
        try {
            String payload = objectMapper.writeValueAsString(message.getPayload());

            String topic;
            if (message.hasKey("topic")) {
                topic = message.get("topic");
            } else {
                topic = (String) getConfig("topic");
            }

            int qos = getConfig("qos") != null ? (int) getConfig("qos") : 1;
            boolean retained = getConfig("retained") != null && (boolean) getConfig("retained");

            MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
            mqttMessage.setQos(qos);
            mqttMessage.setRetained(retained);
            client.publish(topic, mqttMessage).waitForCompletion();
        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.warn("Publish failed: {}", e.getMessage(), e);
        }
    }

    public int getErrorCount() {
        return errorCount.get();
    }
}
