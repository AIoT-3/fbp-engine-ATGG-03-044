package com.fbp.engine.core;

import com.fbp.engine.message.Message;

import java.util.concurrent.LinkedBlockingQueue;

public class BlockBackpressureStrategy implements BackpressureStrategy {
    @Override
    public BackpressureResult deliver(LinkedBlockingQueue<Message> buffer, Message message) throws InterruptedException {
        buffer.put(message);
        BackpressureResult result = BackpressureResult.acceptedResult();
        return result;
    }
}
