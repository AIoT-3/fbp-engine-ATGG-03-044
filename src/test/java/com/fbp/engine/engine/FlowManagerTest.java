package com.fbp.engine.engine;

import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.core.State;
import com.fbp.engine.node.CollectorNode;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.parser.ConnectionDefinition;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.NodeDefinition;
import com.fbp.engine.parser.NodeEndpoint;
import com.fbp.engine.registry.NodeRegistry;
import com.fbp.engine.registry.NodeRegistryException;
import com.fbp.engine.metrics.DomainMetricDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FlowManager - 설정 기반 플로우 배포와 생명주기 관리")
class FlowManagerTest {
    @Test
    @DisplayName("deploy 후 RUNNING이 되고 stop/restart/remove 생명주기를 수행한다")
    void deployStopRestartRemove() {
        FlowEngine engine = new FlowEngine();
        FlowManager manager = new FlowManager(engine, registry());

        manager.deploy(definition("flow-1"));

        assertEquals(State.RUNNING, manager.getStatus("flow-1"));
        assertEquals(1, manager.list().size());

        manager.stop("flow-1");
        assertEquals(State.STOPPED, manager.getStatus("flow-1"));

        manager.restart("flow-1");
        assertEquals(State.RUNNING, manager.getStatus("flow-1"));

        manager.remove("flow-1");
        assertEquals(0, manager.getFlowCount());
    }

    @Test
    @DisplayName("이미 같은 id의 플로우가 배포되어 있으면 중복 배포를 거부한다")
    void duplicateDeployFails() {
        FlowManager manager = new FlowManager(new FlowEngine(), registry());
        manager.deploy(definition("flow-1"));

        FlowManagerException exception = assertThrows(FlowManagerException.class,
                () -> manager.deploy(definition("flow-1")));
        assertEquals(FlowManagerException.ErrorType.CONFLICT, exception.getErrorType());

        manager.remove("flow-1");
    }

    @Test
    @DisplayName("없는 플로우를 조작하면 NOT_FOUND 오류로 응답한다")
    void missingFlowOperationIsNotFound() {
        FlowManager manager = new FlowManager(new FlowEngine(), registry());

        FlowManagerException exception = assertThrows(FlowManagerException.class,
                () -> manager.stop("missing"));

        assertEquals(FlowManagerException.ErrorType.NOT_FOUND, exception.getErrorType());
    }

    @Test
    @DisplayName("RUNNING 플로우를 remove하면 자동 정지 후 엔진에서 제거한다")
    void removeRunningFlowStopsAndRemovesIt() {
        FlowEngine engine = new FlowEngine();
        FlowManager manager = new FlowManager(engine, registry());
        manager.deploy(definition("flow-1"));
        assertEquals(State.RUNNING, manager.getStatus("flow-1"));

        manager.remove("flow-1");

        assertEquals(0, manager.getFlowCount());
        assertFalse(engine.getFlows().containsKey("flow-1"));
    }

    @Test
    @DisplayName("등록되지 않은 노드 타입이 있으면 배포에 실패하고 플로우 수는 증가하지 않는다")
    void deployFailsWhenNodeTypeIsNotRegistered() {
        FlowManager manager = new FlowManager(new FlowEngine(), registry());
        FlowDefinition definition = new FlowDefinition(
                "flow-1",
                "test",
                null,
                List.of(
                        new NodeDefinition("source", "MissingType", Map.of()),
                        new NodeDefinition("sink", "Collector", Map.of())
                ),
                List.of(new ConnectionDefinition(
                        new NodeEndpoint("source", "out"),
                        new NodeEndpoint("sink", "in")
                ))
        );

        assertThrows(NodeRegistryException.class, () -> manager.deploy(definition));
        assertEquals(0, manager.getFlowCount());
    }

    @Test
    @DisplayName("FlowDefinition의 domain metrics 설정은 배포 시 MetricsCollector에 등록된다")
    void deployRegistersDomainMetrics() {
        FlowManager manager = new FlowManager(new FlowEngine(), registry());
        FlowDefinition definition = new FlowDefinition(
                "flow-1",
                "test",
                null,
                null,
                List.of(
                        new NodeDefinition("source", "Generator", Map.of()),
                        new NodeDefinition("sink", "Collector", Map.of())
                ),
                List.of(new ConnectionDefinition(
                        new NodeEndpoint("source", "out"),
                        new NodeEndpoint("sink", "in")
                )),
                List.of(new DomainMetricDefinition(
                        "temperature",
                        "flow-1",
                        "source",
                        "out",
                        "temperature",
                        Map.of("unit", "celsius"),
                        List.of("1m")
                ))
        );

        manager.deploy(definition, false);

        assertEquals(1, manager.getMetricsCollector().getDomainMetrics().size());
        assertEquals("temperature", manager.getMetricsCollector().getDomainMetrics().get(0).getName());
    }

    private NodeRegistry registry() {
        NodeRegistry registry = new NodeRegistry();
        registry.register("Generator", (id, config) -> new GeneratorNode(id));
        registry.register("Collector", (id, config) -> new CollectorNode(id));
        return registry;
    }

    private FlowDefinition definition(String id) {
        return new FlowDefinition(
                id,
                "test",
                null,
                List.of(
                        new NodeDefinition("source", "Generator", Map.of()),
                        new NodeDefinition("sink", "Collector", Map.of())
                ),
                List.of(new ConnectionDefinition(
                        new NodeEndpoint("source", "out"),
                        new NodeEndpoint("sink", "in")
                ))
        );
    }
}
