// 과제 5-6 보조: TimerNode -> FilterNode -> PrintNode 직접 배선 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;

public class MainRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        TimerNode timerNode = new TimerNode("timer-1", 500);
        FilterNode filterNode = new FilterNode("filter-1", "tick", 3.0);
        PrintNode printNode = new PrintNode("printer-1");

        Connection connection1 = new LocalConnection();
        Connection connection2 = new LocalConnection();

        timerNode.getOutputPort("out").connect(connection1);
        filterNode.getOutputPort().connect(connection2);

        Thread filterThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = connection1.poll();
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
                    Message message = connection2.poll();
                    printNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        timerNode.initialize();
        filterNode.initialize();
        printNode.initialize();

        filterThread.start();
        printThread.start();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        running = false;
        timerNode.shutdown();
        filterNode.shutdown();
        printNode.shutdown();
        filterThread.interrupt();
        printThread.interrupt();

        try {
            filterThread.join();
            printThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
