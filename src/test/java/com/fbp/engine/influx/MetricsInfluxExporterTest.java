package com.fbp.engine.influx;

import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.metrics.DomainMetricDefinition;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.node.CollectorNode;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.parser.ConnectionDefinition;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.NodeDefinition;
import com.fbp.engine.parser.NodeEndpoint;
import com.fbp.engine.registry.NodeRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MetricsInfluxExporter - 수집된 메트릭을 InfluxDB measurement로 변환")
class MetricsInfluxExporterTest {
    @Test
    @DisplayName("엔진/플로우/노드/와이어/도메인 메트릭을 line protocol measurement로 export한다")
    void exportsEngineFlowNodeWireAndDomainMetrics() {
        FlowManager manager = new FlowManager(new FlowEngine(), registry());
        manager.deploy(definition(), false);

        MetricsCollector collector = manager.getMetricsCollector();
        collector.recordNodeInput("sensor", "out", 12);
        collector.recordNodeOutput("sensor", "out", 12);
        collector.recordSuccess("rule", 2_000_000);
        collector.recordWireDelivered("sensor:out->rule:in", 12, 0);
        collector.registerDomainMetric(new DomainMetricDefinition(
                "temperature",
                "flow-1",
                "sensor",
                "out",
                "value",
                Map.of("location", "room-1"),
                List.of("1m", "1h", "1d")
        ));
        collector.recordDomainMessage("flow-1", "sensor", "out",
                new Message(Map.of("value", 31.2)));

        FakeInfluxHttpClient client = new FakeInfluxHttpClient();
        InfluxDbWriter writer = new InfluxDbWriter(testConfig(), client);
        MetricsInfluxExporter exporter = new MetricsInfluxExporter(manager, writer, "test-host", 10_000L);

        exporter.exportOnce();
        writer.flush();

        String body = client.bodies.get(0);
        assertTrue(body.contains("engine_stats,host=test-host"));
        assertTrue(body.contains("flow_stats,flow_id=flow-1,transport=local"));
        assertTrue(body.contains("node_stats,flow_id=flow-1,node_id=sensor,node_type=Generator"));
        assertTrue(body.contains("wire_stats,flow_id=flow-1,wire_id=sensor:out->rule:in,transport=local"));
        assertTrue(body.contains("sensor_raw,flow_id=flow-1,node_id=sensor,sensor_name=temperature,location=room-1"));
        assertTrue(body.contains("sensor_stats_1m,sensor_name=temperature,location=room-1"));
    }

    private NodeRegistry registry() {
        NodeRegistry registry = new NodeRegistry();
        registry.register("Generator", (id, config) -> new GeneratorNode(id));
        registry.register("Collector", (id, config) -> new CollectorNode(id));
        return registry;
    }

    private FlowDefinition definition() {
        return new FlowDefinition(
                "flow-1",
                "test flow",
                null,
                List.of(
                        new NodeDefinition("sensor", "Generator", Map.of()),
                        new NodeDefinition("rule", "Collector", Map.of())
                ),
                List.of(new ConnectionDefinition(
                        new NodeEndpoint("sensor", "out"),
                        new NodeEndpoint("rule", "in")
                ))
        );
    }

    private InfluxDbConfig testConfig() {
        return new InfluxDbConfig(
                "http://localhost:8086",
                "token",
                "fbp",
                "fbp-metrics",
                100,
                1000,
                1,
                1,
                "memory",
                1000
        );
    }

    private static class FakeInfluxHttpClient implements InfluxHttpClient {
        private final List<String> bodies = new ArrayList<>();

        @Override
        public void send(String body) {
            bodies.add(body);
        }
    }
}
