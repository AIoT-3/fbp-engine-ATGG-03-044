package com.fbp.engine.cli;

import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.influx.InfluxDbConfig;
import com.fbp.engine.influx.InfluxDbWriter;
import com.fbp.engine.influx.InfluxDbWriterStatus;
import com.fbp.engine.influx.MetricsInfluxExporter;

public class Stage3InfluxRuntime implements AutoCloseable {
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final boolean enabled;
    private final String disabledReason;
    private final InfluxDbConfig config;
    private final InfluxDbWriter writer;
    private final MetricsInfluxExporter exporter;
    private boolean closed;

    private Stage3InfluxRuntime(boolean enabled, String disabledReason,
                                InfluxDbConfig config, InfluxDbWriter writer,
                                MetricsInfluxExporter exporter) {
        this.enabled = enabled;
        this.disabledReason = disabledReason;
        this.config = config;
        this.writer = writer;
        this.exporter = exporter;
    }

    public static Stage3InfluxRuntime fromEnvironment(FlowManager flowManager) {
        String token = System.getenv("INFLUX_TOKEN");
        if (token == null || token.trim().isEmpty()) {
            return disabled("INFLUX_TOKEN 환경변수가 없습니다");
        }

        try {
            InfluxDbConfig config = InfluxDbConfig.fromEnvironment();
            InfluxDbWriter writer = new InfluxDbWriter(config);
            MetricsInfluxExporter exporter = new MetricsInfluxExporter(flowManager, writer);
            exporter.start();
            return new Stage3InfluxRuntime(true, null, config, writer, exporter);
        } catch (RuntimeException e) {
            return disabled(e.getMessage());
        }
    }

    public static Stage3InfluxRuntime disabled(String reason) {
        return new Stage3InfluxRuntime(false, reason, null, null, null);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public InfluxDbConfig getConfig() {
        return config;
    }

    public void exportOnceAndFlush() {
        if (!enabled) {
            return;
        }
        exporter.exportOnce();
        writer.flush();
    }

    public String statusText() {
        StringBuilder builder = new StringBuilder();
        builder.append("InfluxDB:").append(LINE_SEPARATOR);
        if (!enabled) {
            builder.append("  Enabled:   false").append(LINE_SEPARATOR);
            builder.append("  Reason:    ").append(disabledReason).append(LINE_SEPARATOR);
            return builder.toString();
        }

        InfluxDbWriterStatus status = writer.status();
        builder.append("  Enabled:   true").append(LINE_SEPARATOR);
        builder.append("  URL:       ").append(config.getUrl()).append(LINE_SEPARATOR);
        builder.append("  Org:       ").append(config.getOrg()).append(LINE_SEPARATOR);
        builder.append("  Bucket:    ").append(config.getBucket()).append(LINE_SEPARATOR);
        builder.append("  Connected: ").append(status.isConnected()).append(LINE_SEPARATOR);
        builder.append("  Written:   ").append(status.getTotalWritten()).append(LINE_SEPARATOR);
        builder.append("  Queue:     ")
                .append(status.getQueueSize())
                .append(" / ")
                .append(status.getMaxQueueSize())
                .append(LINE_SEPARATOR);
        builder.append("  Dropped:   ").append(status.getTotalDropped()).append(LINE_SEPARATOR);
        builder.append("  LastError: ").append(lastErrorText(status)).append(LINE_SEPARATOR);
        return builder.toString();
    }

    private String lastErrorText(InfluxDbWriterStatus status) {
        if (status.getLastError() == null || status.getLastError().trim().isEmpty()) {
            return "-";
        }
        return status.getLastError();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (exporter != null) {
            exporter.close();
        }
    }
}
