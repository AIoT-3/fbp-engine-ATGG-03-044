package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.core.AbstractNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TemperatureMonitoringTest {
    private List<Message> alertMessages;
    private List<Message> normalMessages;
    private int tickCount;

    static class CollectorNode extends AbstractNode {
        private final List<Message> messages = new ArrayList<>();

        public CollectorNode(String id) {
            super(id);
            addInputPort("in");
        }

        @Override
        protected synchronized void onProcess(Message message) {
            messages.add(message);
        }

        public synchronized List<Message> getMessages() {
            return new ArrayList<>(messages);
        }
    }

    private void runFlow() throws InterruptedException {
        alertMessages = new ArrayList<>();
        normalMessages = new ArrayList<>();
        tickCount = 0;

        TimerNode timerNode = new TimerNode("timer-1", 100);
        TemperatureSensorNode sensorNode = new TemperatureSensorNode("sensor-1", 15.0, 45.0);
        ThresholdFilterNode filterNode = new ThresholdFilterNode("filter-1", "temperature", 30.0);
        CollectorNode alertCollector = new CollectorNode("alert-collector");
        CollectorNode normalCollector = new CollectorNode("normal-collector");

        Connection timerToSensor = new LocalConnection();
        Connection sensorToFilter = new LocalConnection();
        Connection filterToAlert = new LocalConnection();
        Connection filterToNormal = new LocalConnection();

        timerNode.getOutputPort("out").connect(timerToSensor);
        sensorNode.getOutputPort("out").connect(sensorToFilter);
        filterNode.getOutputPort("alert").connect(filterToAlert);
        filterNode.getOutputPort("normal").connect(filterToNormal);

        final boolean[] running = {true};

        Thread sensorThread = new Thread(() -> {
            while (running[0]) {
                try {
                    Message trigger = timerToSensor.poll();
                    tickCount++;
                    sensorNode.process(trigger);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread filterThread = new Thread(() -> {
            while (running[0]) {
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
            while (running[0]) {
                try {
                    Message message = filterToAlert.poll();
                    alertCollector.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread normalThread = new Thread(() -> {
            while (running[0]) {
                try {
                    Message message = filterToNormal.poll();
                    normalCollector.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        timerNode.initialize();

        sensorThread.start();
        filterThread.start();
        alertThread.start();
        normalThread.start();

        Thread.sleep(2000);

        timerNode.shutdown();
        Thread.sleep(300);

        running[0] = false;

        sensorThread.interrupt();
        filterThread.interrupt();
        alertThread.interrupt();
        normalThread.interrupt();

        sensorThread.join();
        filterThread.join();
        alertThread.join();
        normalThread.join();

        alertMessages = alertCollector.getMessages();
        normalMessages = normalCollector.getMessages();
    }

    @Test
    @Tag("integration")
    @DisplayName("alert 경로 검증")
    void AlertPathTest() throws InterruptedException {
        runFlow();

        for (Message message : alertMessages) {
            Double temperature = message.get("temperature");
            assertNotNull(temperature);
            assertTrue(temperature > 30.0);
        }
    }

    @Test
    @Tag("integration")
    @DisplayName("normal 경로 검증")
    void NormalPathTest() throws InterruptedException {
        runFlow();

        for (Message message : normalMessages) {
            Double temperature = message.get("temperature");
            assertNotNull(temperature);
            assertTrue(temperature <= 30.0);
        }
    }

    @Test
    @Tag("integration")
    @DisplayName("전체 메시지 수")
    void TotalMessageCountTest() throws InterruptedException {
        runFlow();

        assertEquals(tickCount, alertMessages.size() + normalMessages.size());
    }
}
