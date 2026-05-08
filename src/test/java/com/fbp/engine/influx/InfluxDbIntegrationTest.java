package com.fbp.engine.influx;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@DisplayName("InfluxDbIntegration - 실제 InfluxDB write API 연동")
class InfluxDbIntegrationTest {
    @Test
    @DisplayName("INFLUX_TOKEN 환경변수가 있으면 실제 InfluxDB에 engine_stats point를 기록한다")
    void writesPointToConfiguredInfluxDb() {
        String token = System.getenv("INFLUX_TOKEN");
        Assumptions.assumeTrue(token != null && !token.trim().isEmpty(),
                "INFLUX_TOKEN is required for real InfluxDB integration test");

        InfluxDbConfig config = InfluxDbConfig.fromEnvironment();
        InfluxDbWriter writer = new InfluxDbWriter(config);

        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("host", "integration-test");

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("active_flows", 1L);
        fields.put("total_nodes", 1L);
        fields.put("throughput", 1.0);
        fields.put("errors", 0L);
        fields.put("heap_used", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

        writer.write(new InfluxPoint("engine_stats", tags, fields, System.currentTimeMillis() * 1_000_000L));

        assertTrue(writer.flush());
        assertTrue(writer.status().isConnected());
        writer.close();
    }
}
