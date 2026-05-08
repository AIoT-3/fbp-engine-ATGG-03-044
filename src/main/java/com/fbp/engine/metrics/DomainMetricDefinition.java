package com.fbp.engine.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DomainMetricDefinition {
    private final String name;
    private final String flowId;
    private final String nodeId;
    private final String portName;
    private final String field;
    private final Map<String, String> tags;
    private final List<String> windows;

    public DomainMetricDefinition(String name, String flowId, String nodeId,
                                  String portName, String field, Map<String, String> tags) {
        this(name, flowId, nodeId, portName, field, tags, Collections.emptyList());
    }

    public DomainMetricDefinition(String name, String flowId, String nodeId,
                                  String portName, String field, Map<String, String> tags,
                                  List<String> windows) {
        this.name = requireText(name, "domain metric name은 필수입니다");
        this.flowId = requireText(flowId, "flowId는 필수입니다");
        this.nodeId = requireText(nodeId, "nodeId는 필수입니다");
        this.portName = requireText(portName, "portName은 필수입니다");
        this.field = requireText(field, "field는 필수입니다");
        if (tags == null) {
            this.tags = Collections.emptyMap();
        } else {
            this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        }
        if (windows == null) {
            this.windows = Collections.emptyList();
        } else {
            this.windows = List.copyOf(windows);
        }
    }

    public String key() {
        return flowId + ":" + nodeId + ":" + portName + ":" + name;
    }

    public boolean matches(String candidateFlowId, String candidateNodeId, String candidatePortName) {
        return flowId.equals(candidateFlowId)
                && nodeId.equals(candidateNodeId)
                && portName.equals(candidatePortName);
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

    public List<String> getWindows() {
        return windows;
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
