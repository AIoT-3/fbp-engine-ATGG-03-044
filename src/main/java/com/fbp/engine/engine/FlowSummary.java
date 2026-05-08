package com.fbp.engine.engine;

import com.fbp.engine.core.State;

public class FlowSummary {
    private final String id;
    private final String name;
    private final State status;
    private final String transportType;
    private final int nodeCount;
    private final int wireCount;

    public FlowSummary(String id, String name, State status) {
        this(id, name, status, "unknown", 0, 0);
    }

    public FlowSummary(String id, String name, State status, String transportType, int nodeCount, int wireCount) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.transportType = transportType;
        this.nodeCount = nodeCount;
        this.wireCount = wireCount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public State getStatus() {
        return status;
    }

    public String getTransportType() {
        return transportType;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getWireCount() {
        return wireCount;
    }
}
