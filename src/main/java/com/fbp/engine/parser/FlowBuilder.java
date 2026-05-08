package com.fbp.engine.parser;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.registry.NodeRegistry;
import com.fbp.engine.transport.BridgeConnectionFactory;

import java.util.Objects;

public class FlowBuilder {
    private final NodeRegistry nodeRegistry;
    private final BridgeConnectionFactory connectionFactory;

    public FlowBuilder(NodeRegistry nodeRegistry) {
        this(nodeRegistry, new BridgeConnectionFactory());
    }

    public FlowBuilder(NodeRegistry nodeRegistry, BridgeConnectionFactory connectionFactory) {
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry must not be null");
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory must not be null");
    }

    public Flow build(FlowDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        Flow flow = new Flow(definition.getId());
        for (NodeDefinition nodeDefinition : definition.getNodes()) {
            String type = nodeDefinition.getType();
            String id = nodeDefinition.getId();
            AbstractNode node = nodeRegistry.create(type, id, nodeDefinition.getConfig());
            flow.addNode(node);
        }
        for (ConnectionDefinition connection : definition.getConnections()) {
            NodeEndpoint from = connection.getFrom();
            NodeEndpoint to = connection.getTo();
            Connection runtimeConnection = connectionFactory.create(definition.getId(), connection, definition.getTransport());
            flow.connect(
                    from.getNodeId(),
                    from.getPortName(),
                    to.getNodeId(),
                    to.getPortName(),
                    runtimeConnection
            );
        }
        return flow;
    }
}
