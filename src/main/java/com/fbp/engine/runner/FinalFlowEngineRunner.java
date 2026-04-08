package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;

import java.util.List;

public class FinalFlowEngineRunner {
    private static volatile boolean running = true;

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

        List<Connection> connections = flow.getConnections();
        Connection timerToSensor = connections.get(0);
        Connection sensorToFilter = connections.get(1);
        Connection filterToAlert = connections.get(2);
        Connection filterToLog = connections.get(3);
        Connection logToFile = connections.get(4);

        Thread sensorThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = timerToSensor.poll();
                    sensorNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread filterThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = sensorToFilter.poll();
                    filterNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread alertThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = filterToAlert.poll();
                    alertNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread logThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = filterToLog.poll();
                    logNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread fileThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = logToFile.poll();
                    fileWriterNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        FlowEngine engine = new FlowEngine();
        engine.register(flow);

        sensorThread.start();
        filterThread.start();
        alertThread.start();
        logThread.start();
        fileThread.start();

        try {
            engine.startFlow("final-monitoring");
            Thread.sleep(10000);
            engine.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running = false;

            sensorThread.interrupt();
            filterThread.interrupt();
            alertThread.interrupt();
            logThread.interrupt();
            fileThread.interrupt();

            try {
                sensorThread.join();
                filterThread.join();
                alertThread.join();
                logThread.join();
                fileThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

