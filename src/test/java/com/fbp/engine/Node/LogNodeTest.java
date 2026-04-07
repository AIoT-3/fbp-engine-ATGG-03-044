package com.fbp.engine.Node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.LogNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogNodeTest {
    private LogNode logNode;
    private Connection connection;
    @BeforeEach
    void setUp() {
        logNode = new LogNode("log-1");
        connection = new Connection();
        logNode.getOutputPort("out").connect(connection);
    }
    @Test
    @DisplayName("메시지 통과 전달")
    void messageThrowTest() throws InterruptedException{
        Message message = new Message(Map.of("temperature", 25.5));
        logNode.process(message);
        Message received = connection.poll();
        assertSame(message, received);

    }
    @Test
    @DisplayName("중간 삽입 가능")
    void insertTest() throws InterruptedException{
        Connection connection1 = new Connection();
        Connection connection2 = new Connection();
        LogNode middle = new LogNode("log-middle");
        middle.getOutputPort("out").connect(connection2);
        Message message = new Message(Map.of("temperature", 25.5));
        connection1.deliver(message);
        Message received1 = connection1.poll();
        middle.process(received1);
        Message received2 = connection2.poll();
        assertSame(message, received2);

    }

}
