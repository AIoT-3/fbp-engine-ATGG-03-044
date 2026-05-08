package com.fbp.engine.flow;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SubFlowNode extends AbstractNode {
    private final Flow internalFlow;
    private final String inputNodeId;
    private final String inputPort;
    private final FlowEngine internalEngine;
    private boolean registered;
    private boolean running;

    public SubFlowNode(String id, Flow internalFlow) {
        this(id, internalFlow, "input", "in", "output", "out");
    }

    public SubFlowNode(String id, Flow internalFlow,
                       String inputNodeId, String inputPort,
                       String outputNodeId, String outputPort) {
        super(id);
        this.internalFlow = Objects.requireNonNull(internalFlow, "internalFlow must not be null");
        this.inputNodeId = requireText(inputNodeId, "inputNodeId");
        this.inputPort = requireText(inputPort, "inputPort");
        String checkedOutputNodeId = requireText(outputNodeId, "outputNodeId");
        String checkedOutputPort = requireText(outputPort, "outputPort");
        this.internalEngine = new FlowEngine();
        addInputPort("in");
        addOutputPort("out");
        addOutputPort("error");
        validateInputMapping();
        String bridgeNodeId = "__subflow_" + getId() + "_output";
        internalFlow.addNode(new SubFlowOutputBridgeNode(bridgeNodeId));
        internalFlow.connect(checkedOutputNodeId, checkedOutputPort, bridgeNodeId, "in");
    }

    @Override
    public synchronized void initialize() {
        if (!registered) {
            internalEngine.register(internalFlow);
            registered = true;
        }
        if (!running) {
            internalEngine.startFlow(internalFlow.getId());
            running = true;
        }
    }

    @Override
    public synchronized void shutdown() {
        if (running) {
            internalEngine.stopFlow(internalFlow.getId());
            running = false;
        }
    }

    @Override
    protected void onProcess(Message message) {
        try {
            internalFlow.getNodes().get(inputNodeId).getInputPort(inputPort).receive(message);
        } catch (RuntimeException e) {
            send("error", createErrorMessage(message, e));
        }
    }

    public Flow getInternalFlow() {
        return internalFlow;
    }

    private void validateInputMapping() {
        AbstractNode inputNode = internalFlow.getNodes().get(inputNodeId);
        if (inputNode == null) {
            throw new IllegalArgumentException("서브플로우 입력 노드가 없습니다: " + inputNodeId);
        }
        if (inputNode.getInputPort(inputPort) == null) {
            throw new IllegalArgumentException("서브플로우 입력 포트가 없습니다: " + inputNodeId + ":" + inputPort);
        }
    }

    private Message createErrorMessage(Message originalMessage, RuntimeException exception) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceNodeId", getId());
        payload.put("errorType", exception.getClass().getName());
        payload.put("errorMessage", exception.getMessage());
        payload.put("originalMessageId", originalMessage.getId().toString());
        payload.put("originalPayload", originalMessage.getPayload());
        return new Message(payload);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다");
        }
        return value.trim();
    }

    private class SubFlowOutputBridgeNode extends AbstractNode {
        private SubFlowOutputBridgeNode(String id) {
            super(id);
            addInputPort("in");
        }

        @Override
        protected void onProcess(Message message) {
            SubFlowNode.this.send("out", message);
        }
    }
}
