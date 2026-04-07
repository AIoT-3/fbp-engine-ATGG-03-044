package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.LogNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;

public class FourNodePipeRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        TimerNode timerNode = new TimerNode("timer-1", 1000);
        LogNode logNode = new LogNode("log-1");
        FilterNode filterNode = new FilterNode("filter-1", "tick", 3.0);
        PrintNode printNode = new PrintNode("printer-1");

        Connection connection1 = new Connection();
        Connection connection2 = new Connection();
        Connection connection3 = new Connection();

        timerNode.getOutputPort("out").connect(connection1);
        logNode.getOutputPort("out").connect(connection2);
        filterNode.getOutputPort("out").connect(connection3);

        Thread logThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = connection1.poll();
                    logNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread filterThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = connection2.poll();
                    filterNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread printThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = connection3.poll();
                    printNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        timerNode.initialize();
        logNode.initialize();
        filterNode.initialize();
        printNode.initialize();

        logThread.start();
        filterThread.start();
        printThread.start();

        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        running = false;
        timerNode.shutdown();
        logNode.shutdown();
        filterNode.shutdown();
        printNode.shutdown();

        logThread.interrupt();
        filterThread.interrupt();
        printThread.interrupt();

        try {
            logThread.join();
            filterThread.join();
            printThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
