// 과제 8 최종 보조: 온도/습도 모니터링 FlowEngine runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.*;
public class TemperatureMonitorRunner {
    public static void main(String[] args) {
        TimerNode timerNode = new TimerNode("timer-1", 1000);
        TemperatureSensorNode tempSensorNode = new TemperatureSensorNode("sensor-1", 15.0, 45.0);
        HumiditySensorNode humiditySensorNode = new HumiditySensorNode("humidity-sensor-1", 30.0, 90.0);
        ThresholdFilterNode tempFilterNode = new ThresholdFilterNode("filter-1", "temperature", 30.0);
        ThresholdFilterNode humidityFilterNode = new ThresholdFilterNode("humidity-filter-1", "humidity", 69.9);
        AlertNode tempAlertNode = new AlertNode("temp-alert-1");
        AlertNode humidityAlertNode = new AlertNode("humidity-alert-1");

        FileWriterNode tempFileNode = new FileWriterNode("temp-file-1", "temperature-normal.log");
        LogNode humidityLogNode = new LogNode("humidity-log-1");

        Flow flow = new Flow("monitoring")
                .addNode(timerNode)
                .addNode(tempSensorNode)
                .addNode(humiditySensorNode)
                .addNode(tempFilterNode)
                .addNode(humidityFilterNode)
                .addNode(tempAlertNode)
                .addNode(humidityAlertNode)
                .addNode(tempFileNode)
                .addNode(humidityLogNode)
                .connect("timer-1", "out", "sensor-1", "trigger")
                .connect("sensor-1", "out", "filter-1", "in")
                .connect("filter-1", "alert", "temp-alert-1", "in")
                .connect("filter-1", "normal", "temp-file-1", "in")
                .connect("timer-1", "out", "humidity-sensor-1", "trigger")
                .connect("humidity-sensor-1", "out", "humidity-filter-1", "in")
                .connect("humidity-filter-1", "alert", "humidity-alert-1", "in")
                .connect("humidity-filter-1", "normal", "humidity-log-1", "in");

        FlowEngine engine = new FlowEngine();
        engine.register(flow);

        try {
            engine.startFlow("monitoring");
            Thread.sleep(10000);
            engine.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
