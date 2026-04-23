// 과제 8-3: FlowEngine에 여러 플로우를 등록하고 동시 실행하는 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;

// 과제 8-3: FlowEngine에 두 개의 플로우를 동시에 등록하고 실행
public class MultiFlowRunner {
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

        FlowEngine engine = new FlowEngine();
        engine.register(flowA);
        engine.register(flowB);

        try {
            engine.startFlow("flowA");
            engine.startFlow("flowB");

            Thread.sleep(5000);
            engine.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
