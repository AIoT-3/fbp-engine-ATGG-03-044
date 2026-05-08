package com.fbp.engine.parser;

public class ConnectionDefinition {
    private final NodeEndpoint from;
    private final NodeEndpoint to;

    public ConnectionDefinition(NodeEndpoint from, NodeEndpoint to) {
        if (from == null) {
            throw new FlowParserException("from 연결은 필수입니다");
        }
        if (to == null) {
            throw new FlowParserException("to 연결은 필수입니다");
        }

        this.from = from;
        this.to = to;
    }

    public NodeEndpoint getFrom() {
        return from;
    }

    public NodeEndpoint getTo() {
        return to;
    }
}
