package com.fbp.engine.influx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class InfluxDbWriter implements AutoCloseable {
    private final InfluxDbConfig config;
    private final InfluxHttpClient client;
    private final LinkedBlockingDeque<String> buffer;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicLong totalWritten = new AtomicLong();
    private final AtomicLong totalDropped = new AtomicLong();
    private final AtomicLong lastFlushMillis = new AtomicLong();
    private volatile String lastError;
    private ScheduledExecutorService scheduler;

    public InfluxDbWriter(InfluxDbConfig config) {
        this(config, new JdkInfluxHttpClient(config));
    }

    public InfluxDbWriter(InfluxDbConfig config, InfluxHttpClient client) {
        if (config == null) {
            throw new IllegalArgumentException("InfluxDbConfig는 null일 수 없습니다");
        }
        if (client == null) {
            throw new IllegalArgumentException("InfluxHttpClient는 null일 수 없습니다");
        }
        this.config = config;
        this.client = client;
        this.buffer = new LinkedBlockingDeque<>(config.getMaxBufferSize());
    }

    public void start() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "influxdb-writer");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::flushSafely,
                config.getFlushIntervalMillis(),
                config.getFlushIntervalMillis(),
                TimeUnit.MILLISECONDS);
    }

    public void write(InfluxPoint point) {
        if (point == null) {
            throw new IllegalArgumentException("InfluxPoint는 null일 수 없습니다");
        }
        writeLine(point.toLineProtocol());
    }

    public void writeAll(Collection<InfluxPoint> points) {
        if (points == null) {
            return;
        }
        for (InfluxPoint point : points) {
            write(point);
        }
    }

    public void writeLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("InfluxDB line protocol은 비어 있을 수 없습니다");
        }
        boolean added = buffer.offerLast(line);
        if (!added) {
            buffer.pollFirst();
            totalDropped.incrementAndGet();
            added = buffer.offerLast(line);
        }
        if (!added) {
            totalDropped.incrementAndGet();
        }
    }

    public boolean flush() {
        List<String> batch = drainBatch();
        if (batch.isEmpty()) {
            return true;
        }
        String body = String.join("\n", batch);
        long backoffMillis = config.getInitialBackoffMillis();
        for (int attempt = 1; attempt <= config.getMaxAttempts(); attempt++) {
            try {
                client.send(body);
                connected.set(true);
                lastError = null;
                lastFlushMillis.set(System.currentTimeMillis());
                totalWritten.addAndGet(batch.size());
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                connected.set(false);
                lastError = e.getMessage();
                requeueFirst(batch);
                return false;
            } catch (Exception e) {
                connected.set(false);
                lastError = e.getMessage();
                if (attempt < config.getMaxAttempts()) {
                    sleep(backoffMillis);
                    backoffMillis = backoffMillis * 2;
                }
            }
        }
        requeueFirst(batch);
        return false;
    }

    public InfluxDbWriterStatus status() {
        return new InfluxDbWriterStatus(
                connected.get(),
                buffer.size(),
                config.getMaxBufferSize(),
                totalWritten.get(),
                totalDropped.get(),
                lastFlushMillis.get(),
                lastError
        );
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        flushSafely();
    }

    @Override
    public void close() {
        stop();
    }

    private List<String> drainBatch() {
        List<String> batch = new ArrayList<>();
        buffer.drainTo(batch, config.getBatchSize());
        return batch;
    }

    private void requeueFirst(List<String> batch) {
        List<String> copy = new ArrayList<>(batch);
        Collections.reverse(copy);
        for (String line : copy) {
            while (!buffer.offerFirst(line)) {
                String dropped = buffer.pollLast();
                if (dropped == null) {
                    totalDropped.incrementAndGet();
                    break;
                }
                totalDropped.incrementAndGet();
            }
        }
    }

    private void flushSafely() {
        try {
            flush();
        } catch (RuntimeException e) {
            connected.set(false);
            lastError = e.getMessage();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
