package com.fbp.engine.core;

import com.fbp.engine.message.Message;

import java.util.concurrent.LinkedBlockingQueue;

public interface BackpressureStrategy {
    BackpressureResult deliver(LinkedBlockingQueue<Message> buffer, Message message) throws InterruptedException;
}
