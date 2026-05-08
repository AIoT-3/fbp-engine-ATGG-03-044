package com.fbp.engine.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WireMetrics {
    private final String wireId;
    private final AtomicLong delivered = new AtomicLong();
    private final AtomicLong bytes = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicInteger queueSize = new AtomicInteger();
    private final AtomicLong mqttRttNanos = new AtomicLong();
    private final AtomicLong mqttRttSamples = new AtomicLong();

    public WireMetrics(String wireId) {
        if (wireId == null || wireId.trim().isEmpty()) {
            throw new IllegalArgumentException("wireId는 비어 있을 수 없습니다");
        }
        this.wireId = wireId.trim();
    }

    public void recordDelivered(long byteCount, int currentQueueSize) {
        delivered.incrementAndGet();
        bytes.addAndGet(Math.max(0, byteCount));
        setQueueSize(currentQueueSize);
    }

    public void recordDropped() {
        dropped.incrementAndGet();
    }

    public void setQueueSize(int value) {
        queueSize.set(Math.max(0, value));
    }

    public void recordMqttRtt(long durationNanos) {
        mqttRttNanos.addAndGet(Math.max(0, durationNanos));
        mqttRttSamples.incrementAndGet();
    }

    public WireMetricsSnapshot snapshot() {
        long rttSamples = mqttRttSamples.get();
        double averageRttMillis;
        if (rttSamples == 0) {
            averageRttMillis = 0.0;
        } else {
            averageRttMillis = (mqttRttNanos.get() / 1_000_000.0) / rttSamples;
        }
        return new WireMetricsSnapshot(
                wireId,
                delivered.get(),
                bytes.get(),
                dropped.get(),
                queueSize.get(),
                averageRttMillis
        );
    }

    public void reset() {
        delivered.set(0);
        bytes.set(0);
        dropped.set(0);
        queueSize.set(0);
        mqttRttNanos.set(0);
        mqttRttSamples.set(0);
    }
}
