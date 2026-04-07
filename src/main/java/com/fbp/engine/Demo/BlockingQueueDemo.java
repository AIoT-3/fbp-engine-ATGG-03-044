package com.fbp.engine.Demo;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingQueueDemo {
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private static final String END = "END";

    public static void main(String[] args) {
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                String message = "메시지-" + i;

                try {
                    queue.put(message);
                    System.out.println("[생산] " + message);
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            try {
                queue.put(END);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            System.out.println("[생산자 종료]");
        });
        Thread consumer = new Thread(() -> {
            while (true) {
                try {
                    String message = queue.take();

                    if (END.equals(message)) {
                        System.out.println("[소비자 종료]");
                        break;
                    }

                    System.out.println("[소비] " + message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        producer.start();
        consumer.start();
    }
}
