package com.fbp.engine.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DomainMetricSnapshot {
    private final String name;
    private final String flowId;
    private final String nodeId;
    private final String portName;
    private final String field;
    private final Map<String, String> tags;
    private final long count;
    private final double average;
    private final double min;
    private final double max;

    public DomainMetricSnapshot(String name, String flowId, String nodeId, String portName,
                                String field, Map<String, String> tags, long count,
                                double average, double min, double max) {
        this.name = name;
        this.flowId = flowId;
        this.nodeId = nodeId;
        this.portName = portName;
        this.field = field;
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

    public String getFlowId() {
        return flowId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getPortName() {
        return portName;
    }

    public String getField() {
        return field;
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
