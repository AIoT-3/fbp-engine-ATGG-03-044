package com.fbp.engine.influx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("InfluxDbConfig - InfluxDB 접속/배치/버퍼 설정")
class InfluxDbConfigTest {
    @Test
    @DisplayName("localDefaults는 로컬 InfluxDB 기본 URL, org, bucket, 배치 설정을 만든다")
    void createsDefaultLocalConfig() {
        InfluxDbConfig config = InfluxDbConfig.localDefaults("token-value");

        assertEquals("http://localhost:8086", config.getUrl());
        assertEquals("token-value", config.getToken());
        assertEquals("fbp", config.getOrg());
        assertEquals("fbp-metrics", config.getBucket());
        assertEquals(1000, config.getBatchSize());
        assertEquals(1000, config.getFlushIntervalMillis());
        assertEquals(5, config.getMaxAttempts());
        assertEquals(200, config.getInitialBackoffMillis());
        assertEquals("memory", config.getBufferType());
        assertEquals(100000, config.getMaxBufferSize());
    }

    @Test
    @DisplayName("URL, token, batchSize 같은 필수 설정이 잘못되면 생성자를 거부한다")
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new InfluxDbConfig(
                "",
                "",
                "fbp",
                "fbp-metrics",
                1000,
                1000,
                5,
                200,
                "memory",
                100000
        ));

        assertThrows(IllegalArgumentException.class, () -> new InfluxDbConfig(
                "http://localhost:8086",
                "",
                "fbp",
                "fbp-metrics",
                0,
                1000,
                5,
                200,
                "memory",
                100000
        ));
    }
}
