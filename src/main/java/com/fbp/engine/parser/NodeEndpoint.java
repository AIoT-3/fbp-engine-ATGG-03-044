package com.fbp.engine.parser;

public class NodeEndpoint {
    private final String nodeId;
    private final String portName;

    public NodeEndpoint(String nodeId, String portName) {
        this.nodeId = ParserValidation.requireText(nodeId, "연결의 노드 id는 비어 있을 수 없습니다");
        this.portName = ParserValidation.requireText(portName, "연결의 포트명은 비어 있을 수 없습니다");
    }

    public static NodeEndpoint parse(String value) {
        String trimmed = ParserValidation.requireText(value, "연결 정보는 비어 있을 수 없습니다");
        int separatorIndex = trimmed.indexOf(':');
        int lastSeparatorIndex = trimmed.lastIndexOf(':');
        boolean separatorMissing = separatorIndex <= 0;
        boolean separatorDuplicated = separatorIndex != lastSeparatorIndex;
        boolean portNameMissing = separatorIndex == trimmed.length() - 1;

        if (separatorMissing || separatorDuplicated || portNameMissing) {
            throw new FlowParserException("연결 형식은 nodeId:portName 이어야 합니다: " + value);
        }

        String parsedNodeId = trimmed.substring(0, separatorIndex);
        String parsedPortName = trimmed.substring(separatorIndex + 1);
        NodeEndpoint endpoint = new NodeEndpoint(parsedNodeId, parsedPortName);
        return endpoint;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getPortName() {
        return portName;
    }

    public String asText() {
        return nodeId + ":" + portName;
    }
}
