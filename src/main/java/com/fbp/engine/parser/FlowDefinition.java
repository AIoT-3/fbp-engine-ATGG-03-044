package com.fbp.engine.parser;

import com.fbp.engine.metrics.DomainMetricDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FlowDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final TransportDefinition transport;
    private final List<NodeDefinition> nodes;
    private final List<ConnectionDefinition> connections;
    private final List<DomainMetricDefinition> domainMetrics;
    private final Map<String, NodeDefinition> nodeIndex;

    public FlowDefinition(String id, String name, String description,
                          List<NodeDefinition> nodes,
                          List<ConnectionDefinition> connections) {
        this(id, name, description, TransportDefinition.local(), nodes, connections, Collections.emptyList());
    }

    public FlowDefinition(String id, String name, String description,
                          TransportDefinition transport,
                          List<NodeDefinition> nodes,
                          List<ConnectionDefinition> connections) {
        this(id, name, description, transport, nodes, connections, Collections.emptyList());
    }

    public FlowDefinition(String id, String name, String description,
                          TransportDefinition transport,
                          List<NodeDefinition> nodes,
                          List<ConnectionDefinition> connections,
                          List<DomainMetricDefinition> domainMetrics) {
        this.id = ParserValidation.requireText(id, "플로우 id는 필수입니다");
        this.name = name;
        this.description = description;
        if (transport == null) {
            this.transport = TransportDefinition.local();
        } else {
            this.transport = transport;
        }
        List<NodeDefinition> checkedNodes = ParserValidation.requireNonEmptyList(nodes, "nodes는 하나 이상 필요합니다");
        this.nodes = List.copyOf(checkedNodes);

        List<ConnectionDefinition> sourceConnections;
        if (connections == null) {
            sourceConnections = Collections.emptyList();
        } else {
            sourceConnections = connections;
        }
        this.connections = List.copyOf(sourceConnections);

        if (domainMetrics == null) {
            this.domainMetrics = Collections.emptyList();
        } else {
            this.domainMetrics = List.copyOf(domainMetrics);
        }

        this.nodeIndex = buildNodeIndex(this.nodes);
        validateConnections();
        validateDomainMetrics();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TransportDefinition getTransport() {
        return transport;
    }

    public List<NodeDefinition> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<ConnectionDefinition> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    public List<DomainMetricDefinition> getDomainMetrics() {
        return Collections.unmodifiableList(domainMetrics);
    }

    public Optional<NodeDefinition> getNode(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(nodeIndex.get(id));
    }

    private Map<String, NodeDefinition> buildNodeIndex(List<NodeDefinition> nodeDefinitions) {
        Map<String, NodeDefinition> index = new LinkedHashMap<>();
        for (NodeDefinition node : nodeDefinitions) {
            String nodeId = node.getId();
            NodeDefinition previousNode = index.put(nodeId, node);
            if (previousNode != null) {
                throw new FlowParserException("중복 노드 id입니다: " + nodeId);
            }
        }
        return Collections.unmodifiableMap(index);
    }

    private void validateConnections() {
        for (ConnectionDefinition connection : connections) {
            NodeEndpoint from = connection.getFrom();
            NodeEndpoint to = connection.getTo();
            validateNodeReference(from.getNodeId());
            validateNodeReference(to.getNodeId());
        }
    }

    private void validateDomainMetrics() {
        for (DomainMetricDefinition domainMetric : domainMetrics) {
            validateNodeReference(domainMetric.getNodeId());
        }
    }

    private void validateNodeReference(String nodeId) {
        if (!nodeIndex.containsKey(nodeId)) {
            throw new FlowParserException("연결이 존재하지 않는 노드를 참조합니다: " + nodeId);
        }
    }
}
