package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultOutputPortTest {
    private DefaultOutputPort outputPort;
    @BeforeEach
    public void setUp() {
        outputPort = new DefaultOutputPort("out");
    }
    @Test
    @DisplayName("단일 Connection 전달")
    void OneConnectionTest() throws InterruptedException {
        Connection connection = new Connection();
        Message message = new Message(Map.of("temperature", 25.5));

        outputPort.connect(connection);
        outputPort.send(message);

        assertEquals(message, connection.poll());
    }
    @Test
    @DisplayName("다중 Connection 전달 (1:N)")
    void MultiConnectionsTest() throws InterruptedException{
        Connection first = new Connection();
        Connection second = new Connection();
        Message message = new Message(Map.of("temperature", 25.5));

        outputPort.connect(first);
        outputPort.connect(second);
        outputPort.send(message);

        assertEquals(message, first.poll());
        assertEquals(message, second.poll());
    }
    @Test
    @DisplayName("Connection 미연결 시")
    void sNoConnectionTest() {
        Message message = new Message(Map.of("temperature", 25.5));

        assertDoesNotThrow(() -> outputPort.send(message));
    }
}
