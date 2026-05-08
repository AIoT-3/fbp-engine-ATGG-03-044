package com.fbp.engine.performance;

public class PerformanceResult {
    private final long messageCount;
    private final long errorCount;
    private final long durationNanos;
    private final double throughputPerSecond;
    private final double averageLatencyMillis;
    private final double p99LatencyMillis;

    public PerformanceResult(long messageCount, long errorCount, long durationNanos,
                             double throughputPerSecond,
                             double averageLatencyMillis,
                             double p99LatencyMillis) {
        this.messageCount = messageCount;
        this.errorCount = errorCount;
        this.durationNanos = Math.max(0, durationNanos);
        this.throughputPerSecond = Math.max(0.0, throughputPerSecond);
        this.averageLatencyMillis = Math.max(0.0, averageLatencyMillis);
        this.p99LatencyMillis = Math.max(0.0, p99LatencyMillis);
    }

    public long getMessageCount() {
        return messageCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public double getThroughputPerSecond() {
        return throughputPerSecond;
    }

    public double getAverageLatencyMillis() {
        return averageLatencyMillis;
    }

    public double getP99LatencyMillis() {
        return p99LatencyMillis;
    }

    public double getErrorRate() {
        if (messageCount == 0) {
            return 0.0;
        }
        return (double) errorCount / (double) messageCount;
    }
}
