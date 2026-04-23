// 과제 3-9: ModbusReaderNode -> ThresholdFilterNode -> ModbusWriterNode runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ModbusReaderNode;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.ThresholdFilterNode;
import com.fbp.engine.node.TimerNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class ModbusAlertRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
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
        ThresholdFilterNode filterNode = new ThresholdFilterNode("threshold-filter-1", "temperature", 30.0);
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
        PrintNode resultPrintNode = new PrintNode("result-printer-1");
        PrintNode errorPrintNode = new PrintNode("error-printer-1");

        Connection timerToReader = new Connection();
        Connection readerToFilter = new Connection();
        Connection filterToWriter = new Connection();
        Connection readerToError = new Connection();
        Connection writerToResult = new Connection();

        timerNode.getOutputPort("out").connect(timerToReader);
        readerNode.getOutputPort("out").connect(readerToFilter);
        readerNode.getOutputPort("error").connect(readerToError);
        filterNode.getOutputPort("alert").connect(filterToWriter);
        writerNode.getOutputPort("result").connect(writerToResult);

        Thread readerThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = timerToReader.poll();
                    readerNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread filterThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = readerToFilter.poll();
                    filterNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread writerThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = filterToWriter.poll();
                    writerNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread resultThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = writerToResult.poll();
                    resultPrintNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread errorThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = readerToError.poll();
                    errorPrintNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        try {
            simulator.setRegister(0, 320);
            simulator.setRegister(1, 600);
            simulator.setRegister(2, 0);
            simulator.start();

            readerNode.initialize();
            writerNode.initialize();
            timerNode.initialize();

            readerThread.start();
            filterThread.start();
            writerThread.start();
            resultThread.start();
            errorThread.start();

            Thread.sleep(5000);
            log.info("register[2] = {}", simulator.getRegister(2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            timerNode.shutdown();
            readerNode.shutdown();
            writerNode.shutdown();
            running = false;

            readerThread.interrupt();
            filterThread.interrupt();
            writerThread.interrupt();
            resultThread.interrupt();
            errorThread.interrupt();

            try {
                readerThread.join();
                filterThread.join();
                writerThread.join();
                resultThread.join();
                errorThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            try {
                simulator.stop();
            } catch (IOException ignored) {
            }
        }
    }
}
