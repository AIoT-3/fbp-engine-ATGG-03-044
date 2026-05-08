package com.fbp.engine.core;

public class BackpressureResult {
    private final boolean accepted;
    private final int droppedCount;

    public BackpressureResult(boolean accepted, int droppedCount) {
        this.accepted = accepted;
        this.droppedCount = droppedCount;
    }

    public static BackpressureResult acceptedResult() {
        BackpressureResult result = new BackpressureResult(true, 0);
        return result;
    }

    public static BackpressureResult droppedResult() {
        BackpressureResult result = new BackpressureResult(false, 1);
        return result;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public int getDroppedCount() {
        return droppedCount;
    }
}
