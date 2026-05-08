package com.fbp.engine.core;

import com.fbp.engine.message.Message;

import java.util.concurrent.LinkedBlockingQueue;

public class DropNewestStrategy implements BackpressureStrategy {
    @Override
    public BackpressureResult deliver(LinkedBlockingQueue<Message> buffer, Message message) {
        if (buffer.offer(message)) {
            BackpressureResult accepted = BackpressureResult.acceptedResult();
            return accepted;
        }
        BackpressureResult dropped = BackpressureResult.droppedResult();
        return dropped;
    }
}
