package com.fbp.engine.influx;

import java.time.Duration;

public class InfluxDbConfig {
    private final String url;
    private final String token;
    private final String org;
    private final String bucket;
    private final int batchSize;
    private final long flushIntervalMillis;
    private final int maxAttempts;
    private final long initialBackoffMillis;
    private final String bufferType;
    private final int maxBufferSize;

    public InfluxDbConfig(String url, String token, String org, String bucket,
                          int batchSize, long flushIntervalMillis,
                          int maxAttempts, long initialBackoffMillis,
                          String bufferType, int maxBufferSize) {
        this.url = requireText(url, "InfluxDB url은 필수입니다");
        this.token = token;
        this.org = requireText(org, "InfluxDB org는 필수입니다");
        this.bucket = requireText(bucket, "InfluxDB bucket은 필수입니다");
        this.batchSize = requirePositive(batchSize, "batchSize는 1 이상이어야 합니다");
        this.flushIntervalMillis = requirePositive(flushIntervalMillis, "flushIntervalMillis는 1 이상이어야 합니다");
        this.maxAttempts = requirePositive(maxAttempts, "maxAttempts는 1 이상이어야 합니다");
        this.initialBackoffMillis = requirePositive(initialBackoffMillis, "initialBackoffMillis는 1 이상이어야 합니다");
        this.bufferType = normalizeBufferType(bufferType);
        this.maxBufferSize = requirePositive(maxBufferSize, "maxBufferSize는 1 이상이어야 합니다");
    }

    public static InfluxDbConfig localDefaults(String token) {
        return new InfluxDbConfig(
                "http://localhost:8086",
                token,
                "fbp",
                "fbp-metrics",
                1000,
                Duration.ofSeconds(1).toMillis(),
                5,
                200,
                "memory",
                100000
        );
    }

    public static InfluxDbConfig fromEnvironment() {
        String url = environmentOrDefault("INFLUX_URL", "http://localhost:8086");
        String token = environmentOrDefault("INFLUX_TOKEN", "");
        String org = environmentOrDefault("INFLUX_ORG", "iot-lab");
        String bucket = environmentOrDefault("INFLUX_BUCKET", "fbp-metrics");
        return localDefaults(token).withConnection(url, org, bucket);
    }

    public InfluxDbConfig withConnection(String newUrl, String newOrg, String newBucket) {
        return new InfluxDbConfig(
                newUrl,
                token,
                newOrg,
                newBucket,
                batchSize,
                flushIntervalMillis,
                maxAttempts,
                initialBackoffMillis,
                bufferType,
                maxBufferSize
        );
    }

    public String getUrl() {
        return url;
    }

    public String getToken() {
        return token;
    }

    public String getOrg() {
        return org;
    }

    public String getBucket() {
        return bucket;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public long getFlushIntervalMillis() {
        return flushIntervalMillis;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getInitialBackoffMillis() {
        return initialBackoffMillis;
    }

    public String getBufferType() {
        return bufferType;
    }

    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private long requirePositive(long value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String normalizeBufferType(String value) {
        String normalized;
        if (value == null || value.trim().isEmpty()) {
            normalized = "memory";
        } else {
            normalized = value.trim().toLowerCase();
        }
        if (!"memory".equals(normalized)) {
            throw new IllegalArgumentException("현재 InfluxDB buffer type은 memory만 지원합니다: " + value);
        }
        return normalized;
    }
}
