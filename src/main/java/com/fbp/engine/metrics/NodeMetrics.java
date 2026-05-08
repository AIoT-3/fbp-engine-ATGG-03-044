package com.fbp.engine.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeMetrics {
    private static final int MAX_DURATION_SAMPLES = 2048;

    private final String nodeId;
    private final AtomicLong processed = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong totalProcessingNanos = new AtomicLong();
    private final AtomicInteger queueSize = new AtomicInteger();
    private final AtomicLong inputCount = new AtomicLong();
    private final AtomicLong outputCount = new AtomicLong();
    private final AtomicLong inputBytes = new AtomicLong();
    private final AtomicLong outputBytes = new AtomicLong();
    private final List<Long> durationSamples = new ArrayList<>();

    public NodeMetrics(String nodeId) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new IllegalArgumentException("nodeId는 비어 있을 수 없습니다");
        }
        this.nodeId = nodeId.trim();
    }

    public void recordSuccess(long durationNanos) {
        processed.incrementAndGet();
        long safeDuration = Math.max(0, durationNanos);
        totalProcessingNanos.addAndGet(safeDuration);
        addDurationSample(safeDuration);
    }

    public void recordError(long durationNanos) {
        processed.incrementAndGet();
        errors.incrementAndGet();
        long safeDuration = Math.max(0, durationNanos);
        totalProcessingNanos.addAndGet(safeDuration);
        addDurationSample(safeDuration);
    }

    public void recordInput(long byteCount) {
        inputCount.incrementAndGet();
        inputBytes.addAndGet(Math.max(0, byteCount));
    }

    public void recordOutput(long byteCount) {
        outputCount.incrementAndGet();
        outputBytes.addAndGet(Math.max(0, byteCount));
    }

    public void setQueueSize(int value) {
        queueSize.set(Math.max(0, value));
    }

    public NodeMetricsSnapshot snapshot() {
        long processedCount = processed.get();
        double averageMillis;
        if (processedCount == 0) {
            averageMillis = 0.0;
        } else {
            long totalNanos = totalProcessingNanos.get();
            double totalMillis = totalNanos / 1_000_000.0;
            averageMillis = totalMillis / processedCount;
        }

        long errorCount = errors.get();
        int currentQueueSize = queueSize.get();
        double p99Millis = percentileMillis(99.0);
        NodeMetricsSnapshot snapshot = new NodeMetricsSnapshot(
                nodeId,
                processedCount,
                errorCount,
                averageMillis,
                currentQueueSize,
                inputCount.get(),
                outputCount.get(),
                inputBytes.get(),
                outputBytes.get(),
                p99Millis
        );
        return snapshot;
    }

    public void reset() {
        processed.set(0);
        errors.set(0);
        totalProcessingNanos.set(0);
        queueSize.set(0);
        inputCount.set(0);
        outputCount.set(0);
        inputBytes.set(0);
        outputBytes.set(0);
        synchronized (durationSamples) {
            durationSamples.clear();
        }
    }

    private void addDurationSample(long durationNanos) {
        synchronized (durationSamples) {
            durationSamples.add(durationNanos);
            if (durationSamples.size() > MAX_DURATION_SAMPLES) {
                durationSamples.remove(0);
            }
        }
    }

    private double percentileMillis(double percentile) {
        List<Long> copy;
        synchronized (durationSamples) {
            if (durationSamples.isEmpty()) {
                return 0.0;
            }
            copy = new ArrayList<>(durationSamples);
        }
        Collections.sort(copy);
        int index = (int) Math.ceil((percentile / 100.0) * copy.size()) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= copy.size()) {
            index = copy.size() - 1;
        }
        return copy.get(index) / 1_000_000.0;
    }
}
