package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectorNodeTest {
    private CollectorNode collectorNode;

    @BeforeEach
    void setUp() {
        collectorNode = new CollectorNode("collector-1");
    }

    @Test
    @DisplayName("메시지 수집")
    void CollectMessageTest() {
        Message message = new Message(Map.of("temperature", 25.5));

        collectorNode.process(message);

        assertEquals(1, collectorNode.getCollected().size());
        assertSame(message, collectorNode.getCollected().get(0));
    }

    @Test
    @DisplayName("수집 순서 보존")
    void MessageOrderTest() {
        Message first = new Message(Map.of("seq", 1));
        Message second = new Message(Map.of("seq", 2));
        Message third = new Message(Map.of("seq", 3));

        collectorNode.process(first);
        collectorNode.process(second);
        collectorNode.process(third);

        assertSame(first, collectorNode.getCollected().get(0));
        assertSame(second, collectorNode.getCollected().get(1));
        assertSame(third, collectorNode.getCollected().get(2));
    }

    @Test
    @DisplayName("초기 상태 빈 리스트")
    void EmptyListTest() {
        assertTrue(collectorNode.getCollected().isEmpty());
    }

    @Test
    @DisplayName("InputPort 존재")
    void InputPortTest() {
        assertNotNull(collectorNode.getInputPort("in"));
    }

    @Test
    @DisplayName("파이프라인 연결 검증")
    void PipelineTest() throws InterruptedException {
        GeneratorNode generatorNode = new GeneratorNode("generator-1");
        Connection connection = new LocalConnection();

        generatorNode.getOutputPort().connect(connection);

        Message first = generatorNode.createMessage("temperature", 20.0);
        Message second = generatorNode.createMessage("temperature", 21.0);
        Message third = generatorNode.createMessage("temperature", 22.0);

        connection.deliver(first);
        connection.deliver(second);
        connection.deliver(third);

        collectorNode.process(connection.poll());
        collectorNode.process(connection.poll());
        collectorNode.process(connection.poll());

        assertEquals(3, collectorNode.getCollected().size());
        assertSame(first, collectorNode.getCollected().get(0));
        assertSame(second, collectorNode.getCollected().get(1));
        assertSame(third, collectorNode.getCollected().get(2));
    }
}