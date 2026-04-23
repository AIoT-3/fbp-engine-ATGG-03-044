package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import lombok.Getter;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class Connection {
    private static final AtomicLong SEQUENCE = new AtomicLong();

    @Getter
    private final String id;
    private final LinkedBlockingQueue<Message> buffer;

    public Connection() {
        this("connection-" + SEQUENCE.incrementAndGet(), 100);
    }

    public Connection(int capacity) {
        this("connection-" + SEQUENCE.incrementAndGet(), capacity);
    }

    public Connection(String id) {
        this(id, 100);
    }

    public Connection(String id, int capacity) {
        this.id = id;
        this.buffer = new LinkedBlockingQueue<>(capacity);
    }

    // Test support constructor. Production code should use capacity-based constructors.
    public Connection(String id, LinkedBlockingQueue<Message> buffer) {
        this.id = id;
        this.buffer = buffer;
    }

    public void deliver(Message message) throws InterruptedException {
        buffer.put(message);
    }

    public int getBufferSize() {
        return buffer.size();
    }

    public Message poll() throws InterruptedException {
        return buffer.take();
    }
}
