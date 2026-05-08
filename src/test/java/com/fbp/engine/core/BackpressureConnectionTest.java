package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BackpressureConnection - 큐 포화 시 전달 전략")
class BackpressureConnectionTest {
    @Test
    @DisplayName("Block 전략은 큐가 가득 차면 생산자 스레드를 대기시킨다")
    void blockStrategyWaitsWhenQueueIsFull() throws Exception {
        BackpressureConnection connection = new BackpressureConnection("c1", 1, new BlockBackpressureStrategy());
        connection.deliver(new Message(Map.of("seq", 1)));

        Thread producer = new Thread(() -> {
            try {
                connection.deliver(new Message(Map.of("seq", 2)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();

        Thread.sleep(200);
        assertTrue(producer.isAlive());

        assertEquals(1, ((Number) connection.poll().get("seq")).intValue());
        producer.join(1000);

        assertFalse(producer.isAlive());
        assertEquals(2, ((Number) connection.poll().get("seq")).intValue());
    }

    @Test
    @DisplayName("DropOldest 전략은 새 메시지가 오면 가장 오래된 메시지를 버린다")
    void dropOldestRemovesOldestMessage() throws InterruptedException {
        BackpressureConnection connection = new BackpressureConnection("c1", 1, new DropOldestStrategy());
        connection.deliver(new Message(Map.of("seq", 1)));
        connection.deliver(new Message(Map.of("seq", 2)));

        assertEquals(1, connection.getDroppedMessages());
        assertEquals(2, ((Number) connection.poll().get("seq")).intValue());
    }

    @Test
    @DisplayName("DropNewest 전략은 큐가 가득 차면 새 메시지를 버리고 기존 메시지를 유지한다")
    void dropNewestKeepsExistingMessage() throws InterruptedException {
        BackpressureConnection connection = new BackpressureConnection("c1", 1, new DropNewestStrategy());
        connection.deliver(new Message(Map.of("seq", 1)));
        connection.deliver(new Message(Map.of("seq", 2)));

        assertEquals(1, connection.getDroppedMessages());
        assertEquals(1, ((Number) connection.poll().get("seq")).intValue());
    }

    @Test
    @DisplayName("런타임에 백프레셔 전략을 변경하면 이후 메시지부터 새 전략이 적용된다")
    void strategyCanBeChangedAtRuntime() throws InterruptedException {
        BackpressureConnection connection = new BackpressureConnection("c1", 1, new DropNewestStrategy());
        connection.deliver(new Message(Map.of("seq", 1)));
        connection.deliver(new Message(Map.of("seq", 2)));
        assertEquals(1, ((Number) connection.poll().get("seq")).intValue());

        connection.setStrategy(new DropOldestStrategy());
        connection.deliver(new Message(Map.of("seq", 3)));
        connection.deliver(new Message(Map.of("seq", 4)));

        assertEquals(2, connection.getDroppedMessages());
        assertEquals(4, ((Number) connection.poll().get("seq")).intValue());
    }

    @Test
    @DisplayName("생성자에서 지정한 capacity가 실제 큐 크기 상한으로 적용된다")
    void capacityControlsQueueSize() throws InterruptedException {
        BackpressureConnection connection = new BackpressureConnection("c1", 2, new DropNewestStrategy());

        connection.deliver(new Message(Map.of("seq", 1)));
        connection.deliver(new Message(Map.of("seq", 2)));
        connection.deliver(new Message(Map.of("seq", 3)));

        assertEquals(2, connection.getBufferSize());
        assertEquals(1, connection.getDroppedMessages());
    }

    @Test
    @DisplayName("capacity가 0 이하이면 BackpressureConnection을 생성할 수 없다")
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> new BackpressureConnection("c1", 0, new DropNewestStrategy()));
    }

    @Test
    @DisplayName("Block 전략은 여러 생산자 스레드의 메시지를 드롭 없이 보존한다")
    void blockStrategyPreservesMessagesWithMultipleProducers() throws Exception {
        int producers = 4;
        int messagesPerProducer = 25;
        BackpressureConnection connection = new BackpressureConnection("c1", 200, new BlockBackpressureStrategy());
        CountDownLatch done = new CountDownLatch(producers);

        for (int i = 0; i < producers; i++) {
            final int producerId = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int seq = 0; seq < messagesPerProducer; seq++) {
                        connection.deliver(new Message(Map.of("producer", producerId, "seq", seq)));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            thread.start();
        }

        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertEquals(producers * messagesPerProducer, connection.getBufferSize());
        assertEquals(0, connection.getDroppedMessages());
    }
}
