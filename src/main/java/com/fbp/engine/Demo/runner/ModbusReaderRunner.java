// 과제 3-8: TimerNode -> ModbusReaderNode -> PrintNode runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ModbusReaderNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;

import java.io.IOException;
import java.util.Map;

public class ModbusReaderRunner {
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
                        "count", 3
                )
        );
        PrintNode printNode = new PrintNode("printer-1");
        PrintNode errorPrintNode = new PrintNode("error-printer-1");

        Connection timerToReader = new Connection();
        Connection readerToPrint = new Connection();
        Connection readerToError = new Connection();

        timerNode.getOutputPort("out").connect(timerToReader);
        readerNode.getOutputPort("out").connect(readerToPrint);
        readerNode.getOutputPort("error").connect(readerToError);

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

        Thread printThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = readerToPrint.poll();
                    printNode.process(message);
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
            simulator.setRegister(0, 250);
            simulator.setRegister(1, 600);
            simulator.setRegister(2, 1);
            simulator.start();

            readerNode.initialize();
            timerNode.initialize();

            readerThread.start();
            printThread.start();
            errorThread.start();

            Thread.sleep(10000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            timerNode.shutdown();
            readerNode.shutdown();
            running = false;

            readerThread.interrupt();
            printThread.interrupt();
            errorThread.interrupt();

            try {
                readerThread.join();
                printThread.join();
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
