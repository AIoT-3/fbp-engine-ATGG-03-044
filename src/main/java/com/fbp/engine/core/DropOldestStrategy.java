package com.fbp.engine.core;

import com.fbp.engine.message.Message;

import java.util.concurrent.LinkedBlockingQueue;

public class DropOldestStrategy implements BackpressureStrategy {
    @Override
    public BackpressureResult deliver(LinkedBlockingQueue<Message> buffer, Message message) {
        // LinkedBlockingQueue는 내부 lock을 가진다.
        // 여기서 synchronized는 poll + offer 조합을 여러 producer 사이에서 한 덩어리로 보이게 하려는 추가 직렬화다.
        synchronized (buffer) {
            if (buffer.offer(message)) {
                BackpressureResult accepted = BackpressureResult.acceptedResult();
                return accepted;
            }

            buffer.poll();
            boolean added = buffer.offer(message);
            if (added) {
                BackpressureResult replacedOldest = new BackpressureResult(true, 1);
                return replacedOldest;
            }

            BackpressureResult droppedNewest = BackpressureResult.droppedResult();
            return droppedNewest;
        }
    }
}
