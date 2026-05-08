package com.fbp.engine.demo.runner;

import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.engine.FlowSummary;
import com.fbp.engine.influx.InfluxDbConfig;
import com.fbp.engine.influx.InfluxDbWriter;
import com.fbp.engine.influx.InfluxDbWriterStatus;
import com.fbp.engine.influx.MetricsInfluxExporter;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.JsonFlowParser;
import com.fbp.engine.plugin.BuiltInNodeProvider;
import com.fbp.engine.plugin.NodeDescriptor;
import com.fbp.engine.registry.NodeRegistry;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class InfluxMetricsRunner {
    public static void main(String[] args) throws Exception {
        String token = System.getenv("INFLUX_TOKEN");
        if (token == null || token.trim().isEmpty()) {
            System.out.println("INFLUX_TOKEN 환경변수가 필요합니다.");
            return;
        }

        Path flowPath = Path.of("config/stage3-plus-local-flow.json");
        if (args.length > 0) {
            flowPath = Path.of(args[0]);
        }
        long runMillis = 15_000L;
        if (args.length > 1) {
            runMillis = Long.parseLong(args[1]);
        }

        NodeRegistry nodeRegistry = new NodeRegistry();
        BuiltInNodeProvider builtInNodeProvider = new BuiltInNodeProvider();
        for (NodeDescriptor descriptor : builtInNodeProvider.getNodeDescriptors()) {
            nodeRegistry.register(descriptor.getTypeName(), descriptor.getFactory());
        }

        FlowEngine flowEngine = new FlowEngine();
        FlowManager flowManager = new FlowManager(flowEngine, nodeRegistry);
        JsonFlowParser parser = new JsonFlowParser();

        InfluxDbConfig config = InfluxDbConfig.fromEnvironment();
        InfluxDbWriter writer = new InfluxDbWriter(config);
        MetricsInfluxExporter exporter = new MetricsInfluxExporter(flowManager, writer);

        try (InputStream inputStream = Files.newInputStream(flowPath);
             MetricsInfluxExporter ignored = exporter) {
            FlowDefinition definition = parser.parse(inputStream);
            flowManager.deploy(definition, false);
            flowManager.start(definition.getId());
            System.out.println("Flow started: " + definition.getId());

            Thread.sleep(runMillis);

            exporter.exportOnce();
            writer.flush();
            flowManager.stop(definition.getId());

            InfluxDbWriterStatus status = writer.status();
            System.out.println("InfluxDB write status");
            System.out.println("  connected: " + status.isConnected());
            System.out.println("  written:   " + status.getTotalWritten());
            System.out.println("  queued:    " + status.getQueueSize() + " / " + status.getMaxQueueSize());
            System.out.println("  dropped:   " + status.getTotalDropped());
            if (status.getLastError() != null) {
                System.out.println("  lastError: " + status.getLastError());
            }

            for (FlowSummary summary : flowManager.list()) {
                System.out.println("Flow summary: "
                        + summary.getId()
                        + " status="
                        + summary.getStatus()
                        + " transport="
                        + summary.getTransportType());
            }
        }
    }
}
