package com.fbp.engine.core;

import com.fbp.engine.message.Message;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class BackpressureConnection extends LocalConnection {
    private final AtomicLong droppedMessages = new AtomicLong();
    private volatile BackpressureStrategy strategy;

    public BackpressureConnection(String id, int capacity, BackpressureStrategy strategy) {
        super(id, capacity);
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity는 1 이상이어야 합니다");
        }
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
    }

    @Override
    public void deliver(Message message) throws InterruptedException {
        BackpressureResult result = strategy.deliver(buffer, message);
        int droppedCount = result.getDroppedCount();
        droppedMessages.addAndGet(droppedCount);
    }

    @Override
    public int getBufferSize() {
        return buffer.size();
    }

    @Override
    public Message poll() throws InterruptedException {
        return buffer.take();
    }

    public long getDroppedMessages() {
        return droppedMessages.get();
    }

    public void setStrategy(BackpressureStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
    }
}
