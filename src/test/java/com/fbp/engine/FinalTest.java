package com.fbp.engine;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.core.State;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FinalTest {
    private List<Message> alertMessages;
    private List<Message> normalMessages;
    private List<Message> sensorMessages;
    private State stateAfterStart;
    private State stateAfterShutdown;
    private String filePath;

    private void deleteTestFile() {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    private void runScenario() throws Exception {
        filePath = "final-scenario-" + System.nanoTime() + ".log";

        TimerNode timerNode = new TimerNode("timer-1", 100);
        TemperatureSensorNode sensorNode = new TemperatureSensorNode("sensor-1", 15.0, 45.0);
        ThresholdFilterNode filterNode = new ThresholdFilterNode("filter-1", "temperature", 30.0);
        AlertNode alertNode = new AlertNode("alert-1");
        LogNode logNode = new LogNode("log-1");
        FileWriterNode fileWriterNode = new FileWriterNode("file-1", filePath);

        CollectorNode sensorCollector = new CollectorNode("sensor-collector");
        CollectorNode alertCollector = new CollectorNode("alert-collector");
        CollectorNode normalCollector = new CollectorNode("normal-collector");

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

        Connection sensorToCollector = new Connection();
        Connection alertToCollector = new Connection();
        Connection normalToCollector = new Connection();

        sensorNode.getOutputPort("out").connect(sensorToCollector);
        filterNode.getOutputPort("alert").connect(alertToCollector);
        filterNode.getOutputPort("normal").connect(normalToCollector);

        final boolean[] running = {true};

        Thread sensorCollectorThread = new Thread(() -> {
            while (running[0]) {
                try {
                    Message message = sensorToCollector.poll();
                    sensorCollector.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread alertCollectorThread = new Thread(() -> {
            while (running[0]) {
                try {
                    Message message = alertToCollector.poll();
                    alertCollector.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread normalCollectorThread = new Thread(() -> {
            while (running[0]) {
                try {
                    Message message = normalToCollector.poll();
                    normalCollector.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        FlowEngine engine = new FlowEngine();
        engine.register(flow);

        sensorCollectorThread.start();
        alertCollectorThread.start();
        normalCollectorThread.start();

        engine.startFlow("final-monitoring");
        stateAfterStart = engine.getState();

        Thread.sleep(2000);

        engine.shutdown();
        stateAfterShutdown = engine.getState();

        Thread.sleep(300);
        running[0] = false;

        sensorCollectorThread.interrupt();
        alertCollectorThread.interrupt();
        normalCollectorThread.interrupt();

        sensorCollectorThread.join();
        alertCollectorThread.join();
        normalCollectorThread.join();

        sensorMessages = sensorCollector.getCollected();
        alertMessages = alertCollector.getCollected();
        normalMessages = normalCollector.getCollected();
    }

    @Test
    @Tag("integration")
    @DisplayName("엔진 시작/종료")
    void EngineStateTest() throws Exception {
        runScenario();

        assertEquals(State.RUNNING, stateAfterStart);
        assertEquals(State.STOPPED, stateAfterShutdown);

        deleteTestFile();
    }

    @Test
    @Tag("integration")
    @DisplayName("alert 경로 정확성")
    void AlertPathTest() throws Exception {
        runScenario();

        for (Message message : alertMessages) {
            Double temperature = message.get("temperature");
            assertNotNull(temperature);
            assertTrue(temperature > 30.0);
        }

        deleteTestFile();
    }

    @Test
    @Tag("integration")
    @DisplayName("normal 경로 정확성")
    void NormalPathTest() throws Exception {
        runScenario();

        for (Message message : normalMessages) {
            Double temperature = message.get("temperature");
            assertNotNull(temperature);
            assertTrue(temperature <= 30.0);
        }

        deleteTestFile();
    }

    @Test
    @Tag("integration")
    @DisplayName("전체 분기 완전성")
    void TotalBranchTest() throws Exception {
        runScenario();

        assertEquals(sensorMessages.size(), alertMessages.size() + normalMessages.size());

        deleteTestFile();
    }

    @Test
    @Tag("integration")
    @DisplayName("파일 기록 검증")
    void FileWriteTest() throws Exception {
        runScenario();

        List<String> lines = Files.readAllLines(Path.of(filePath));
        assertEquals(normalMessages.size(), lines.size());

        deleteTestFile();
    }

    @Test
    @Tag("integration")
    @DisplayName("센서 데이터 형식")
    void MessageFormatTest() throws Exception {
        runScenario();

        for (Message message : sensorMessages) {
            assertTrue(message.hasKey("sensorId"));
            assertTrue(message.hasKey("temperature"));
            assertTrue(message.hasKey("unit"));
        }

        deleteTestFile();
    }

    @Test
    @Tag("integration")
    @DisplayName("온도 범위")
    void TemperatureRangeTest() throws Exception {
        runScenario();

        for (Message message : sensorMessages) {
            Double temperature = message.get("temperature");
            assertNotNull(temperature);
            assertTrue(temperature >= 15.0);
            assertTrue(temperature <= 45.0);
        }

        deleteTestFile();
    }
}
