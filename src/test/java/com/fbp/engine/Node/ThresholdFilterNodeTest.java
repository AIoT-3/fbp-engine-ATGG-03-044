package com.fbp.engine.Node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ThresholdFilterNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThresholdFilterNodeTest {
    private ThresholdFilterNode thresholdFilterNode;
    private Connection alertConnection;
    private Connection normalConnection;

    @BeforeEach
    void setUp() {
        thresholdFilterNode = new ThresholdFilterNode("filter-1", "temperature", 30.0);
        alertConnection = new Connection();
        normalConnection = new Connection();
        thresholdFilterNode.getOutputPort("alert").connect(alertConnection);
        thresholdFilterNode.getOutputPort("normal").connect(normalConnection);
    }
    @Test
    @DisplayName("초과 → alert 포트")
    void AlertTest() throws InterruptedException {
        Message message = new Message(Map.of("temperature", 35.0));

        thresholdFilterNode.process(message);

        Message received = alertConnection.poll();
        assertSame(message, received);
        assertEquals(0, normalConnection.getBufferSize());
    }

    @Test
    @DisplayName("이하 → normal 포트")
    void NormalTest() throws InterruptedException {
        Message message = new Message(Map.of("temperature", 25.0));

        thresholdFilterNode.process(message);

        Message received = normalConnection.poll();
        assertSame(message, received);
        assertEquals(0, alertConnection.getBufferSize());
    }

    @Test
    @DisplayName("경계값 (정확히 같은 값)")
    void EdgeTest() throws InterruptedException {
        Message message = new Message(Map.of("temperature", 30.0));

        thresholdFilterNode.process(message);

        Message received = normalConnection.poll();
        assertSame(message, received);
        assertEquals(0, alertConnection.getBufferSize());
    }

    @Test
    @DisplayName("키 없는 메시지")
    void NoKeyTest() {
        Message message = new Message(Map.of("humidity", 60.0));

        assertDoesNotThrow(() -> thresholdFilterNode.process(message));
        assertEquals(0, alertConnection.getBufferSize());
        assertEquals(0, normalConnection.getBufferSize());
    }

    @Test
    @DisplayName("양쪽 동시 검증")
    void BothPortTest() throws InterruptedException {
        Message alertMessage = new Message(Map.of("temperature", 35.0));
        Message normalMessage = new Message(Map.of("temperature", 20.0));

        thresholdFilterNode.process(alertMessage);
        thresholdFilterNode.process(normalMessage);

        Message receivedAlert = alertConnection.poll();
        Message receivedNormal = normalConnection.poll();

        assertSame(alertMessage, receivedAlert);
        assertSame(normalMessage, receivedNormal);
    }



}