package com.fbp.engine.Demo;

import java.util.ArrayList;
import java.util.List;

public class UnSafeBufferDemo {
    public static void main(String[] args) {
        List<String> buffer = new ArrayList<>();

        Thread producer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                String message = "메시지-" + i;
                buffer.add(message);
                System.out.println("[생산] " + message);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            System.out.println("[생산자 종료]");
        });

        Thread consumer = new Thread(() -> {
            while (true) {
                if (!buffer.isEmpty()) {
                    String message = buffer.remove(0);
                    System.out.println("[소비] " + message);
                }
            }
        });

        producer.start();
        consumer.start();
    }
}