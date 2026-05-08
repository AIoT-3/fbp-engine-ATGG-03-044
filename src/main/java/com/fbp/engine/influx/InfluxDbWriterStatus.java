package com.fbp.engine.influx;

public class InfluxDbWriterStatus {
    private final boolean connected;
    private final int queueSize;
    private final int maxQueueSize;
    private final long totalWritten;
    private final long totalDropped;
    private final long lastFlushMillis;
    private final String lastError;

    public InfluxDbWriterStatus(boolean connected, int queueSize, int maxQueueSize,
                                long totalWritten, long totalDropped,
                                long lastFlushMillis, String lastError) {
        this.connected = connected;
        this.queueSize = queueSize;
        this.maxQueueSize = maxQueueSize;
        this.totalWritten = totalWritten;
        this.totalDropped = totalDropped;
        this.lastFlushMillis = lastFlushMillis;
        this.lastError = lastError;
    }

    public boolean isConnected() {
        return connected;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public long getTotalWritten() {
        return totalWritten;
    }

    public long getTotalDropped() {
        return totalDropped;
    }

    public long getLastFlushMillis() {
        return lastFlushMillis;
    }

    public String getLastError() {
        return lastError;
    }
}
