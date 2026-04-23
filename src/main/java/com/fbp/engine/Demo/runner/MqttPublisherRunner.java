// 과제 2-6: GeneratorNode -> MqttPublisherNode 발행 확인 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.MqttPublisherNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class MqttPublisherRunner {
    public static void main(String[] args) {
        GeneratorNode generatorNode = new GeneratorNode("generator-1");
        MqttPublisherNode publisherNode = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-publisher-runner",
                        "topic", "sensor/temp",
                        "qos", 1,
                        "retained", false
                )
        );

        try {
            publisherNode.initialize();
            log.info("sensor/temp 토픽으로 5개의 메시지를 발행합니다.");
            log.info("미리 mosquitto_sub -h localhost -p 1883 -t \"sensor/temp\" -v 를 실행해두면 됩니다.");

            for (int i = 0; i < 5; i++) {
                Message message = generatorNode.createMessage("temperature", 25.0 + i)
                        .withEntry("unit", "C");
                publisherNode.process(message);
                log.info("Published: {}", message.getPayload());
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            publisherNode.shutdown();
        }
    }
}
