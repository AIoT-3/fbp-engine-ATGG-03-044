package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.SplitNode;
import com.fbp.engine.node.TimerNode;

public class SplitNodeRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        TimerNode timerNode = new TimerNode("timer-1", 500);
        SplitNode splitNode = new SplitNode("split-1", "tick", 3.0);
        PrintNode warningNode = new PrintNode("warning");
        PrintNode normalNode = new PrintNode("normal");

        Connection connection1 = new Connection();
        Connection connection2 = new Connection();
        Connection connection3 = new Connection();

        timerNode.getOutputPort("out").connect(connection1);
        splitNode.getOutputPort("match").connect(connection2);
        splitNode.getOutputPort("mismatch").connect(connection3);

        Thread splitThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = connection1.poll();
                    splitNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread warningThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = connection2.poll();
                    warningNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread normalThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = connection3.poll();
                    normalNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        timerNode.initialize();
        splitNode.initialize();
        warningNode.initialize();
        normalNode.initialize();

        splitThread.start();
        warningThread.start();
        normalThread.start();

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        running = false;
        timerNode.shutdown();
        splitNode.shutdown();
        warningNode.shutdown();
        normalNode.shutdown();

        splitThread.interrupt();
        warningThread.interrupt();
        normalThread.interrupt();

        try {
            splitThread.join();
            warningThread.join();
            normalThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
