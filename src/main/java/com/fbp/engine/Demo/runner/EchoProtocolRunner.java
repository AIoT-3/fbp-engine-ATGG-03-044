// 과제 1-4: EchoProtocolNode 연결, 송수신, 종료 확인 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.EchoProtocolNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class EchoProtocolRunner {
    public static void main(String[] args) {
        EchoProtocolNode node = new EchoProtocolNode(
                "echo-1",
                Map.of(
                        "host", "localhost",
                        "port", 12345
                )
        );
        Connection connection = new LocalConnection();
        node.getOutputPort("out").connect(connection);

        node.initialize();
        log.info("{}", node.getConnectionState());

        try {
            node.getInputPort("in").receive(new Message(Map.of("payload", "Hello FBP")));
            Message response = connection.poll();
            log.info("{}", String.valueOf(response.get("response")));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        node.shutdown();
        log.info("{}", node.getConnectionState());
    }
}
