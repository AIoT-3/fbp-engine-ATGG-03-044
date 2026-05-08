// 과제 4-5: 3노드 스레드 파이프라인 실행 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.PrintNode;
import lombok.extern.slf4j.Slf4j;

// 과제 4-5: 3노드 스레드 파이프라인 실행
@Slf4j
public class ThreeNodeRunner {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        GeneratorNode generatorNode = new GeneratorNode("generator-1");
        FilterNode filterNode = new FilterNode("filter-1", "temperature", 22.0);
        PrintNode printNode = new PrintNode("printer-1");

        Connection connection1 = new LocalConnection();
        Connection connection2 = new LocalConnection();

        Thread generatorThread = Thread.ofVirtual().start(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    Message message = generatorNode.createMessage("temperature", 20.0 + i);
                    connection1.deliver(message);
                    log.info("[생산자] 메시지 전송: {}", message.getPayload());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread filterThread = Thread.ofVirtual().start(() -> {
            while (running) {
                try {
                    Message message = connection1.poll();
                    if (filterNode.matches(message)) {
                        connection2.deliver(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread printThread = Thread.ofVirtual().start(() -> {
            while (running) {
                try {
                    Message message = connection2.poll();
                    printNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        try {
            generatorThread.join();
            Thread.sleep(1000);
            running = false;
            filterThread.interrupt();
            printThread.interrupt();
            filterThread.join();
            printThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
