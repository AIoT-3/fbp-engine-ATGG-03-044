package com.fbp.engine.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DomainMetricWindowSnapshot {
    private final String name;
    private final String window;
    private final long bucketStartMillis;
    private final Map<String, String> tags;
    private final long count;
    private final double average;
    private final double min;
    private final double max;

    public DomainMetricWindowSnapshot(String name, String window, long bucketStartMillis,
                                      Map<String, String> tags, long count,
                                      double average, double min, double max) {
        this.name = name;
        this.window = window;
        this.bucketStartMillis = bucketStartMillis;
        if (tags == null) {
            this.tags = Collections.emptyMap();
        } else {
            this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        }
        this.count = count;
        this.average = average;
        this.min = min;
        this.max = max;
    }

    public String getName() {
        return name;
    }

    public String getWindow() {
        return window;
    }

    public long getBucketStartMillis() {
        return bucketStartMillis;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public long getCount() {
        return count;
    }

    public double getAverage() {
        return average;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
