// 과제 5-1 시나리오 1: MQTT -> Rule -> MQTT runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.node.LogNode;
import com.fbp.engine.node.MqttPublisherNode;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.RuleNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MqttRuleToMqttRunner {
    public static void main(String[] args) {
        AtomicBoolean running = new AtomicBoolean(true);
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-rule-mqtt-subscriber",
                        "topic", "sensor/temp",
                        "qos", 1
                )
        );
        RuleNode ruleNode = new RuleNode("rule-1", "value > 30");
        MqttPublisherNode publisherNode = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-rule-mqtt-publisher",
                        "topic", "alert/temp",
                        "qos", 1,
                        "retained", false
                )
        );
        LogNode logNode = new LogNode("log-1");

        Connection subscriberToRule = new Connection();
        Connection ruleToPublisher = new Connection();
        Connection ruleToLog = new Connection();

        subscriberNode.getOutputPort("out").connect(subscriberToRule);
        ruleNode.getOutputPort("match").connect(ruleToPublisher);
        ruleNode.getOutputPort("mismatch").connect(ruleToLog);

        Thread ruleThread = RunnerSupport.startWorker("mqtt-rule-mqtt-rule-thread", subscriberToRule, ruleNode, running);
        Thread publisherThread = RunnerSupport.startWorker("mqtt-rule-mqtt-publisher-thread", ruleToPublisher, publisherNode, running);
        Thread logThread = RunnerSupport.startWorker("mqtt-rule-mqtt-log-thread", ruleToLog, logNode, running);

        try {
            subscriberNode.initialize();
            publisherNode.initialize();
            log.info("시나리오 1: MQTT -> Rule -> MQTT");
            log.info("예시: mosquitto_pub -h localhost -p 1883 -t \"sensor/temp\" -m '{{\"value\": 35.0}}'");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            subscriberNode.shutdown();
            publisherNode.shutdown();
            RunnerSupport.stopWorkers(running, ruleThread, publisherThread, logThread);
        }
    }
}
