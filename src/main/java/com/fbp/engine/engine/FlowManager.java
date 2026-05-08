package com.fbp.engine.engine;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.core.Connection;
import com.fbp.engine.core.State;
import com.fbp.engine.parser.FlowBuilder;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.DomainMetricDefinition;
import com.fbp.engine.registry.NodeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FlowManager {
    private final FlowEngine flowEngine;
    private final FlowBuilder flowBuilder;
    private final Map<String, FlowDefinition> definitions = new LinkedHashMap<>();

    public FlowManager(FlowEngine flowEngine, NodeRegistry nodeRegistry) {
        this.flowEngine = Objects.requireNonNull(flowEngine, "flowEngine must not be null");
        this.flowBuilder = new FlowBuilder(Objects.requireNonNull(nodeRegistry, "nodeRegistry must not be null"));
    }

    public synchronized Flow deploy(FlowDefinition definition) {
        return deploy(definition, true);
    }

    public synchronized Flow deploy(FlowDefinition definition, boolean startImmediately) {
        Objects.requireNonNull(definition, "definition must not be null");
        if (flowEngine.getFlows().containsKey(definition.getId())) {
            throw new FlowManagerException(FlowManagerException.ErrorType.CONFLICT,
                    "이미 배포된 플로우 id입니다: " + definition.getId());
        }
        Flow flow = flowBuilder.build(definition);
        registerDomainMetrics(definition);
        flowEngine.register(flow);
        definitions.put(definition.getId(), definition);
        if (startImmediately) {
            flowEngine.startFlow(definition.getId());
        }
        return flow;
    }

    private void registerDomainMetrics(FlowDefinition definition) {
        MetricsCollector metricsCollector = flowEngine.getMetricsCollector();
        for (DomainMetricDefinition domainMetric : definition.getDomainMetrics()) {
            metricsCollector.registerDomainMetric(domainMetric);
        }
    }

    /**
     * 수동으로 구성한 Flow를 배포하기 위한 호환 API.
     * 설정 기반 재배포와 definition 조회가 필요한 경우 deploy(FlowDefinition)을 사용한다.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public synchronized Flow deploy(Flow flow) {
        Objects.requireNonNull(flow, "flow must not be null");
        if (flowEngine.getFlows().containsKey(flow.getId())) {
            throw new FlowManagerException(FlowManagerException.ErrorType.CONFLICT,
                    "이미 배포된 플로우 id입니다: " + flow.getId());
        }
        flowEngine.register(flow);
        flowEngine.startFlow(flow.getId());
        return flow;
    }

    public synchronized List<FlowSummary> list() {
        List<FlowSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, Flow> entry : flowEngine.getFlows().entrySet()) {
            FlowDefinition definition = definitions.get(entry.getKey());
            String name;
            if (definition == null) {
                name = null;
            } else {
                name = definition.getName();
            }
            String id = entry.getKey();
            State status = flowEngine.getFlowState(id);
            String transportType = "unknown";
            int nodeCount = entry.getValue().getNodes().size();
            int wireCount = entry.getValue().getConnections().size();
            if (definition != null) {
                transportType = definition.getTransport().getType();
                nodeCount = definition.getNodes().size();
                wireCount = definition.getConnections().size();
            }
            FlowSummary summary = new FlowSummary(id, name, status, transportType, nodeCount, wireCount);
            summaries.add(summary);
        }
        return Collections.unmodifiableList(summaries);
    }

    public synchronized State getStatus(String flowId) {
        return flowEngine.getFlowState(flowId);
    }

    public synchronized void stop(String flowId) {
        ensureExists(flowId);
        flowEngine.stopFlow(flowId);
    }

    public synchronized void start(String flowId) {
        ensureExists(flowId);
        flowEngine.startFlow(flowId);
    }

    public synchronized void restart(String flowId) {
        ensureExists(flowId);
        if (flowEngine.getFlowState(flowId) == State.RUNNING) {
            flowEngine.stopFlow(flowId);
        }
        flowEngine.startFlow(flowId);
    }

    public synchronized void remove(String flowId) {
        ensureExists(flowId);
        flowEngine.unregister(flowId);
        definitions.remove(flowId);
    }

    public synchronized FlowDefinition getDefinition(String flowId) {
        ensureExists(flowId);
        FlowDefinition definition = definitions.get(flowId);
        if (definition == null) {
            throw new FlowManagerException(FlowManagerException.ErrorType.NOT_FOUND,
                    "설정 기반 정의가 없는 플로우입니다: " + flowId);
        }
        return definition;
    }

    public synchronized int getFlowCount() {
        return flowEngine.getFlows().size();
    }

    public synchronized State getEngineState() {
        return flowEngine.getState();
    }

    public synchronized MetricsCollector getMetricsCollector() {
        return flowEngine.getMetricsCollector();
    }

    public synchronized List<String> getNodeIds(String flowId) {
        ensureExists(flowId);
        FlowDefinition definition = definitions.get(flowId);
        if (definition != null) {
            List<String> nodeIds = new ArrayList<>();
            for (com.fbp.engine.parser.NodeDefinition node : definition.getNodes()) {
                nodeIds.add(node.getId());
            }
            return Collections.unmodifiableList(nodeIds);
        }

        List<String> nodeIds = new ArrayList<>(flowEngine.getFlows().get(flowId).getNodes().keySet());
        return Collections.unmodifiableList(nodeIds);
    }

    public synchronized List<String> getConnectionIds(String flowId) {
        ensureExists(flowId);
        List<String> connectionIds = new ArrayList<>();
        for (com.fbp.engine.core.Connection connection : flowEngine.getFlows().get(flowId).getConnections()) {
            connectionIds.add(connection.getId());
        }
        return Collections.unmodifiableList(connectionIds);
    }

    public synchronized List<Connection> getConnections(String flowId) {
        ensureExists(flowId);
        List<Connection> connections = new ArrayList<>(flowEngine.getFlows().get(flowId).getConnections());
        return Collections.unmodifiableList(connections);
    }

    private void ensureExists(String flowId) {
        if (!flowEngine.getFlows().containsKey(flowId)) {
            throw new FlowManagerException(FlowManagerException.ErrorType.NOT_FOUND,
                    "플로우가 없습니다: " + flowId);
        }
    }
}
