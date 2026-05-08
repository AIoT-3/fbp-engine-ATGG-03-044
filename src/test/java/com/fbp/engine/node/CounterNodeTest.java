package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CounterNodeTest {
    private CounterNode counterNode;
    private Connection connection;
    @BeforeEach
    void setUp() {
        counterNode = new CounterNode("counter-1");
        connection = new LocalConnection();
        counterNode.getOutputPort("out").connect(connection);
    }
    @Test
    @DisplayName("count 키 추가")
    void countTest() throws InterruptedException{
        Message message = new Message(Map.of("temperature",25.5));
        counterNode.process(message);
        Message received = connection.poll();
        assertEquals(Integer.valueOf(1),received.get("count"));
    }
    @Test
    @DisplayName("count 누적")
    void countKeepTest() throws InterruptedException{
        counterNode.process(new Message(Map.of("temperature", 10.0)));
        counterNode.process(new Message(Map.of("temperature", 20.0)));
        counterNode.process(new Message(Map.of("temperature", 30.0)));

        connection.poll();
        connection.poll();
        Message received = connection.poll();

        assertEquals(Integer.valueOf(3), received.get("count"));
    }
    @Test
    @DisplayName("원본 키 유지")
    void noChangeTest() throws InterruptedException{
        Message message = new Message(Map.of("temperature",25.5,"location","room1"));
        counterNode.process(message);
        Message received = connection.poll();
        assertEquals(Double.valueOf(25.5),message.get("temperature"));
        assertEquals("room1",received.get("location"));
        assertEquals(Integer.valueOf(1),received.get("count"));
    }

}