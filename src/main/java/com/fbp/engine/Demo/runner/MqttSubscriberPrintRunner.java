// 과제 2-5: MqttSubscriberNode -> PrintNode 수신 확인 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.PrintNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class MqttSubscriberPrintRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-subscriber-print-runner",
                        "topic", "sensor/temp",
                        "qos", 1
                )
        );
        PrintNode printNode = new PrintNode("printer-1");
        Connection subscriberToPrint = new Connection();

        subscriberNode.getOutputPort("out").connect(subscriberToPrint);

        Thread printThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = subscriberToPrint.poll();
                    printNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        printThread.start();

        try {
            subscriberNode.initialize();
            log.info("30초 동안 sensor/temp 토픽을 구독합니다.");
            log.info("예시: mosquitto_pub -h localhost -p 1883 -t \"sensor/temp\" -m '{{\"temperature\": 28.5}}'");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            subscriberNode.shutdown();
            running = false;
            printThread.interrupt();

            try {
                printThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
