package com.fbp.engine.transport;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.parser.ConnectionDefinition;
import com.fbp.engine.parser.NodeEndpoint;
import com.fbp.engine.parser.TransportDefinition;

public class BridgeConnectionFactory {
    private final MessageSerializer serializer;

    public BridgeConnectionFactory() {
        this(new JacksonMessageSerializer());
    }

    public BridgeConnectionFactory(MessageSerializer serializer) {
        this.serializer = serializer;
    }

    public Connection create(String flowId, ConnectionDefinition definition, TransportDefinition transport) {
        NodeEndpoint from = definition.getFrom();
        NodeEndpoint to = definition.getTo();
        String connectionId = connectionId(from, to);

        if (transport != null && transport.isMqtt()) {
            String topic = topic(flowId, from, to);
            return new MqttBridgeConnection(connectionId, transport.getBroker(), topic, transport.getQos(), serializer);
        }

        return new LocalConnection(connectionId);
    }

    public String topic(String flowId, NodeEndpoint from, NodeEndpoint to) {
        return "fbp/"
                + safeTopicPart(flowId)
                + "/"
                + safeTopicPart(from.getNodeId())
                + "."
                + safeTopicPart(from.getPortName())
                + "->"
                + safeTopicPart(to.getNodeId())
                + "."
                + safeTopicPart(to.getPortName());
    }

    private String connectionId(NodeEndpoint from, NodeEndpoint to) {
        return from.getNodeId() + ":" + from.getPortName() + "->" + to.getNodeId() + ":" + to.getPortName();
    }

    private String safeTopicPart(String value) {
        String trimmed = value.trim();
        return trimmed.replace('/', '_')
                .replace('+', '_')
                .replace('#', '_');
    }
}
