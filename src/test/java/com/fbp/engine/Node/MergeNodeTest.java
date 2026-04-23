package com.fbp.engine.Node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.MergeNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeNodeTest {
    private MergeNode mergeNode;
    private Connection connection;

    @BeforeEach
    void setUp() {
        mergeNode = new MergeNode("merge-1");
        connection = new Connection();
        mergeNode.getOutputPort("out").connect(connection);
    }

    @Test
    @DisplayName("양쪽 입력 수신")
    void InputTest() throws InterruptedException {
        Message message1 = new Message(Map.of("temperature", 25.0)).withEntry("_inputPort", "in-1");
        Message message2 = new Message(Map.of("humidity", 60.0)).withEntry("_inputPort", "in-2");

        mergeNode.process(message1);
        mergeNode.process(message2);

        Message received = connection.poll();

        assertEquals(Double.valueOf(25.0), received.get("temperature"));
        assertEquals(Double.valueOf(60.0), received.get("humidity"));
    }

    @Test
    @DisplayName("합쳐진 메시지 출력")
    void MergeMessageTest() throws InterruptedException {
        Message message1 = new Message(Map.of("sensorId", "temp-1")).withEntry("_inputPort", "in-1");
        Message message2 = new Message(Map.of("temperature", 31.5)).withEntry("_inputPort", "in-2");

        mergeNode.process(message1);
        mergeNode.process(message2);

        Message received = connection.poll();

        assertEquals("temp-1", received.get("sensorId"));
        assertEquals(Double.valueOf(31.5), received.get("temperature"));
        assertFalse(received.hasKey("_inputPort"));
    }

    @Test
    @DisplayName("한쪽만 도착 시 대기")
    void PendingTest() {
        Message message1 = new Message(Map.of("temperature", 25.0)).withEntry("_inputPort", "in-1");

        mergeNode.process(message1);

        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("포트 구성 확인")
    void PortTest() {
        assertNotNull(mergeNode.getInputPort("in-1"));
        assertNotNull(mergeNode.getInputPort("in-2"));
        assertNotNull(mergeNode.getOutputPort("out"));
    }

    @Test
    @DisplayName("입력 포트 태깅")
    void InputPortReceiveTest() throws InterruptedException {
        mergeNode.getInputPort("in-1").receive(new Message(Map.of("temperature", 25.0)));
        mergeNode.getInputPort("in-2").receive(new Message(Map.of("humidity", 60.0)));

        Message received = connection.poll();

        assertEquals(Double.valueOf(25.0), received.get("temperature"));
        assertEquals(Double.valueOf(60.0), received.get("humidity"));
        assertFalse(received.hasKey("_inputPort"));
    }

}
