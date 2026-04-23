// 과제 5-3: 처리량과 지연 시간 성능 측정 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.RuleNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class Step5PerformanceRunner {
    public static void main(String[] args) throws Exception {
        runScenario(100);
        runScenario(500);
        runScenario(1000);
    }

    private static void runScenario(int messageCount) throws Exception {
        RuleNode ruleNode = new RuleNode("rule-1", "value > 30");
        Connection matchConnection = new Connection(messageCount + 10);
        Connection mismatchConnection = new Connection(messageCount + 10);

        ruleNode.getOutputPort("match").connect(matchConnection);
        ruleNode.getOutputPort("mismatch").connect(mismatchConnection);

        long start = System.nanoTime();
        long totalLatency = 0L;

        for (int i = 0; i < messageCount; i++) {
            Message message = new Message(Map.of("value", (double) i));
            long before = System.nanoTime();
            ruleNode.process(message);
            long after = System.nanoTime();
            totalLatency += (after - before);
        }

        int matchCount = 0;
        int mismatchCount = 0;
        while (matchConnection.getBufferSize() > 0) {
            matchConnection.poll();
            matchCount++;
        }
        while (mismatchConnection.getBufferSize() > 0) {
            mismatchConnection.poll();
            mismatchCount++;
        }

        long elapsed = System.nanoTime() - start;
        double throughput = messageCount / (elapsed / 1_000_000_000.0);
        double avgLatencyMicros = (totalLatency / (double) messageCount) / 1_000.0;

        log.info("messages={}", messageCount);
        log.info("match={}, mismatch={}", matchCount, mismatchCount);
        log.info("throughput={} msg/s", throughput);
        log.info("avgLatency={} us", avgLatencyMicros);
    }
}
