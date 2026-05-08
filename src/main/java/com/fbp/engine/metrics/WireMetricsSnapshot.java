package com.fbp.engine.metrics;

public class WireMetricsSnapshot {
    private final String wireId;
    private final long delivered;
    private final long bytes;
    private final long dropped;
    private final int queueSize;
    private final double mqttRttMillis;

    public WireMetricsSnapshot(String wireId, long delivered, long bytes, long dropped,
                               int queueSize, double mqttRttMillis) {
        this.wireId = wireId;
        this.delivered = delivered;
        this.bytes = bytes;
        this.dropped = dropped;
        this.queueSize = queueSize;
        this.mqttRttMillis = mqttRttMillis;
    }

    public String getWireId() {
        return wireId;
    }

    public long getDelivered() {
        return delivered;
    }

    public long getBytes() {
        return bytes;
    }

    public long getDropped() {
        return dropped;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public double getMqttRttMillis() {
        return mqttRttMillis;
    }
}
