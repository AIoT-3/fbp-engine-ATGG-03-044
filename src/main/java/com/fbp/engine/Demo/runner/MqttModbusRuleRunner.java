// 과제 4-4: MQTT 수신 -> Rule 분기 -> MQTT 알림 + MODBUS 제어 통합 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.node.LogNode;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.node.MqttPublisherNode;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.RuleNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MqttModbusRuleRunner {
    public static void main(String[] args) {
        AtomicBoolean running = new AtomicBoolean(true);
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-modbus-rule-subscriber",
                        "topic", "sensor/temp",
                        "qos", 1
                )
        );
        RuleNode ruleNode = new RuleNode("rule-1", "temperature > 30");
        MqttPublisherNode publisherNode = new MqttPublisherNode(
                "mqtt-publisher-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-modbus-rule-publisher",
                        "topic", "alert/temp",
                        "qos", 1,
                        "retained", false
                )
        );
        ModbusWriterNode writerNode = new ModbusWriterNode(
                "modbus-writer-1",
                Map.of(
                        "host", "localhost",
                        "port", 5020,
                        "slaveId", 1,
                        "registerAddress", 2,
                        "fixedValue", 1
                )
        );
        LogNode logNode = new LogNode("log-1");
        PrintNode resultPrintNode = new PrintNode("result-printer-1");

        Connection subscriberToRule = new LocalConnection();
        Connection ruleToPublisher = new LocalConnection();
        Connection ruleToWriter = new LocalConnection();
        Connection ruleToLog = new LocalConnection();
        Connection writerToPrint = new LocalConnection();

        subscriberNode.getOutputPort("out").connect(subscriberToRule);
        ruleNode.getOutputPort("match").connect(ruleToPublisher);
        ruleNode.getOutputPort("match").connect(ruleToWriter);
        ruleNode.getOutputPort("mismatch").connect(ruleToLog);
        writerNode.getOutputPort("result").connect(writerToPrint);

        Thread ruleThread = RunnerSupport.startWorker("mqtt-modbus-rule-thread", subscriberToRule, ruleNode, running);
        Thread publisherThread = RunnerSupport.startWorker("mqtt-modbus-publisher-thread", ruleToPublisher, publisherNode, running);
        Thread writerThread = RunnerSupport.startWorker("mqtt-modbus-writer-thread", ruleToWriter, writerNode, running);
        Thread logThread = RunnerSupport.startWorker("mqtt-modbus-log-thread", ruleToLog, logNode, running);
        Thread resultThread = RunnerSupport.startWorker("mqtt-modbus-result-thread", writerToPrint, resultPrintNode, running);

        try {
            simulator.setRegister(2, 0);
            simulator.start();
            subscriberNode.initialize();
            publisherNode.initialize();
            writerNode.initialize();

            log.info("sensor/temp로 MQTT 메시지를 발행하면 RuleNode가 분기합니다.");
            log.info("예시: mosquitto_pub -h localhost -p 1883 -t \"sensor/temp\" -m '{{\"temperature\": 35.0}}'");
            log.info("alert/temp는 별도 터미널의 mosquitto_sub로 확인하면 됩니다.");
            Thread.sleep(30000);
            log.info("register[2] = {}", simulator.getRegister(2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            subscriberNode.shutdown();
            publisherNode.shutdown();
            writerNode.shutdown();
            RunnerSupport.stopWorkers(running, ruleThread, publisherThread, writerThread, logThread, resultThread);
            try {
                simulator.stop();
            } catch (IOException ignored) {
            }
        }
    }
}
