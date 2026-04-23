package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionTest {
    private Connection connection;

    @BeforeEach
    void setUp() {
        connection = new Connection();
    }

    @Test
    @DisplayName("deliver 후 target 수신")
    void DeliverTargetTest() throws InterruptedException {
        Message message = new Message(Map.of("temperature", 25.5));
        TestTargetNode targetNode = new TestTargetNode("target-1");

        connection.deliver(message);
        targetNode.getInputPort("in").receive(connection.poll());

        assertSame(message, targetNode.getReceived());
    }

    @Test
    @DisplayName("target 미설정 시 동작")
    void NoTargetTest() {
        Message message = new Message(Map.of("temperature", 25.5));

        assertDoesNotThrow(() -> connection.deliver(message));
    }

    @Test
    @DisplayName("다수 메시지 순서 보장")
    void MessageOrderTest() throws InterruptedException {
        Message first = new Message(Map.of("seq", 1));
        Message second = new Message(Map.of("seq", 2));
        Message third = new Message(Map.of("seq", 3));

        connection.deliver(first);
        connection.deliver(second);
        connection.deliver(third);

        assertSame(first, connection.poll());
        assertSame(second, connection.poll());
        assertSame(third, connection.poll());
    }

    @Test
    @DisplayName("멀티스레드 deliver-poll")
    void MultiThreadDeliverPollTest() throws InterruptedException {
        Message message = new Message(Map.of("temperature", 25.5));
        Message[] receivedBox = new Message[1];

        Thread consumer = new Thread(() -> {
            try {
                receivedBox[0] = connection.poll();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();
        connection.deliver(message);
        consumer.join(2000);

        assertFalse(consumer.isAlive());
        assertSame(message, receivedBox[0]);
    }

    @Test
    @DisplayName("poll 대기 동작")
    void PollBlockingTest() throws InterruptedException {
        Message[] receivedBox = new Message[1];

        Thread consumer = new Thread(() -> {
            try {
                receivedBox[0] = connection.poll();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();

        Thread.sleep(500);
        assertTrue(consumer.isAlive());

        Message message = new Message(Map.of("temperature", 25.5));
        connection.deliver(message);

        consumer.join(2000);

        assertFalse(consumer.isAlive());
        assertSame(message, receivedBox[0]);
    }

    @Test
    @DisplayName("버퍼 크기 제한")
    void BufferLimitTest() throws InterruptedException {
        Connection limitedConnection = new Connection(2);

        Message first = new Message(Map.of("seq", 1));
        Message second = new Message(Map.of("seq", 2));
        Message third = new Message(Map.of("seq", 3));

        limitedConnection.deliver(first);
        limitedConnection.deliver(second);

        Thread producer = new Thread(() -> {
            try {
                limitedConnection.deliver(third);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();

        Thread.sleep(500);
        assertTrue(producer.isAlive());

        limitedConnection.poll();

        producer.join(2000);

        assertFalse(producer.isAlive());
    }

    @Test
    @DisplayName("버퍼 크기 확인")
    void BufferSizeTest() throws InterruptedException {
        connection.deliver(new Message(Map.of("seq", 1)));
        connection.deliver(new Message(Map.of("seq", 2)));

        assertEquals(2, connection.getBufferSize());
    }

    static class TestTargetNode extends AbstractNode {
        private Message received;

        TestTargetNode(String id) {
            super(id);
            addInputPort("in");
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
