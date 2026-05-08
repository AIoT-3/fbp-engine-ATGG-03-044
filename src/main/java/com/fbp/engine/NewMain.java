package com.fbp.engine;

import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.cli.Stage3InfluxRuntime;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.JsonFlowParser;
import com.fbp.engine.plugin.BuiltInNodeProvider;
import com.fbp.engine.plugin.NodeDescriptor;
import com.fbp.engine.registry.NodeRegistry;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class NewMain {
    private static final long STATUS_INTERVAL_MILLIS = 10_000L;

    public static void main(String[] args) throws Exception {
        Path flowPath = flowPath(args);
        long runMillis = runMillis(args);

        NodeRegistry nodeRegistry = new NodeRegistry();
        registerBuiltInNodes(nodeRegistry);

        FlowEngine flowEngine = new FlowEngine();
        FlowManager flowManager = new FlowManager(flowEngine, nodeRegistry);
        JsonFlowParser flowParser = new JsonFlowParser();
        Stage3InfluxRuntime influxRuntime = Stage3InfluxRuntime.fromEnvironment(flowManager);

        try (Stage3InfluxRuntime closeableInfluxRuntime = influxRuntime;
             InputStream inputStream = Files.newInputStream(flowPath)) {
            FlowDefinition definition = flowParser.parse(inputStream);
            flowManager.deploy(definition, false);
            flowManager.start(definition.getId());
            registerShutdownHook(flowManager, influxRuntime, definition.getId());

            System.out.println("Stage 3+ debug flow started");
            System.out.println("  Flow file: " + flowPath);
            System.out.println("  Flow id:   " + definition.getId());
            System.out.println("  Nodes:     " + definition.getNodes().size());
            System.out.println("  Wires:     " + definition.getConnections().size());
            System.out.println("  Transport: " + definition.getTransport().getType());
            printInfluxQueryHint(influxRuntime);

            runStatusLoop(flowManager, influxRuntime, definition.getId(), runMillis);
        }
    }

    private static Path flowPath(String[] args) {
        if (args.length == 0 || args[0].trim().isEmpty()) {
            return Path.of("config/stage3-plus-iot-sensor-flow.json");
        }
        return Path.of(args[0].trim());
    }

    private static long runMillis(String[] args) {
        if (args.length < 2 || args[1].trim().isEmpty()) {
            return 0L;
        }
        return Long.parseLong(args[1].trim());
    }

    private static void registerBuiltInNodes(NodeRegistry nodeRegistry) {
        BuiltInNodeProvider builtInNodeProvider = new BuiltInNodeProvider();
        for (NodeDescriptor descriptor : builtInNodeProvider.getNodeDescriptors()) {
            nodeRegistry.register(descriptor.getTypeName(), descriptor.getFactory());
        }
    }

    private static void registerShutdownHook(FlowManager flowManager, Stage3InfluxRuntime influxRuntime,
                                             String flowId) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                flowManager.stop(flowId);
            } catch (RuntimeException ignored) {
                // JVM 종료 시점에는 이미 정지되었거나 제거되었을 수 있다.
            }
            influxRuntime.close();
        }, "stage3-plus-debug-shutdown"));
    }

    private static void runStatusLoop(FlowManager flowManager, Stage3InfluxRuntime influxRuntime,
                                      String flowId, long runMillis) throws InterruptedException {
        long startMillis = System.currentTimeMillis();
        while (true) {
            Thread.sleep(STATUS_INTERVAL_MILLIS);
            influxRuntime.exportOnceAndFlush();
            printRuntimeStatus(flowManager, influxRuntime, flowId);

            if (runMillis > 0 && System.currentTimeMillis() - startMillis >= runMillis) {
                flowManager.stop(flowId);
                System.out.println("Stage 3+ debug flow stopped: " + flowId);
                return;
            }
        }
    }

    private static void printRuntimeStatus(FlowManager flowManager, Stage3InfluxRuntime influxRuntime,
                                           String flowId) {
        System.out.println();
        System.out.println("[status]");
        System.out.println("  Flow:   " + flowId);
        System.out.println("  State:  " + flowManager.getStatus(flowId));
        System.out.println("  Flows:  " + flowManager.getFlowCount());
        System.out.print(influxRuntime.statusText());
    }

    private static void printInfluxQueryHint(Stage3InfluxRuntime influxRuntime) {
        System.out.println("  Influx:   " + (influxRuntime.isEnabled() ? "enabled" : "disabled"));
        if (influxRuntime.isEnabled()) {
            System.out.println("  InfluxDB UI query hint:");
            System.out.println("    from(bucket: \"" + influxRuntime.getConfig().getBucket() + "\")");
            System.out.println("      |> range(start: -1h)");
            System.out.println("      |> filter(fn: (r) => r._measurement == \"sensor_raw\")");
            System.out.println("      |> filter(fn: (r) => r._field == \"value\")");
        }
        System.out.println("  Stop:     Ctrl+C");
        System.out.println();
    }
}
