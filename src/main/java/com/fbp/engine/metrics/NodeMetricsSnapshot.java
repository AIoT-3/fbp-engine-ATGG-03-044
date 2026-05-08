package com.fbp.engine.metrics;

public class NodeMetricsSnapshot {
    private final String nodeId;
    private final long processed;
    private final long errors;
    private final double averageTimeMillis;
    private final int queueSize;
    private final long inputCount;
    private final long outputCount;
    private final long inputBytes;
    private final long outputBytes;
    private final double p99TimeMillis;

    public NodeMetricsSnapshot(String nodeId, long processed, long errors,
                               double averageTimeMillis, int queueSize) {
        this(nodeId, processed, errors, averageTimeMillis, queueSize,
                0, 0, 0, 0, 0.0);
    }

    public NodeMetricsSnapshot(String nodeId, long processed, long errors,
                               double averageTimeMillis, int queueSize,
                               long inputCount, long outputCount,
                               long inputBytes, long outputBytes,
                               double p99TimeMillis) {
        this.nodeId = nodeId;
        this.processed = processed;
        this.errors = errors;
        this.averageTimeMillis = averageTimeMillis;
        this.queueSize = queueSize;
        this.inputCount = inputCount;
        this.outputCount = outputCount;
        this.inputBytes = inputBytes;
        this.outputBytes = outputBytes;
        this.p99TimeMillis = p99TimeMillis;
    }

    public String getNodeId() {
        return nodeId;
    }

    public long getProcessed() {
        return processed;
    }

    public long getErrors() {
        return errors;
    }

    public double getAverageTimeMillis() {
        return averageTimeMillis;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public long getInputCount() {
        return inputCount;
    }

    public long getOutputCount() {
        return outputCount;
    }

    public long getInputBytes() {
        return inputBytes;
    }

    public long getOutputBytes() {
        return outputBytes;
    }

    public double getP99TimeMillis() {
        return p99TimeMillis;
    }
}
