package com.fbp.engine.core;

import com.fbp.engine.core.interfaces.OutputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.metrics.MessageMetrics;
import com.fbp.engine.metrics.MetricsCollector;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class DefaultOutputPort implements OutputPort {
    private final String name;
    private final String ownerNodeId;
    private final List<Connection> connections = new ArrayList<>();
    private MetricsCollector metricsCollector;
    private String flowId;

    public DefaultOutputPort(String name) {
        this(name, null);
    }

    public DefaultOutputPort(String name, String ownerNodeId) {
        this.name = name;
        this.ownerNodeId = ownerNodeId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void connect(Connection connection) {
        connections.add(Objects.requireNonNull(connection));
    }

    public void attachMetrics(String flowId, MetricsCollector metricsCollector) {
        this.flowId = flowId;
        this.metricsCollector = metricsCollector;
    }

    public void detachMetrics() {
        this.flowId = null;
        this.metricsCollector = null;
    }

    @Override
    public void send(Message message) {
        for (Connection connection : connections) {
            try {
                connection.deliver(message);
                recordMetrics(connection, message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("메시지 전달이 중단됨: port={}, connection={}", name, connection.getId(), e);
                return;
            }
        }
    }

    private void recordMetrics(Connection connection, Message message) {
        if (metricsCollector == null || ownerNodeId == null) {
            return;
        }
        long byteCount = MessageMetrics.estimateBytes(message);
        metricsCollector.recordNodeOutput(ownerNodeId, name, byteCount);
        metricsCollector.recordWireDelivered(connection.getId(), byteCount, connection.getBufferSize());
        metricsCollector.recordDomainMessage(flowId, ownerNodeId, name, message);
    }
}
