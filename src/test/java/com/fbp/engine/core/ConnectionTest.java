package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionTest {
    private Connection connection;

    @BeforeEach
    void setUp() {
        connection = new Connection();
    }

    @Test
    @DisplayName("deliver-poll 기본 동작")
    void DeliverPollTest() throws InterruptedException {
        Message message = new Message(Map.of("temperature", 25.5));

        connection.deliver(message);
        Message received = connection.poll();

        assertSame(message, received);
    }

    @Test
    @DisplayName("메시지 순서 보장")
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
        CountDownLatch latch = new CountDownLatch(1);

        Thread consumer = new Thread(() -> {
            try {
                receivedBox[0] = connection.poll();
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();
        connection.deliver(message);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertSame(message, receivedBox[0]);
    }

    @Test
    @DisplayName("poll 대기 동작")
    void PollBlockingTest() throws InterruptedException {
        Message[] receivedBox = new Message[1];
        CountDownLatch latch = new CountDownLatch(1);

        Thread consumer = new Thread(() -> {
            try {
                receivedBox[0] = connection.poll();
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();

        Thread.sleep(500);
        assertEquals(1, latch.getCount());

        Message message = new Message(Map.of("temperature", 25.5));
        connection.deliver(message);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
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

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);

        Thread producer = new Thread(() -> {
            try {
                started.countDown();
                limitedConnection.deliver(third);
                completed.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();

        assertTrue(started.await(1, TimeUnit.SECONDS));
        assertFalse(completed.await(500, TimeUnit.MILLISECONDS));

        limitedConnection.poll();

        assertTrue(completed.await(1, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("버퍼 크기 조회")
    void BufferSizeTest() throws InterruptedException {
        connection.deliver(new Message(Map.of("seq", 1)));
        connection.deliver(new Message(Map.of("seq", 2)));

        assertEquals(2, connection.getBufferSize());
    }
}