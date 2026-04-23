// 과제 6-2: TransformNode 화씨 -> 섭씨 변환 플로우 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TransformNode;
// 과제 6-2: TransformNode를 활용한 화씨→섭씨 변환 플로우 실행
public class TransformNodeRunner {
    private static volatile boolean running = true;
    public static void main(String[] args) {
        GeneratorNode generatorNode = new GeneratorNode("generator-1");
        TransformNode transformNode = new TransformNode(
                "transform-1",
                message -> {
                    Double fahrenheit = message.get("temperatureF");
                    if (fahrenheit == null) {
                        return null;
                    }

                    double celsius = (fahrenheit - 32) * 5 / 9;
                    return message.withoutKey("temperatureF")
                            .withEntry("temperatureC", celsius);
                }
        );
        PrintNode printNode = new PrintNode("printer-1");

        Connection connection1 = new Connection();
        Connection connection2 = new Connection();

        transformNode.getOutputPort("out").connect(connection2);

        Thread transformThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = connection1.poll();
                    transformNode.process(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        Thread printThread = new Thread(() -> {
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

        transformThread.start();
        printThread.start();

        try {
            Message message = generatorNode.createMessage("temperatureF", 86.0);
            connection1.deliver(message);

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        running = false;
        transformThread.interrupt();
        printThread.interrupt();

        try {
            transformThread.join();
            printThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
