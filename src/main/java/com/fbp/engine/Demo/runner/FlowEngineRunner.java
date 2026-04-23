// 과제 8-2: FlowEngine으로 Step 7 플로우 실행 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.LogNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;

// 과제 8-2: FlowEngine을 사용하여 Step 7의 플로우 실행
public class FlowEngineRunner {
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

        FlowEngine engine = new FlowEngine();
        engine.register(flow);

        try {
            engine.startFlow("monitoring");
            Thread.sleep(5000);
            engine.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
