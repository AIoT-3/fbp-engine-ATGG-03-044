// 과제 5-1 시나리오 2: Timer -> MODBUS Reader -> Rule -> MODBUS Writer runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.node.ModbusReaderNode;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.RuleNode;
import com.fbp.engine.node.TimerNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TimerModbusRuleRunner {
    public static void main(String[] args) {
        AtomicBoolean running = new AtomicBoolean(true);
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        TimerNode timerNode = new TimerNode("timer-1", 1000);
        ModbusReaderNode readerNode = new ModbusReaderNode(
                "modbus-reader-1",
                Map.of(
                        "host", "localhost",
                        "port", 5020,
                        "slaveId", 1,
                        "startAddress", 0,
                        "count", 1,
                        "registerMapping", Map.of(
                                "0", Map.of(
                                        "name", "temperature",
                                        "scale", 0.1
                                )
                        )
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
        PrintNode errorPrintNode = new PrintNode("error-printer-1");

        Connection timerToReader = new LocalConnection();
        Connection readerToRule = new LocalConnection();
        Connection readerToError = new LocalConnection();
        Connection ruleToWriter = new LocalConnection();

        timerNode.getOutputPort("out").connect(timerToReader);
        readerNode.getOutputPort("out").connect(readerToRule);
        readerNode.getOutputPort("error").connect(readerToError);
        ruleNode.getOutputPort("match").connect(ruleToWriter);

        Thread readerThread = RunnerSupport.startWorker("timer-modbus-reader-thread", timerToReader, readerNode, running);
        Thread ruleThread = RunnerSupport.startWorker("timer-modbus-rule-thread", readerToRule, ruleNode, running);
        Thread writerThread = RunnerSupport.startWorker("timer-modbus-writer-thread", ruleToWriter, writerNode, running);
        Thread errorThread = RunnerSupport.startWorker("timer-modbus-error-thread", readerToError, errorPrintNode, running);

        try {
            simulator.setRegister(0, 320);
            simulator.setRegister(2, 0);
            simulator.start();
            readerNode.initialize();
            writerNode.initialize();
            timerNode.initialize();
            log.info("시나리오 2: Timer -> MODBUS Reader -> Rule -> MODBUS Writer");
            Thread.sleep(10000);
            log.info("register[2] = {}", simulator.getRegister(2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            timerNode.shutdown();
            readerNode.shutdown();
            writerNode.shutdown();
            RunnerSupport.stopWorkers(running, readerThread, ruleThread, writerThread, errorThread);
            try {
                simulator.stop();
            } catch (IOException ignored) {
            }
        }
    }
}
