package com.fbp.engine.cli;

import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.parser.JsonFlowParser;
import com.fbp.engine.plugin.BuiltInNodeProvider;
import com.fbp.engine.plugin.NodeDescriptor;
import com.fbp.engine.registry.NodeRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Stage3PlusCommandHandler - CLI 명령어를 FlowManager 작업으로 변환")
class Stage3PlusCommandHandlerTest {

    @Test
    @DisplayName("flow list/deploy/start/status/stop/restart/remove 명령이 런타임 FlowManager를 사용한다")
    void runtimeFlowCommandsUseFlowManager() {
        Stage3PlusCommandHandler handler = runtimeHandler();

        String emptyList = handler.handle("flow list");
        assertTrue(emptyList.contains("ID"));
        assertFalse(emptyList.contains("stage3-plus-local-flow"));

        String deployOutput = handler.handle("flow deploy config/stage3-plus-local-flow.json");
        assertTrue(deployOutput.contains("Flow 'stage3-plus-local-flow' deployed successfully"));
        assertTrue(deployOutput.contains("STOPPED"));

        String listOutput = handler.handle("flow list");
        assertTrue(listOutput.contains("stage3-plus-local-flow"));
        assertTrue(listOutput.contains("STOPPED"));
        assertTrue(listOutput.contains("local"));

        String startOutput = handler.handle("flow start stage3-plus-local-flow");
        assertTrue(startOutput.contains("started"));

        String engineStatus = handler.handle("status");
        assertTrue(engineStatus.contains("Engine:"));
        assertTrue(engineStatus.contains("FlowCount: 1"));
        assertTrue(engineStatus.contains("stage3-plus-local-flow"));
        assertTrue(engineStatus.contains("InfluxDB:"));
        assertTrue(engineStatus.contains("Enabled:   false"));

        String runningStatus = handler.handle("flow status stage3-plus-local-flow");
        assertTrue(runningStatus.contains("Status:    RUNNING"));
        assertTrue(runningStatus.contains("Nodes:     6"));
        assertTrue(runningStatus.contains("Wires:     5"));

        String stopOutput = handler.handle("flow stop stage3-plus-local-flow");
        assertTrue(stopOutput.contains("stopped"));

        String stoppedStatus = handler.handle("flow status stage3-plus-local-flow");
        assertTrue(stoppedStatus.contains("Status:    STOPPED"));

        String restartOutput = handler.handle("flow restart stage3-plus-local-flow");
        assertTrue(restartOutput.contains("restarted"));

        String removeOutput = handler.handle("flow remove stage3-plus-local-flow");
        assertTrue(removeOutput.contains("removed"));
    }

    @Test
    @DisplayName("지원하지 않는 명령어는 help 안내와 함께 거부한다")
    void unsupportedCommandsAreRejected() {
        Stage3PlusCommandHandler handler = runtimeHandler();

        String output = handler.handle("wire info w-1");

        assertTrue(output.contains("알 수 없는 명령어입니다"));
        assertTrue(output.contains("help"));
    }

    @Test
    @DisplayName("exit와 quit은 CLI 종료 명령으로 인식한다")
    void exitCommandsAreDetected() {
        Stage3PlusCommandHandler handler = runtimeHandler();

        assertTrue(handler.isExitCommand("exit"));
        assertTrue(handler.isExitCommand("quit"));
        assertFalse(handler.isExitCommand("flow list"));
    }

    private Stage3PlusCommandHandler runtimeHandler() {
        NodeRegistry nodeRegistry = new NodeRegistry();
        BuiltInNodeProvider builtInNodeProvider = new BuiltInNodeProvider();
        for (NodeDescriptor descriptor : builtInNodeProvider.getNodeDescriptors()) {
            nodeRegistry.register(descriptor.getTypeName(), descriptor.getFactory());
        }

        FlowEngine flowEngine = new FlowEngine();
        FlowManager flowManager = new FlowManager(flowEngine, nodeRegistry);
        JsonFlowParser flowParser = new JsonFlowParser();
        Stage3PlusCommandHandler handler = new Stage3PlusCommandHandler(
                flowManager,
                flowParser,
                () -> "InfluxDB:" + System.lineSeparator()
                        + "  Enabled:   false" + System.lineSeparator()
        );
        return handler;
    }
}
