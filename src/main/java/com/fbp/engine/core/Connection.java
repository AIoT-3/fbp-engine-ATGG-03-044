package com.fbp.engine.core;

import com.fbp.engine.message.Message;

public interface Connection extends AutoCloseable {
    String getId();

    void deliver(Message message) throws InterruptedException;

    Message poll() throws InterruptedException;

    int getBufferSize();

    @Override
    default void close() {
    }
}
