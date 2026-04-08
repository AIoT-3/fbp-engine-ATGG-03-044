package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;

import java.util.List;
public class TemperatureMonitorRunner {
    private static volatile boolean running = true;

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
        List<Connection> connections = flow.getConnections();
        Connection timerToSensor = connections.get(0);
        Connection sensorToFilter = connections.get(1);
        Connection filterToAlert = connections.get(2);
        Connection filterToLog = connections.get(3);
        Connection timerToHumiditySensor = connections.get(4);
        Connection humiditySensorToFilter = connections.get(5);
        Connection humidityFilterToAlert = connections.get(6);
        Connection humidityFilterToLog = connections.get(7);

        Thread tempSensorThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = timerToSensor.poll();
                    tempSensorNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread tempFilterThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = sensorToFilter.poll();
                    tempFilterNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread tempAlertThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = filterToAlert.poll();
                    tempAlertNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread tempFileThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = filterToLog.poll();
                    tempFileNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        Thread humiditySensorThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = timerToHumiditySensor.poll();
                    humiditySensorNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread humidityFilterThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = humiditySensorToFilter.poll();
                    humidityFilterNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread humidityAlertThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = humidityFilterToAlert.poll();
                    humidityAlertNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        Thread humidityLogThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = humidityFilterToLog.poll();
                    humidityLogNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        FlowEngine engine = new FlowEngine();
        engine.register(flow);

        tempSensorThread.start();
        tempFilterThread.start();
        tempAlertThread.start();
        tempFileThread.start();

        humiditySensorThread.start();
        humidityFilterThread.start();
        humidityAlertThread.start();
        humidityLogThread.start();

        try {
            engine.startFlow("monitoring");
            Thread.sleep(10000);
            engine.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running = false;
            tempSensorThread.interrupt();
            tempFilterThread.interrupt();
            tempAlertThread.interrupt();
            tempFileThread.interrupt();

            humiditySensorThread.interrupt();
            humidityFilterThread.interrupt();
            humidityAlertThread.interrupt();
            humidityLogThread.interrupt();

            try {
                tempSensorThread.join();
                tempFilterThread.join();
                tempAlertThread.join();
                tempFileThread.join();

                humiditySensorThread.join();
                humidityFilterThread.join();
                humidityAlertThread.join();
                humidityLogThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
