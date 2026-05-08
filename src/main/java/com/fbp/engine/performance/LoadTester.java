package com.fbp.engine.performance;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.TransformNode;

import java.util.Arrays;
import java.util.Map;

public class LoadTester {
    public PerformanceResult runPassThrough(int messageCount) {
        if (messageCount <= 0) {
            throw new IllegalArgumentException("messageCount는 1 이상이어야 합니다");
        }

        TransformNode passThrough = new TransformNode("load-pass-through", message -> message);
        Connection output = new LocalConnection("load-output", messageCount);
        passThrough.getOutputPort("out").connect(output);

        long[] latencies = new long[messageCount];
        long errors = 0;
        long startedAt = System.nanoTime();

        for (int i = 0; i < messageCount; i++) {
            long messageStartedAt = System.nanoTime();
            try {
                Message message = new Message(Map.of("seq", i));
                passThrough.process(message);
                output.poll();
                latencies[i] = System.nanoTime() - messageStartedAt;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                errors++;
                latencies[i] = System.nanoTime() - messageStartedAt;
                break;
            } catch (RuntimeException e) {
                errors++;
                latencies[i] = System.nanoTime() - messageStartedAt;
            }
        }

        long durationNanos = System.nanoTime() - startedAt;
        double seconds = durationNanos / 1_000_000_000.0;
        double throughput;
        if (seconds == 0.0) {
            throughput = messageCount;
        } else {
            throughput = messageCount / seconds;
        }

        double averageLatencyMillis = averageMillis(latencies);
        double p99LatencyMillis = percentileMillis(latencies, 99.0);
        return new PerformanceResult(
                messageCount,
                errors,
                durationNanos,
                throughput,
                averageLatencyMillis,
                p99LatencyMillis
        );
    }

    private double averageMillis(long[] latencies) {
        long total = 0;
        for (long latency : latencies) {
            total += latency;
        }
        return (total / (double) latencies.length) / 1_000_000.0;
    }

    private double percentileMillis(long[] latencies, double percentile) {
        long[] copy = Arrays.copyOf(latencies, latencies.length);
        Arrays.sort(copy);
        int index = (int) Math.ceil((percentile / 100.0) * copy.length) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= copy.length) {
            index = copy.length - 1;
        }
        return copy[index] / 1_000_000.0;
    }
}
