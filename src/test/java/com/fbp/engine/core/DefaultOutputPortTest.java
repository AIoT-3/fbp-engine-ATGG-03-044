package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultOutputPortTest {
    private DefaultOutputPort outputPort;
    @BeforeEach
    public void setUp() {
        outputPort = new DefaultOutputPort("out");
    }
    @Test
    @DisplayName("단일 Connection 전달")
    void OneConnectionTest() throws InterruptedException {
        Connection connection = mock(Connection.class);
        Message message = new Message(Map.of("temperature", 25.5));

        outputPort.connect(connection);
        outputPort.send(message);

        verify(connection).deliver(message);
    }
    @Test
    @DisplayName("다중 Connection 전달 (1:N)")
    void MultiConnectionsTest() throws InterruptedException{
        Connection first = mock(Connection.class);
        Connection second = mock(Connection.class);
        Message message = new Message(Map.of("temperature", 25.5));

        outputPort.connect(first);
        outputPort.connect(second);
        outputPort.send(message);

        verify(first).deliver(message);
        verify(second).deliver(message);
    }
    @Test
    @DisplayName("Connection 미연결 시")
    void sNoConnectionTest() {
        Message message = new Message(Map.of("temperature", 25.5));

        assertDoesNotThrow(() -> outputPort.send(message));
    }
}
