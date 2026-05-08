package com.fbp.engine.core;

import com.fbp.engine.message.Message;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class LocalConnection implements Connection {
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final String id;
    protected final LinkedBlockingQueue<Message> buffer;

    public LocalConnection() {
        this("connection-" + SEQUENCE.incrementAndGet(), 100);
    }

    public LocalConnection(int capacity) {
        this("connection-" + SEQUENCE.incrementAndGet(), capacity);
    }

    public LocalConnection(String id) {
        this(id, 100);
    }

    public LocalConnection(String id, int capacity) {
        this.id = id;
        this.buffer = new LinkedBlockingQueue<>(capacity);
    }

    public LocalConnection(String id, LinkedBlockingQueue<Message> buffer) {
        this.id = id;
        this.buffer = buffer;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void deliver(Message message) throws InterruptedException {
        buffer.put(message);
    }

    @Override
    public Message poll() throws InterruptedException {
        return buffer.take();
    }

    @Override
    public int getBufferSize() {
        return buffer.size();
    }
}
