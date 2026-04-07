package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultInputPortTest {
    private Node owner;
    private DefaultInputPort inputPort;

    @BeforeEach
    public void setUp() {
        owner = mock(Node.class);
        inputPort = new DefaultInputPort("in",owner);
    }
    @Test
    @DisplayName("receive 시 owner 호출")
    void shouldCallOwnerProcessWhenReceive() {
        Message message = new Message(Map.of("temperature", 25.5));

        inputPort.receive(message);

        verify(owner).process(message);
    }

    @Test
    @DisplayName("포트 이름 확인")
    void shouldReturnGivenPortName() {
        assertEquals("in", inputPort.getName());
    }
}
