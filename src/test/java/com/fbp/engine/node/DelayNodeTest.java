package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DelayNodeTest {
    private DelayNode delayNode;
    private Connection connection;
    @BeforeEach
    void setUp() {
        connection = new LocalConnection();
        delayNode = new DelayNode("delay-1", 1000);
        delayNode.getOutputPort("out").connect(connection);
    }
    @Test
    @DisplayName("지연 후 전달")
    void delayTest() throws InterruptedException{
        Message message = new Message();
        long start = System.currentTimeMillis();
        delayNode.process(message);
        Message received = connection.poll();
        long time = System.currentTimeMillis() - start;
        assertSame(message, received);
        assertTrue(time >= 1000);
    }
    @Test
    @DisplayName("메시지 내용 보존")
    void textTest() throws InterruptedException{
        delayNode.process(new Message(Map.of("temperature",25.5)));
        Message received = connection.poll();
        assertEquals(Double.valueOf(25.5),received.get("temperature"));

    }


}