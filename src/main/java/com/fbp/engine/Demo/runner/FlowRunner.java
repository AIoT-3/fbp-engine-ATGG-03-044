// 과제 7-2: Flow를 사용한 4노드 파이프라인 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.LogNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;
// 과제 7-2: Flow를 사용한 4노드 파이프라인 재구성
public class FlowRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        TimerNode timerNode = new TimerNode("timer-1", 1000);
        LogNode logNode = new LogNode("log-1");
        FilterNode filterNode = new FilterNode("filter-1", "tick", 3.0);
        PrintNode printNode = new PrintNode("printer-1");

        Flow flow = new Flow("flow-1")
                .addNode(timerNode)
                .addNode(logNode)
                .addNode(filterNode)
                .addNode(printNode)
                .connect("timer-1", "out", "log-1", "in")
                .connect("log-1", "out", "filter-1", "in")
                .connect("filter-1", "out", "printer-1", "in");

        var timerToLog = flow.getConnection("timer-1:out->log-1:in");
        var logToFilter = flow.getConnection("log-1:out->filter-1:in");
        var filterToPrint = flow.getConnection("filter-1:out->printer-1:in");

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

        flow.initialize();

        logThread.start();
        filterThread.start();
        printThread.start();

        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        running = false;
        flow.shutdown();

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
