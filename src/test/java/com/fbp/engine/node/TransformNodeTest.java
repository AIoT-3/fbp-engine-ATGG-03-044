package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformNodeTest {
    private TransformNode transformNode;
    private Connection connection;
    @BeforeEach
    void setUp() {
        transformNode = new TransformNode(
                "transform-1", message -> message.withEntry("temperatureC", 30.0)
        );
        connection = new LocalConnection();
        transformNode.getOutputPort("out").connect(connection);
    }
    @Test
    @DisplayName("변환 정상 동작")
    void TransformTest() throws InterruptedException{
        Message message = new Message(Map.of("temperatureF", 86.0));
        transformNode.process(message);
        Message received = connection.poll();
        assertEquals(Double.valueOf(30.0),received.get("temperatureC"));
    }
    @Test
    @DisplayName("null 반환 시 미전달")
    void NullTest(){
        TransformNode nullTransformNode = new TransformNode(
                "transform-null",
            message -> null
        );
        Connection nullConnection = new LocalConnection();
        nullTransformNode.getOutputPort("out").connect(nullConnection);
        Message nullMessage = new Message(Map.of("temperatureF", 86.0));
        nullTransformNode.process(nullMessage);
        assertEquals(0,nullConnection.getBufferSize());
    }
    @Test
    @DisplayName("원본 메시지 불변")
    void noChangeTest() throws InterruptedException{
        TransformNode transformNode2 = new TransformNode(
                "transform-2",
                message ->  message.withEntry("temperatureC", 30.0)
        );
        Connection connection2 = new LocalConnection();
        transformNode2.getOutputPort("out").connect(connection2);
        Message message = new Message(Map.of("temperatureF", 86.0));

        transformNode2.process(message);
        Message received = connection2.poll();

        assertEquals(Double.valueOf(86.0),message.get("temperatureF"));
        assertFalse(message.hasKey("temperatureC"));
        assertEquals(Double.valueOf(30.0),received.get("temperatureC"));
    }

}