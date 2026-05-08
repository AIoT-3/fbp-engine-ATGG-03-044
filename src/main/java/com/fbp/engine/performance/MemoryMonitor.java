package com.fbp.engine.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoryMonitor {
    private final List<Long> samples = new ArrayList<>();

    public long sampleUsedHeap() {
        long usedHeap = currentUsedHeap();
        samples.add(usedHeap);
        return usedHeap;
    }

    public long sampleUsedHeapAfterGc() {
        System.gc();
        return sampleUsedHeap();
    }

    public List<Long> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    public void reset() {
        samples.clear();
    }

    public boolean isLastSampleWithin(long maxBytes) {
        if (samples.isEmpty()) {
            return false;
        }
        long lastSample = samples.get(samples.size() - 1);
        return lastSample <= maxBytes;
    }

    private long currentUsedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
