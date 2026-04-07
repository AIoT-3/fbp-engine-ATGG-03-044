package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.PrintNode;

public class ThreeNodeRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        GeneratorNode generatorNode = new GeneratorNode("generator-1");
        FilterNode filterNode = new FilterNode("filter-1", "temperature", 22.0);
        PrintNode printNode = new PrintNode("printer-1");

        Connection connection1 = new Connection();
        Connection connection2 = new Connection();

        Thread generatorThread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    Message message = generatorNode.createMessage("temperature", 20.0 + i);
                    connection1.deliver(message);
                    System.out.println("[생산자] 메시지 전송: " + message.getPayload());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread filterThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = connection1.poll();
                    if (filterNode.matches(message)) {
                        connection2.deliver(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread printThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = connection2.poll();
                    printNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        generatorThread.start();
        filterThread.start();
        printThread.start();

        try {
            generatorThread.join();
            Thread.sleep(1000);
            running = false;
            filterThread.interrupt();
            printThread.interrupt();
            filterThread.join();
            printThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
