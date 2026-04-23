package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DefaultInputPortTest {
    private TestNode owner;
    private DefaultInputPort inputPort;

    @BeforeEach
    void setUp() {
        owner = new TestNode("owner-1");
        inputPort = new DefaultInputPort("in", owner);
    }

    @Test
    @DisplayName("receive 시 owner 호출")
    void ReceiveOwnerTest() {
        Message message = new Message(Map.of("temperature", 25.5));

        inputPort.receive(message);

        assertSame(message, owner.getReceived());
    }

    @Test
    @DisplayName("포트 이름 확인")
    void NameTest() {
        assertEquals("in", inputPort.getName());
    }

    static class TestNode extends AbstractNode {
        private Message received;

        TestNode(String id) {
            super(id);
        }

        @Override
        protected void onProcess(Message message) {
            received = message;
        }

        Message getReceived() {
            return received;
        }
    }
}
