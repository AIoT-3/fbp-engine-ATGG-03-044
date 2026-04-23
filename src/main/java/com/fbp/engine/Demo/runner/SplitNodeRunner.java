// 과제 6-4: SplitNode 분기 플로우 실행 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.SplitNode;
import com.fbp.engine.node.TimerNode;
// 과제 6-4: SplitNode를 사용한 분기 플로우 실행
public class SplitNodeRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        TimerNode timerNode = new TimerNode("timer-1", 500);
        SplitNode splitNode = new SplitNode("split-1", "tick", 3.0);
        PrintNode warningNode = new PrintNode("warning");
        PrintNode normalNode = new PrintNode("normal");

        Flow flow = new Flow("split-flow")
                .addNode(timerNode)
                .addNode(splitNode)
                .addNode(warningNode)
                .addNode(normalNode)
                .connect("timer-1","out","split-1","in")
                .connect("split-1","match","warning","in")
                .connect("split-1","mismatch","normal","in");
        Connection connection1 = flow.getConnection("timer-1:out->split-1:in");
        Connection connection2 = flow.getConnection("split-1:match->warning:in");
        Connection connection3 = flow.getConnection("split-1:mismatch->normal:in");

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
