package com.fbp.engine.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DomainRawMetricSnapshot {
    private final String name;
    private final String flowId;
    private final String nodeId;
    private final String portName;
    private final Map<String, String> tags;
    private final double value;
    private final long timestampMillis;

    public DomainRawMetricSnapshot(String name, String flowId, String nodeId, String portName,
                                   Map<String, String> tags, double value, long timestampMillis) {
        this.name = name;
        this.flowId = flowId;
        this.nodeId = nodeId;
        this.portName = portName;
        if (tags == null) {
            this.tags = Collections.emptyMap();
        } else {
            this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        }
        this.value = value;
        this.timestampMillis = timestampMillis;
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

    public Map<String, String> getTags() {
        return tags;
    }

    public double getValue() {
        return value;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
