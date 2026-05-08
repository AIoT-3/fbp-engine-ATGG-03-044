package com.fbp.engine.metrics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class DomainMetric {
    private final DomainMetricDefinition definition;
    private final AtomicLong count = new AtomicLong();
    private final Map<String, Map<TagSet, Map<Long, WindowBucket>>> windowBuckets = new LinkedHashMap<>();
    private final List<DomainRawMetricSnapshot> rawSnapshots = new ArrayList<>();
    private double sum;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    public DomainMetric(DomainMetricDefinition definition) {
        this.definition = definition;
        for (String window : definition.getWindows()) {
            windowBuckets.put(window, new LinkedHashMap<>());
        }
    }

    public synchronized void record(double value) {
        record(value, definition.getTags());
    }

    public synchronized void record(double value, Map<String, String> tags) {
        record(value, System.currentTimeMillis(), tags);
    }

    public synchronized void record(double value, long timestampMillis) {
        record(value, timestampMillis, definition.getTags());
    }

    public synchronized void record(double value, long timestampMillis, Map<String, String> tags) {
        Map<String, String> resolvedTags;
        if (tags == null) {
            resolvedTags = definition.getTags();
        } else {
            resolvedTags = new LinkedHashMap<>(tags);
        }
        count.incrementAndGet();
        sum += value;
        min = Math.min(min, value);
        max = Math.max(max, value);
        rawSnapshots.add(new DomainRawMetricSnapshot(
                definition.getName(),
                definition.getFlowId(),
                definition.getNodeId(),
                definition.getPortName(),
                resolvedTags,
                value,
                timestampMillis
        ));
        recordWindows(value, timestampMillis, resolvedTags);
    }

    public synchronized DomainMetricSnapshot snapshot() {
        long currentCount = count.get();
        double average;
        double currentMin;
        double currentMax;
        if (currentCount == 0) {
            average = 0.0;
            currentMin = 0.0;
            currentMax = 0.0;
        } else {
            average = sum / currentCount;
            currentMin = min;
            currentMax = max;
        }
        return new DomainMetricSnapshot(
                definition.getName(),
                definition.getFlowId(),
                definition.getNodeId(),
                definition.getPortName(),
                definition.getField(),
                definition.getTags(),
                currentCount,
                average,
                currentMin,
                currentMax
        );
    }

    public synchronized void reset() {
        count.set(0);
        sum = 0.0;
        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;
        for (Map<TagSet, Map<Long, WindowBucket>> bucketsByTags : windowBuckets.values()) {
            bucketsByTags.clear();
        }
        rawSnapshots.clear();
    }

    public synchronized List<DomainMetricWindowSnapshot> windowSnapshots() {
        List<DomainMetricWindowSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<String, Map<TagSet, Map<Long, WindowBucket>>> windowEntry : windowBuckets.entrySet()) {
            String window = windowEntry.getKey();
            for (Map.Entry<TagSet, Map<Long, WindowBucket>> tagEntry : windowEntry.getValue().entrySet()) {
                for (Map.Entry<Long, WindowBucket> bucketEntry : tagEntry.getValue().entrySet()) {
                    snapshots.add(bucketEntry.getValue().snapshot(
                            definition,
                            window,
                            bucketEntry.getKey(),
                            tagEntry.getKey().tags()
                    ));
                }
            }
        }
        return snapshots;
    }

    public synchronized List<DomainRawMetricSnapshot> drainRawSnapshots() {
        List<DomainRawMetricSnapshot> snapshots = new ArrayList<>(rawSnapshots);
        rawSnapshots.clear();
        return snapshots;
    }

    private void recordWindows(double value, long timestampMillis, Map<String, String> tags) {
        TagSet tagSet = new TagSet(tags);
        for (Map.Entry<String, Map<TagSet, Map<Long, WindowBucket>>> windowEntry : windowBuckets.entrySet()) {
            long windowMillis = windowMillis(windowEntry.getKey());
            long bucketStart = (timestampMillis / windowMillis) * windowMillis;
            Map<Long, WindowBucket> buckets = windowEntry.getValue().get(tagSet);
            if (buckets == null) {
                buckets = new LinkedHashMap<>();
                windowEntry.getValue().put(tagSet, buckets);
            }
            WindowBucket bucket = buckets.get(bucketStart);
            if (bucket == null) {
                bucket = new WindowBucket();
                buckets.put(bucketStart, bucket);
            }
            bucket.record(value);
        }
    }

    private long windowMillis(String window) {
        if ("1m".equals(window)) {
            return 60_000L;
        }
        if ("1h".equals(window)) {
            return 60L * 60_000L;
        }
        if ("1d".equals(window)) {
            return 24L * 60L * 60_000L;
        }
        throw new IllegalArgumentException("지원하지 않는 domain metric window입니다: " + window);
    }

    private static class WindowBucket {
        private long count;
        private double sum;
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;

        private void record(double value) {
            count++;
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        private DomainMetricWindowSnapshot snapshot(DomainMetricDefinition definition,
                                                    String window,
                                                    long bucketStartMillis,
                                                    Map<String, String> tags) {
            double average;
            double currentMin;
            double currentMax;
            if (count == 0) {
                average = 0.0;
                currentMin = 0.0;
                currentMax = 0.0;
            } else {
                average = sum / count;
                currentMin = min;
                currentMax = max;
            }
            return new DomainMetricWindowSnapshot(
                    definition.getName(),
                    window,
                    bucketStartMillis,
                    tags,
                    count,
                    average,
                    currentMin,
                    currentMax
            );
        }
    }

    private static class TagSet {
        private final Map<String, String> tags;

        private TagSet(Map<String, String> tags) {
            if (tags == null) {
                this.tags = Map.of();
            } else {
                this.tags = Map.copyOf(tags);
            }
        }

        private Map<String, String> tags() {
            return tags;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TagSet otherTagSet)) {
                return false;
            }
            return tags.equals(otherTagSet.tags);
        }

        @Override
        public int hashCode() {
            return tags.hashCode();
        }
    }
}
