package com.fbp.engine.demo;

import java.util.ArrayList;
import java.util.List;

public class SynchronizedBufferDemo {
    private static final List<String> buffer = new ArrayList<>();
    private static final String END = "END";

    public static void main(String[] args) {
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                String message = "메시지-" + i;

                synchronized (buffer) {
                    buffer.add(message);
                    System.out.println("[생산] " + message);
                    buffer.notify();
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            synchronized (buffer) {
                buffer.add(END);
                buffer.notify();
            }

            System.out.println("[생산자 종료]");
        });
        Thread consumer = new Thread(() -> {
            while (true) {
                String message;

                synchronized (buffer) {
                    while (buffer.isEmpty()) {
                        try {
                            buffer.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    message = buffer.remove(0);
                }

                if (END.equals(message)) {
                    System.out.println("[소비자 종료]");
                    break;
                }

                System.out.println("[소비] " + message);
            }
        });

        producer.start();
        consumer.start();
    }
}
