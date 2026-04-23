package com.fbp.engine.protocol;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

@Slf4j
public class MqttPathDemo {
    public static void main(String[] args){
        try{
            MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:1883", "demo-client", new MemoryPersistence());
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void disconnected(MqttDisconnectResponse disconnectResponse) {
                    log.info("Disconnected");
                }

                @Override
                public void mqttErrorOccurred(MqttException exception) {
                    log.error("MQTT error occurred", exception);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    log.info("Received: {} -> {}", topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttToken token) {
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    log.info("Connect complete: {}", serverURI);
                }

                @Override
                public void authPacketArrived(int reasonCode, MqttProperties properties) {
                }
            });

            client.connect(options).waitForCompletion();
            log.info("Connected");

            client.subscribe("sensor/temp", 1).waitForCompletion();

            MqttMessage mqttMessage = new MqttMessage("{\"value\": 28.5}".getBytes());
            mqttMessage.setQos(1);
            client.publish("sensor/temp", mqttMessage).waitForCompletion();

            Thread.sleep(2000);

            client.disconnect().waitForCompletion();
            client.close();
            log.info("Disconnected");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("MQTT demo interrupted", e);
        } catch (Exception e) {
            log.error("MQTT demo failed", e);
        }
    }
}
