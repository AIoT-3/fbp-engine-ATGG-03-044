package com.fbp.engine.cli;

import com.fbp.engine.core.State;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.engine.FlowManagerException;
import com.fbp.engine.engine.FlowSummary;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.FlowParserException;
import com.fbp.engine.parser.JsonFlowParser;
import com.fbp.engine.parser.TransportDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class Stage3PlusCommandHandler {
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final FlowManager flowManager;
    private final JsonFlowParser flowParser;
    private final Supplier<String> externalStatusSupplier;

    public Stage3PlusCommandHandler(FlowManager flowManager, JsonFlowParser flowParser) {
        this(flowManager, flowParser, () -> "");
    }

    public Stage3PlusCommandHandler(FlowManager flowManager, JsonFlowParser flowParser,
                                    Supplier<String> externalStatusSupplier) {
        this.flowManager = Objects.requireNonNull(flowManager, "flowManager must not be null");
        this.flowParser = Objects.requireNonNull(flowParser, "flowParser must not be null");
        this.externalStatusSupplier = Objects.requireNonNull(
                externalStatusSupplier,
                "externalStatusSupplier must not be null"
        );
    }

    public String handle(String line) {
        try {
            String command = normalizeCommand(line);
            if (command.isEmpty()) {
                return "";
            }
            if ("help".equals(command)) {
                return help();
            }
            if ("status".equals(command)) {
                return status();
            }
            if ("flow list".equals(command)) {
                return flowList();
            }
            if (command.startsWith("flow deploy ")) {
                String filePath = command.substring("flow deploy ".length()).trim();
                return flowDeploy(filePath);
            }
            if (command.startsWith("flow status ")) {
                String flowId = command.substring("flow status ".length()).trim();
                return flowStatus(flowId);
            }
            if (command.startsWith("flow start ")) {
                String flowId = command.substring("flow start ".length()).trim();
                return flowStart(flowId);
            }
            if (command.startsWith("flow stop ")) {
                String flowId = command.substring("flow stop ".length()).trim();
                return flowStop(flowId);
            }
            if (command.startsWith("flow restart ")) {
                String flowId = command.substring("flow restart ".length()).trim();
                return flowRestart(flowId);
            }
            if (command.startsWith("flow remove ")) {
                String flowId = command.substring("flow remove ".length()).trim();
                return flowRemove(flowId);
            }

            return "알 수 없는 명령어입니다: " + command + LINE_SEPARATOR
                    + "사용 가능한 명령어를 보려면 help를 입력하세요." + LINE_SEPARATOR;
        } catch (FlowManagerException | FlowParserException | IllegalArgumentException e) {
            return "명령 실행 실패: " + e.getMessage() + LINE_SEPARATOR;
        } catch (RuntimeException e) {
            return "명령 실행 실패: " + e.getMessage() + LINE_SEPARATOR;
        }
    }

    public boolean isExitCommand(String line) {
        String command = normalizeCommand(line);
        if ("exit".equals(command)) {
            return true;
        }
        return "quit".equals(command);
    }

    private String normalizeCommand(String line) {
        if (line == null) {
            return "";
        }
        return line.trim();
    }

    private String help() {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Stage 3+ CLI commands");
        appendLine(builder, "  status");
        appendLine(builder, "  flow list");
        appendLine(builder, "  flow deploy <file>");
        appendLine(builder, "  flow status <flow-id>");
        appendLine(builder, "  flow start <flow-id>");
        appendLine(builder, "  flow stop <flow-id>");
        appendLine(builder, "  flow restart <flow-id>");
        appendLine(builder, "  flow remove <flow-id>");
        appendLine(builder, "  exit");
        return builder.toString();
    }

    private String status() {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Engine:");
        appendLine(builder, "  State:     " + flowManager.getEngineState().name());
        appendLine(builder, "  FlowCount: " + flowManager.getFlowCount());
        appendLine(builder, "");
        builder.append(flowList());

        String externalStatus = externalStatusSupplier.get();
        if (externalStatus != null && !externalStatus.trim().isEmpty()) {
            appendLine(builder, "");
            builder.append(externalStatus);
        }
        return builder.toString();
    }

    private String flowList() {
        List<FlowSummary> summaries = flowManager.list();
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "ID                        STATUS   TRANSPORT  NODES  WIRES");
        for (FlowSummary summary : summaries) {
            String line = String.format(
                    "%-25s %-8s %-10s %-5d %d",
                    summary.getId(),
                    summary.getStatus().name(),
                    summary.getTransportType(),
                    summary.getNodeCount(),
                    summary.getWireCount()
            );
            appendLine(builder, line);
        }
        return builder.toString();
    }

    private String flowDeploy(String filePath) {
        if (filePath.isEmpty()) {
            return "배포할 flow JSON 파일 경로가 필요합니다." + LINE_SEPARATOR;
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return "flow JSON 파일이 없습니다: " + filePath + LINE_SEPARATOR;
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            FlowDefinition definition = flowParser.parse(inputStream);
            flowManager.deploy(definition, false);
            return "Flow '" + definition.getId() + "' deployed successfully. Status: STOPPED" + LINE_SEPARATOR;
        } catch (IOException e) {
            return "flow JSON 파일 읽기 실패: " + filePath + " (" + e.getMessage() + ")" + LINE_SEPARATOR;
        }
    }

    private String flowStatus(String flowId) {
        if (flowId.isEmpty()) {
            return "flow id가 필요합니다." + LINE_SEPARATOR;
        }

        FlowDefinition definition = flowManager.getDefinition(flowId);
        State status = flowManager.getStatus(flowId);
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Flow: " + definition.getId());
        appendLine(builder, "  Status:    " + status.name());
        appendLine(builder, "  Transport: " + transportText(definition.getTransport()));
        appendLine(builder, "  Nodes:     " + definition.getNodes().size());
        appendLine(builder, "  Wires:     " + definition.getConnections().size());
        return builder.toString();
    }

    private String flowStart(String flowId) {
        if (flowId.isEmpty()) {
            return "flow id가 필요합니다." + LINE_SEPARATOR;
        }
        flowManager.start(flowId);
        return "Flow '" + flowId + "' started." + LINE_SEPARATOR;
    }

    private String flowStop(String flowId) {
        if (flowId.isEmpty()) {
            return "flow id가 필요합니다." + LINE_SEPARATOR;
        }
        flowManager.stop(flowId);
        return "Flow '" + flowId + "' stopped." + LINE_SEPARATOR;
    }

    private String flowRestart(String flowId) {
        if (flowId.isEmpty()) {
            return "flow id가 필요합니다." + LINE_SEPARATOR;
        }
        flowManager.restart(flowId);
        return "Flow '" + flowId + "' restarted." + LINE_SEPARATOR;
    }

    private String flowRemove(String flowId) {
        if (flowId.isEmpty()) {
            return "flow id가 필요합니다." + LINE_SEPARATOR;
        }
        flowManager.remove(flowId);
        return "Flow '" + flowId + "' removed." + LINE_SEPARATOR;
    }

    private String transportText(TransportDefinition transport) {
        if (transport == null) {
            return "local";
        }
        if (transport.isMqtt()) {
            return "mqtt (" + transport.getBroker() + ", QoS=" + transport.getQos() + ")";
        }
        return transport.getType();
    }

    private void appendLine(StringBuilder builder, String text) {
        builder.append(text);
        builder.append(LINE_SEPARATOR);
    }
}
