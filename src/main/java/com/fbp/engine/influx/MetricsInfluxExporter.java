package com.fbp.engine.influx;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.State;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.engine.FlowManagerException;
import com.fbp.engine.engine.FlowSummary;
import com.fbp.engine.metrics.DomainMetricWindowSnapshot;
import com.fbp.engine.metrics.DomainRawMetricSnapshot;
import com.fbp.engine.metrics.FlowMetrics;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.NodeMetricsSnapshot;
import com.fbp.engine.metrics.WireMetricsSnapshot;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.NodeDefinition;
import com.fbp.engine.transport.MqttBridgeConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsInfluxExporter implements AutoCloseable {
    private final FlowManager flowManager;
    private final InfluxDbWriter writer;
    private final String host;
    private final long exportIntervalMillis;
    private final Map<String, Long> previousFlowProcessed = new LinkedHashMap<>();
    private long previousExportMillis;
    private ScheduledExecutorService scheduler;

    public MetricsInfluxExporter(FlowManager flowManager, InfluxDbWriter writer) {
        this(flowManager, writer, defaultHost(), 10_000L);
    }

    public MetricsInfluxExporter(FlowManager flowManager, InfluxDbWriter writer,
                                 String host, long exportIntervalMillis) {
        if (flowManager == null) {
            throw new IllegalArgumentException("FlowManager는 null일 수 없습니다");
        }
        if (writer == null) {
            throw new IllegalArgumentException("InfluxDbWriter는 null일 수 없습니다");
        }
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("host는 비어 있을 수 없습니다");
        }
        if (exportIntervalMillis <= 0) {
            throw new IllegalArgumentException("exportIntervalMillis는 1 이상이어야 합니다");
        }
        this.flowManager = flowManager;
        this.writer = writer;
        this.host = host.trim();
        this.exportIntervalMillis = exportIntervalMillis;
    }

    public void start() {
        if (scheduler != null) {
            return;
        }
        writer.start();
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "metrics-influx-exporter");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::exportSafely,
                0,
                exportIntervalMillis,
                TimeUnit.MILLISECONDS);
    }

    public void exportOnce() {
        long nowMillis = System.currentTimeMillis();
        long nowNanos = millisToNanos(nowMillis);
        MetricsCollector collector = flowManager.getMetricsCollector();
        List<FlowSummary> flows = flowManager.list();
        List<InfluxPoint> points = new ArrayList<>();

        long activeFlows = 0;
        long totalNodes = 0;
        long totalErrors = 0;
        double engineThroughput = 0.0;

        for (FlowSummary summary : flows) {
            if (summary.getStatus() == State.RUNNING) {
                activeFlows++;
            }
            totalNodes += summary.getNodeCount();

            FlowMetrics flowMetrics = collector.getFlowMetrics(
                    summary.getId(),
                    flowManager.getNodeIds(summary.getId()),
                    flowManager.getConnectionIds(summary.getId())
            );
            totalErrors += flowMetrics.getTotalErrors();
            double flowThroughput = throughput(summary.getId(), flowMetrics.getTotalProcessed(), nowMillis);
            engineThroughput += flowThroughput;

            points.add(flowStatsPoint(summary, flowMetrics, flowThroughput, nowNanos));
            points.addAll(nodeStatsPoints(summary.getId(), flowMetrics, nowNanos));
            points.addAll(wireStatsPoints(summary.getId(), collector, nowNanos));
        }

        points.add(engineStatsPoint(activeFlows, totalNodes, engineThroughput, totalErrors, nowNanos));
        points.addAll(sensorRawPoints(collector.drainDomainRawMetrics()));
        points.addAll(sensorWindowPoints(collector.getDomainWindowMetrics()));

        writer.writeAll(points);
        previousExportMillis = nowMillis;
    }

    public void writeFlowEvent(String flowId, String eventType, String user,
                               long revision, String changeSummary) {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("flow_id", flowId);
        tags.put("event_type", eventType);
        tags.put("user", user);

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("revision", revision);
        fields.put("change_summary", changeSummary == null ? "" : changeSummary);

        InfluxPoint point = new InfluxPoint("flow_events", tags, fields, millisToNanos(System.currentTimeMillis()));
        writer.write(point);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        writer.stop();
    }

    @Override
    public void close() {
        stop();
    }

    private InfluxPoint engineStatsPoint(long activeFlows, long totalNodes,
                                         double throughput, long errors, long timestampNanos) {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("host", host);

        Runtime runtime = Runtime.getRuntime();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("active_flows", activeFlows);
        fields.put("total_nodes", totalNodes);
        fields.put("throughput", throughput);
        fields.put("errors", errors);
        fields.put("heap_used", heapUsed);

        return new InfluxPoint("engine_stats", tags, fields, timestampNanos);
    }

    private InfluxPoint flowStatsPoint(FlowSummary summary, FlowMetrics metrics,
                                       double throughput, long timestampNanos) {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("flow_id", summary.getId());
        tags.put("transport", summary.getTransportType());

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("processed", metrics.getTotalProcessed());
        fields.put("errors", metrics.getTotalErrors());
        fields.put("throughput", throughput);
        fields.put("avg_latency_ms", averageNodeLatency(metrics));

        return new InfluxPoint("flow_stats", tags, fields, timestampNanos);
    }

    private List<InfluxPoint> nodeStatsPoints(String flowId, FlowMetrics metrics, long timestampNanos) {
        List<InfluxPoint> points = new ArrayList<>();
        Map<String, String> nodeTypes = nodeTypes(flowId);
        for (NodeMetricsSnapshot node : metrics.getNodes()) {
            Map<String, String> tags = new LinkedHashMap<>();
            tags.put("flow_id", flowId);
            tags.put("node_id", node.getNodeId());
            tags.put("node_type", nodeTypes.getOrDefault(node.getNodeId(), "unknown"));

            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("in_count", node.getInputCount());
            fields.put("out_count", node.getOutputCount());
            fields.put("in_bytes", node.getInputBytes());
            fields.put("out_bytes", node.getOutputBytes());
            fields.put("avg_time_ms", node.getAverageTimeMillis());
            fields.put("p99_time_ms", node.getP99TimeMillis());
            fields.put("errors", node.getErrors());
            fields.put("queue_size", (long) node.getQueueSize());

            points.add(new InfluxPoint("node_stats", tags, fields, timestampNanos));
        }
        return points;
    }

    private List<InfluxPoint> wireStatsPoints(String flowId, MetricsCollector collector, long timestampNanos) {
        List<InfluxPoint> points = new ArrayList<>();
        for (Connection connection : flowManager.getConnections(flowId)) {
            WireMetricsSnapshot wire = collector.getWireMetrics(connection.getId());

            Map<String, String> tags = new LinkedHashMap<>();
            tags.put("flow_id", flowId);
            tags.put("wire_id", wire.getWireId());
            tags.put("transport", transportType(connection));
            tags.put("topic", topic(connection));

            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("delivered", wire.getDelivered());
            fields.put("queue_size", (long) wire.getQueueSize());
            fields.put("dropped", wire.getDropped());
            fields.put("mqtt_rtt_ms", wire.getMqttRttMillis());
            fields.put("bytes", wire.getBytes());

            points.add(new InfluxPoint("wire_stats", tags, fields, timestampNanos));
        }
        return points;
    }

    private List<InfluxPoint> sensorRawPoints(List<DomainRawMetricSnapshot> snapshots) {
        List<InfluxPoint> points = new ArrayList<>();
        for (DomainRawMetricSnapshot snapshot : snapshots) {
            Map<String, String> tags = new LinkedHashMap<>();
            tags.put("flow_id", snapshot.getFlowId());
            tags.put("node_id", snapshot.getNodeId());
            tags.put("sensor_name", snapshot.getName());
            tags.putAll(snapshot.getTags());

            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("value", snapshot.getValue());

            points.add(new InfluxPoint("sensor_raw", tags, fields, millisToNanos(snapshot.getTimestampMillis())));
        }
        return points;
    }

    private List<InfluxPoint> sensorWindowPoints(List<DomainMetricWindowSnapshot> snapshots) {
        List<InfluxPoint> points = new ArrayList<>();
        for (DomainMetricWindowSnapshot snapshot : snapshots) {
            Map<String, String> tags = new LinkedHashMap<>();
            tags.put("sensor_name", snapshot.getName());
            tags.putAll(snapshot.getTags());

            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("avg", snapshot.getAverage());
            fields.put("min", snapshot.getMin());
            fields.put("max", snapshot.getMax());
            fields.put("count", snapshot.getCount());

            String measurement = "sensor_stats_" + snapshot.getWindow();
            points.add(new InfluxPoint(measurement, tags, fields, millisToNanos(snapshot.getBucketStartMillis())));
        }
        return points;
    }

    private Map<String, String> nodeTypes(String flowId) {
        Map<String, String> nodeTypes = new LinkedHashMap<>();
        try {
            FlowDefinition definition = flowManager.getDefinition(flowId);
            for (NodeDefinition node : definition.getNodes()) {
                nodeTypes.put(node.getId(), node.getType());
            }
        } catch (FlowManagerException e) {
            return nodeTypes;
        }
        return nodeTypes;
    }

    private double throughput(String flowId, long processed, long nowMillis) {
        Long previousProcessed = previousFlowProcessed.put(flowId, processed);
        if (previousProcessed == null || previousExportMillis == 0L) {
            return 0.0;
        }
        long elapsedMillis = Math.max(1L, nowMillis - previousExportMillis);
        long delta = Math.max(0L, processed - previousProcessed);
        return delta / (elapsedMillis / 1000.0);
    }

    private double averageNodeLatency(FlowMetrics metrics) {
        double total = 0.0;
        long count = 0;
        for (NodeMetricsSnapshot node : metrics.getNodes()) {
            long processed = node.getProcessed();
            total += node.getAverageTimeMillis() * processed;
            count += processed;
        }
        if (count == 0) {
            return 0.0;
        }
        return total / count;
    }

    private String transportType(Connection connection) {
        if (connection instanceof MqttBridgeConnection) {
            return "mqtt";
        }
        return "local";
    }

    private String topic(Connection connection) {
        if (connection instanceof MqttBridgeConnection bridgeConnection) {
            return bridgeConnection.getTopic();
        }
        return connection.getId();
    }

    private void exportSafely() {
        try {
            exportOnce();
        } catch (RuntimeException e) {
            writeFlowEvent("engine", "export_error", "system", 0, e.getMessage());
        }
    }

    private static long millisToNanos(long millis) {
        return millis * 1_000_000L;
    }

    private static String defaultHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }
}
