package com.fbp.engine.metrics;

import com.fbp.engine.message.Message;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MetricsCollector {
    private final ConcurrentMap<String, NodeMetrics> nodeMetrics = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WireMetrics> wireMetrics = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DomainMetricDefinition> domainDefinitions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DomainMetric> domainMetrics = new ConcurrentHashMap<>();

    public long startTimer() {
        return System.nanoTime();
    }

    public void recordProcessing(String nodeId, long startNanos, boolean success) {
        long durationNanos = System.nanoTime() - startNanos;
        NodeMetrics metrics = getOrCreate(nodeId);
        if (success) {
            metrics.recordSuccess(durationNanos);
        } else {
            metrics.recordError(durationNanos);
        }
    }

    public void recordSuccess(String nodeId, long durationNanos) {
        getOrCreate(nodeId).recordSuccess(durationNanos);
    }

    public void recordError(String nodeId, long durationNanos) {
        getOrCreate(nodeId).recordError(durationNanos);
    }

    public void setQueueSize(String nodeId, int queueSize) {
        getOrCreate(nodeId).setQueueSize(queueSize);
    }

    public void recordNodeInput(String nodeId, String portName, long byteCount) {
        getOrCreate(nodeId).recordInput(byteCount);
    }

    public void recordNodeOutput(String nodeId, String portName, long byteCount) {
        getOrCreate(nodeId).recordOutput(byteCount);
    }

    public void recordWireDelivered(String wireId, long byteCount, int queueSize) {
        getOrCreateWire(wireId).recordDelivered(byteCount, queueSize);
    }

    public void recordWireDropped(String wireId) {
        getOrCreateWire(wireId).recordDropped();
    }

    public void setWireQueueSize(String wireId, int queueSize) {
        getOrCreateWire(wireId).setQueueSize(queueSize);
    }

    public void recordWireMqttRtt(String wireId, long durationNanos) {
        getOrCreateWire(wireId).recordMqttRtt(durationNanos);
    }

    public NodeMetricsSnapshot getNodeMetrics(String nodeId) {
        return getOrCreate(nodeId).snapshot();
    }

    public WireMetricsSnapshot getWireMetrics(String wireId) {
        return getOrCreateWire(wireId).snapshot();
    }

    public FlowMetrics getFlowMetrics(String flowId, Collection<String> nodeIds) {
        return getFlowMetrics(flowId, nodeIds, Collections.emptyList());
    }

    public FlowMetrics getFlowMetrics(String flowId, Collection<String> nodeIds, Collection<String> wireIds) {
        List<NodeMetricsSnapshot> snapshots = new ArrayList<>();
        for (String nodeId : nodeIds) {
            NodeMetricsSnapshot snapshot = getNodeMetrics(nodeId);
            snapshots.add(snapshot);
        }
        List<WireMetricsSnapshot> wireSnapshots = new ArrayList<>();
        for (String wireId : wireIds) {
            WireMetricsSnapshot snapshot = getWireMetrics(wireId);
            wireSnapshots.add(snapshot);
        }
        FlowMetrics metrics = new FlowMetrics(flowId, snapshots, wireSnapshots);
        return metrics;
    }

    public void registerDomainMetric(DomainMetricDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("domain metric definition은 null일 수 없습니다");
        }
        domainDefinitions.put(definition.key(), definition);
        domainMetrics.putIfAbsent(definition.key(), new DomainMetric(definition));
    }

    public void recordDomainMessage(String flowId, String nodeId, String portName, Message message) {
        for (DomainMetricDefinition definition : domainDefinitions.values()) {
            if (!definition.matches(flowId, nodeId, portName)) {
                continue;
            }
            Object value = message.get(definition.getField());
            Double numericValue = toDouble(value);
            if (numericValue != null) {
                Map<String, String> tags = resolveTags(definition, message);
                domainMetrics.get(definition.key()).record(numericValue, tags);
            }
        }
    }

    public List<DomainMetricSnapshot> getDomainMetrics() {
        List<DomainMetricSnapshot> snapshots = new ArrayList<>();
        for (DomainMetric metric : domainMetrics.values()) {
            snapshots.add(metric.snapshot());
        }
        return Collections.unmodifiableList(snapshots);
    }

    public List<DomainMetricWindowSnapshot> getDomainWindowMetrics() {
        List<DomainMetricWindowSnapshot> snapshots = new ArrayList<>();
        for (DomainMetric metric : domainMetrics.values()) {
            snapshots.addAll(metric.windowSnapshots());
        }
        return Collections.unmodifiableList(snapshots);
    }

    public List<DomainRawMetricSnapshot> drainDomainRawMetrics() {
        List<DomainRawMetricSnapshot> snapshots = new ArrayList<>();
        for (DomainMetric metric : domainMetrics.values()) {
            snapshots.addAll(metric.drainRawSnapshots());
        }
        return Collections.unmodifiableList(snapshots);
    }

    public Map<String, DomainMetricSnapshot> getDomainMetricsByName() {
        Map<String, DomainMetricSnapshot> snapshots = new LinkedHashMap<>();
        for (DomainMetric metric : domainMetrics.values()) {
            DomainMetricSnapshot snapshot = metric.snapshot();
            snapshots.put(snapshot.getName(), snapshot);
        }
        return Collections.unmodifiableMap(snapshots);
    }

    public void reset() {
        nodeMetrics.values().forEach(NodeMetrics::reset);
        wireMetrics.values().forEach(WireMetrics::reset);
        domainMetrics.values().forEach(DomainMetric::reset);
    }

    private NodeMetrics getOrCreate(String nodeId) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new IllegalArgumentException("nodeId는 비어 있을 수 없습니다");
        }
        String normalizedNodeId = nodeId.trim();
        NodeMetrics existingMetrics = nodeMetrics.get(normalizedNodeId);
        if (existingMetrics != null) {
            return existingMetrics;
        }

        NodeMetrics newMetrics = new NodeMetrics(normalizedNodeId);
        NodeMetrics previousMetrics = nodeMetrics.putIfAbsent(normalizedNodeId, newMetrics);
        if (previousMetrics != null) {
            return previousMetrics;
        }
        return newMetrics;
    }

    private WireMetrics getOrCreateWire(String wireId) {
        if (wireId == null || wireId.trim().isEmpty()) {
            throw new IllegalArgumentException("wireId는 비어 있을 수 없습니다");
        }
        String normalizedWireId = wireId.trim();
        WireMetrics existingMetrics = wireMetrics.get(normalizedWireId);
        if (existingMetrics != null) {
            return existingMetrics;
        }

        WireMetrics newMetrics = new WireMetrics(normalizedWireId);
        WireMetrics previousMetrics = wireMetrics.putIfAbsent(normalizedWireId, newMetrics);
        if (previousMetrics != null) {
            return previousMetrics;
        }
        return newMetrics;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            if ("open".equalsIgnoreCase(text)) {
                return 1.0;
            }
            if ("close".equalsIgnoreCase(text) || "closed".equalsIgnoreCase(text)) {
                return 0.0;
            }
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Map<String, String> resolveTags(DomainMetricDefinition definition, Message message) {
        Map<String, String> resolvedTags = new LinkedHashMap<>();
        for (Map.Entry<String, String> tag : definition.getTags().entrySet()) {
            String rawValue = tag.getValue();
            if (rawValue != null && rawValue.startsWith("field:")) {
                String fieldPath = rawValue.substring("field:".length());
                Object fieldValue = message.get(fieldPath);
                if (fieldValue != null) {
                    resolvedTags.put(tag.getKey(), String.valueOf(fieldValue));
                }
            } else if (rawValue != null) {
                resolvedTags.put(tag.getKey(), rawValue);
            }
        }
        return resolvedTags;
    }
}
