package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;

// 과제 8-3: FlowEngine에 두 개의 플로우를 동시에 등록하고 실행
public class MultiFlowRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        TimerNode timerA = new TimerNode("timer-A", 500);
        PrintNode printA = new PrintNode("A");

        Flow flowA = new Flow("flowA")
                .addNode(timerA)
                .addNode(printA)
                .connect("timer-A", "out", "A", "in");

        TimerNode timerB = new TimerNode("timer-B", 1000);
        PrintNode printB = new PrintNode("B");

        Flow flowB = new Flow("flowB")
                .addNode(timerB)
                .addNode(printB)
                .connect("timer-B", "out", "B", "in");

        Connection connectionA = flowA.getConnections().get(0);
        Connection connectionB = flowB.getConnections().get(0);

        Thread printThreadA = new Thread(() -> {
            while (running) {
                try {
                    Message message = connectionA.poll();
                    printA.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread printThreadB = new Thread(() -> {
            while (running) {
                try {
                    Message message = connectionB.poll();
                    printB.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        FlowEngine engine = new FlowEngine();
        engine.register(flowA);
        engine.register(flowB);

        printThreadA.start();
        printThreadB.start();

        try {
            engine.startFlow("flowA");
            engine.startFlow("flowB");

            Thread.sleep(5000);
            engine.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running = false;
            printThreadA.interrupt();
            printThreadB.interrupt();

            try {
                printThreadA.join();
                printThreadB.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

