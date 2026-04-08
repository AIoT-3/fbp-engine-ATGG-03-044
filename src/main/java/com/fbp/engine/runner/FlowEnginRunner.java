package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.LogNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;

import java.util.List;

// 과제 8-2: FlowEngine을 사용하여 Step 7의 플로우 실행
public class FlowEnginRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        TimerNode timerNode = new TimerNode("timer-1", 1000);
        LogNode logNode = new LogNode("log-1");
        FilterNode filterNode = new FilterNode("filter-1", "tick", 3.0);
        PrintNode printNode = new PrintNode("printer-1");

        Flow flow = new Flow("monitoring")
                .addNode(timerNode)
                .addNode(logNode)
                .addNode(filterNode)
                .addNode(printNode)
                .connect("timer-1", "out", "log-1", "in")
                .connect("log-1", "out", "filter-1", "in")
                .connect("filter-1", "out", "printer-1", "in");

        List<Connection> connections = flow.getConnections();
        Connection timerToLog = connections.get(0);
        Connection logToFilter = connections.get(1);
        Connection filterToPrint = connections.get(2);

        Thread logThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = timerToLog.poll();
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
                    Message message = logToFilter.poll();
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
                    Message message = filterToPrint.poll();
                    printNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        FlowEngine engine = new FlowEngine();
        engine.register(flow);

        logThread.start();
        filterThread.start();
        printThread.start();

        try {
            engine.startFlow("monitoring");
            Thread.sleep(5000);
            engine.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running = false;
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
}