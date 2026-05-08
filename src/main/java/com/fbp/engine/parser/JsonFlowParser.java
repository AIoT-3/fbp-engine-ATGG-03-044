package com.fbp.engine.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.metrics.DomainMetricDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonFlowParser implements FlowParser {
    private final ObjectMapper objectMapper;

    public JsonFlowParser() {
        this(new ObjectMapper());
    }

    public JsonFlowParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public FlowDefinition parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new FlowParserException("inputStream은 null일 수 없습니다");
        }
        try {
            JsonNode root = objectMapper.readTree(inputStream);
            if (root == null || !root.isObject()) {
                throw new FlowParserException("플로우 정의는 JSON object여야 합니다");
            }
            String id = requiredText(root, "id");
            String name = optionalText(root, "name");
            String description = optionalText(root, "description");
            TransportDefinition transport = parseTransport(root);
            List<NodeDefinition> nodes = parseNodes(root);
            List<ConnectionDefinition> connections = parseConnections(root);
            List<DomainMetricDefinition> domainMetrics = parseDomainMetrics(id, root);
            FlowDefinition definition = new FlowDefinition(id, name, description, transport, nodes, connections, domainMetrics);
            return definition;
        } catch (FlowParserException e) {
            throw e;
        } catch (IOException e) {
            throw new FlowParserException("JSON 파싱 실패", e);
        }
    }

    private List<NodeDefinition> parseNodes(JsonNode root) {
        JsonNode nodesNode = root.get("nodes");
        if (nodesNode == null || !nodesNode.isArray()) {
            throw new FlowParserException("nodes 배열은 필수입니다");
        }
        if (nodesNode.isEmpty()) {
            throw new FlowParserException("nodes는 하나 이상 필요합니다");
        }

        List<NodeDefinition> nodes = new ArrayList<>();
        for (JsonNode node : nodesNode) {
            if (!node.isObject()) {
                throw new FlowParserException("node 정의는 object여야 합니다");
            }
            Map<String, Object> config = Map.of();
            JsonNode configNode = node.get("config");
            if (configNode != null && !configNode.isNull()) {
                if (!configNode.isObject()) {
                    throw new FlowParserException("node config는 object여야 합니다");
                }
                TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {
                };
                config = objectMapper.convertValue(configNode, mapType);
            }

            String id = requiredText(node, "id");
            String type = requiredText(node, "type");
            NodeDefinition nodeDefinition = new NodeDefinition(id, type, config);
            nodes.add(nodeDefinition);
        }
        return nodes;
    }

    private TransportDefinition parseTransport(JsonNode root) {
        JsonNode transportNode = root.get("transport");
        if (transportNode == null || transportNode.isNull()) {
            return TransportDefinition.local();
        }
        if (!transportNode.isObject()) {
            throw new FlowParserException("transport는 object여야 합니다");
        }

        String type = requiredText(transportNode, "type");
        String broker = optionalText(transportNode, "broker");
        int qos = 0;
        JsonNode qosNode = transportNode.get("qos");
        if (qosNode != null && !qosNode.isNull()) {
            if (!qosNode.isInt()) {
                throw new FlowParserException("transport qos는 정수여야 합니다");
            }
            qos = qosNode.asInt();
        }

        TransportDefinition transport = new TransportDefinition(type, broker, qos);
        return transport;
    }

    private List<ConnectionDefinition> parseConnections(JsonNode root) {
        JsonNode connectionsNode = root.get("connections");
        if (connectionsNode == null || connectionsNode.isNull()) {
            return List.of();
        }
        if (!connectionsNode.isArray()) {
            throw new FlowParserException("connections는 배열이어야 합니다");
        }

        List<ConnectionDefinition> connections = new ArrayList<>();
        for (JsonNode connection : connectionsNode) {
            if (!connection.isObject()) {
                throw new FlowParserException("connection 정의는 object여야 합니다");
            }
            String fromText = requiredText(connection, "from");
            String toText = requiredText(connection, "to");
            NodeEndpoint from = NodeEndpoint.parse(fromText);
            NodeEndpoint to = NodeEndpoint.parse(toText);
            ConnectionDefinition connectionDefinition = new ConnectionDefinition(from, to);
            connections.add(connectionDefinition);
        }
        return connections;
    }

    private List<DomainMetricDefinition> parseDomainMetrics(String flowId, JsonNode root) {
        JsonNode metricsNode = root.get("metrics");
        if (metricsNode == null || metricsNode.isNull()) {
            return List.of();
        }
        if (!metricsNode.isObject()) {
            throw new FlowParserException("metrics는 object여야 합니다");
        }

        JsonNode domainNode = metricsNode.get("domain");
        if (domainNode == null || domainNode.isNull()) {
            return List.of();
        }
        if (!domainNode.isArray()) {
            throw new FlowParserException("metrics.domain은 배열이어야 합니다");
        }

        List<DomainMetricDefinition> definitions = new ArrayList<>();
        for (JsonNode metricNode : domainNode) {
            if (!metricNode.isObject()) {
                throw new FlowParserException("domain metric 정의는 object여야 합니다");
            }
            String name = requiredText(metricNode, "name");
            String field = requiredText(metricNode, "field");
            JsonNode sourceNode = metricNode.get("source");
            if (sourceNode == null || !sourceNode.isObject()) {
                throw new FlowParserException("domain metric source는 object여야 합니다");
            }
            String nodeId = requiredText(sourceNode, "node");
            String portName = requiredText(sourceNode, "port");

            Map<String, String> tags = parseStringMap(metricNode.get("tags"), "tags");
            List<String> windows = parseStringList(metricNode.get("windows"), "windows");
            definitions.add(new DomainMetricDefinition(name, flowId, nodeId, portName, field, tags, windows));
        }
        return definitions;
    }

    private Map<String, String> parseStringMap(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return Collections.emptyMap();
        }
        if (!node.isObject()) {
            throw new FlowParserException(fieldName + "는 object여야 합니다");
        }
        TypeReference<Map<String, String>> mapType = new TypeReference<Map<String, String>>() {
        };
        return objectMapper.convertValue(node, mapType);
    }

    private List<String> parseStringList(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return Collections.emptyList();
        }
        if (!node.isArray()) {
            throw new FlowParserException(fieldName + "는 배열이어야 합니다");
        }
        TypeReference<List<String>> listType = new TypeReference<List<String>>() {
        };
        return objectMapper.convertValue(node, listType);
    }

    private String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull() || !value.isTextual()) {
            throw new FlowParserException(fieldName + " 필드는 필수 문자열입니다");
        }
        return ParserValidation.requireText(value.asText(), fieldName + " 필드는 필수 문자열입니다");
    }

    private String optionalText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new FlowParserException(fieldName + " 필드는 문자열이어야 합니다");
        }
        return value.asText();
    }
}
