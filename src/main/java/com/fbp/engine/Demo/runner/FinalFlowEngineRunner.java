// 과제 8 최종: Timer/Sensor/Filter/Alert/Log/FileWriter 통합 플로우 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.*;

public class FinalFlowEngineRunner {
    public static void main(String[] args) {
        TimerNode timerNode = new TimerNode("timer-1", 1000);
        TemperatureSensorNode sensorNode = new TemperatureSensorNode("sensor-1", 15.0, 45.0);
        ThresholdFilterNode filterNode = new ThresholdFilterNode("filter-1", "temperature", 30.0);
        AlertNode alertNode = new AlertNode("alert-1");
        LogNode logNode = new LogNode("log-1");
        FileWriterNode fileWriterNode = new FileWriterNode("file-1", "temperature-normal.log");

        Flow flow = new Flow("final-monitoring")
                .addNode(timerNode)
                .addNode(sensorNode)
                .addNode(filterNode)
                .addNode(alertNode)
                .addNode(logNode)
                .addNode(fileWriterNode)
                .connect("timer-1", "out", "sensor-1", "trigger")
                .connect("sensor-1", "out", "filter-1", "in")
                .connect("filter-1", "alert", "alert-1", "in")
                .connect("filter-1", "normal", "log-1", "in")
                .connect("log-1", "out", "file-1", "in");
        FlowEngine engine = new FlowEngine();
        engine.register(flow);

        try {
            engine.startFlow("final-monitoring");
            Thread.sleep(10000);
            engine.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
