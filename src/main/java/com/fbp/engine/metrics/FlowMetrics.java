package com.fbp.engine.metrics;

import java.util.Collections;
import java.util.List;

public class FlowMetrics {
    private final String flowId;
    private final List<NodeMetricsSnapshot> nodes;
    private final List<WireMetricsSnapshot> wires;

    public FlowMetrics(String flowId, List<NodeMetricsSnapshot> nodes) {
        this(flowId, nodes, Collections.emptyList());
    }

    public FlowMetrics(String flowId, List<NodeMetricsSnapshot> nodes, List<WireMetricsSnapshot> wires) {
        this.flowId = flowId;
        List<NodeMetricsSnapshot> copiedNodes = List.copyOf(nodes);
        this.nodes = copiedNodes;
        List<WireMetricsSnapshot> copiedWires = List.copyOf(wires);
        this.wires = copiedWires;
    }

    public String getFlowId() {
        return flowId;
    }

    public List<NodeMetricsSnapshot> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<WireMetricsSnapshot> getWires() {
        return Collections.unmodifiableList(wires);
    }

    public long getTotalProcessed() {
        long total = 0;
        for (NodeMetricsSnapshot node : nodes) {
            total += node.getProcessed();
        }
        return total;
    }

    public long getTotalErrors() {
        long total = 0;
        for (NodeMetricsSnapshot node : nodes) {
            total += node.getErrors();
        }
        return total;
    }

    public long getTotalWireDelivered() {
        long total = 0;
        for (WireMetricsSnapshot wire : wires) {
            total += wire.getDelivered();
        }
        return total;
    }
}
