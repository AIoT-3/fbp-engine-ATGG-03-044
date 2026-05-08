// 과제 2-7: MqttSubscriberNode -> ThresholdFilterNode -> MqttPublisherNode 양방향 MQTT runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.MqttPublisherNode;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.ThresholdFilterNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class MqttBidirectionalRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-bidirectional-subscriber",
                        "topic", "sensor/temp",
                        "qos", 1
                )
        );
        ThresholdFilterNode filterNode = new ThresholdFilterNode("filter-1", "temperature", 30.0);
        MqttPublisherNode publisherNode = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-bidirectional-publisher",
                        "topic", "alert/temp",
                        "qos", 1,
                        "retained", false
                )
        );

        Connection subscriberToFilter = new LocalConnection();
        Connection filterToPublisher = new LocalConnection();

        subscriberNode.getOutputPort("out").connect(subscriberToFilter);
        filterNode.getOutputPort("alert").connect(filterToPublisher);

        Thread filterThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = subscriberToFilter.poll();
                    filterNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread publisherThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = filterToPublisher.poll();
                    publisherNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        filterThread.start();
        publisherThread.start();

        try {
            subscriberNode.initialize();
            publisherNode.initialize();
            log.info("30초 동안 sensor/temp -> alert/temp 양방향 MQTT 플로우를 실행합니다.");
            log.info("예시 발행: mosquitto_pub -h localhost -p 1883 -t \"sensor/temp\" -m '{{\"temperature\": 31.5, \"unit\": \"C\"}}'");
            log.info("별도 터미널에서 mosquitto_sub -h localhost -p 1883 -t \"alert/temp\" -v 로 확인하면 됩니다.");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            subscriberNode.shutdown();
            publisherNode.shutdown();
            running = false;
            filterThread.interrupt();
            publisherThread.interrupt();

            try {
                filterThread.join();
                publisherThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
