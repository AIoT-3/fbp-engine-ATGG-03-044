package com.fbp.engine.integration;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;

import java.util.concurrent.atomic.AtomicBoolean;

public final class TestNodeWorkerSupport {
    private TestNodeWorkerSupport() {
    }

    public static Thread startWorker(String name, Connection connection, AbstractNode node, AtomicBoolean running) {
        Thread thread = new Thread(() -> {
            while (running.get()) {
                try {
                    Message message = connection.poll();
                    node.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, name);
        thread.start();
        return thread;
    }

    public static void stopWorkers(AtomicBoolean running, Thread... threads) {
        running.set(false);
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
