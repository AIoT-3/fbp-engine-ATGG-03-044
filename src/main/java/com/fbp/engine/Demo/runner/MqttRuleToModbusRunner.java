// 과제 5-1 시나리오 3: MQTT -> Rule -> MODBUS 크로스 프로토콜 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.node.LogNode;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.RuleNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MqttRuleToModbusRunner {
    public static void main(String[] args) {
        AtomicBoolean running = new AtomicBoolean(true);
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode(
                "mqtt-subscriber-1",
                Map.of(
                        "brokerUrl", "tcp://localhost:1883",
                        "clientId", "mqtt-rule-modbus-subscriber",
                        "topic", "sensor/temp",
                        "qos", 1
                )
        );
        RuleNode ruleNode = new RuleNode("rule-1", "temperature > 30");
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

        Connection subscriberToRule = new Connection();
        Connection ruleToWriter = new Connection();
        Connection ruleToLog = new Connection();

        subscriberNode.getOutputPort("out").connect(subscriberToRule);
        ruleNode.getOutputPort("match").connect(ruleToWriter);
        ruleNode.getOutputPort("mismatch").connect(ruleToLog);

        Thread ruleThread = RunnerSupport.startWorker("mqtt-rule-modbus-rule-thread", subscriberToRule, ruleNode, running);
        Thread writerThread = RunnerSupport.startWorker("mqtt-rule-modbus-writer-thread", ruleToWriter, writerNode, running);
        Thread logThread = RunnerSupport.startWorker("mqtt-rule-modbus-log-thread", ruleToLog, logNode, running);

        try {
            simulator.setRegister(2, 0);
            simulator.start();
            subscriberNode.initialize();
            writerNode.initialize();
            log.info("시나리오 3: MQTT -> Rule -> MODBUS");
            log.info("예시: mosquitto_pub -h localhost -p 1883 -t \"sensor/temp\" -m '{{\"temperature\": 35.0}}'");
            Thread.sleep(30000);
            log.info("register[2] = {}", simulator.getRegister(2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            subscriberNode.shutdown();
            writerNode.shutdown();
            RunnerSupport.stopWorkers(running, ruleThread, writerThread, logThread);
            try {
                simulator.stop();
            } catch (IOException ignored) {
            }
        }
    }
}
