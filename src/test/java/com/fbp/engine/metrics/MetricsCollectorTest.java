package com.fbp.engine.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetricsCollector - 노드/와이어/도메인 메트릭 수집")
class MetricsCollectorTest {
    @Test
    @DisplayName("성공/실패 처리 시간을 기록하면 처리 수, 에러 수, 평균 시간이 갱신된다")
    void recordProcessingCountsSuccessAndError() {
        MetricsCollector collector = new MetricsCollector();

        collector.recordSuccess("node-1", 1_000_000);
        collector.recordError("node-1", 3_000_000);

        NodeMetricsSnapshot snapshot = collector.getNodeMetrics("node-1");
        assertEquals(2, snapshot.getProcessed());
        assertEquals(1, snapshot.getErrors());
        assertEquals(2.0, snapshot.getAverageTimeMillis());
    }

    @Test
    @DisplayName("여러 스레드가 동시에 기록해도 처리 건수가 정확히 누적된다")
    void concurrentRecordingIsSafe() throws InterruptedException {
        MetricsCollector collector = new MetricsCollector();
        int threads = 10;
        int iterations = 100;
        CountDownLatch latch = new CountDownLatch(threads);
        var executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    collector.recordSuccess("node-1", 1);
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        executor.shutdownNow();
        assertEquals(threads * iterations, collector.getNodeMetrics("node-1").getProcessed());
    }

    @Test
    @DisplayName("플로우 메트릭은 여러 노드와 와이어의 합계를 집계한다")
    void flowMetricsAggregatesNodes() {
        MetricsCollector collector = new MetricsCollector();
        collector.recordSuccess("a", 1);
        collector.recordError("b", 1);
        collector.recordWireDelivered("a:out->b:in", 20, 2);

        FlowMetrics metrics = collector.getFlowMetrics("flow-1", List.of("a", "b"), List.of("a:out->b:in"));

        assertEquals(2, metrics.getTotalProcessed());
        assertEquals(1, metrics.getTotalErrors());
        assertEquals(1, metrics.getTotalWireDelivered());
        assertEquals(1, metrics.getWires().size());
    }

    @Test
    @DisplayName("노드 입력/출력 횟수와 바이트 수, 처리 시간 백분위가 기록된다")
    void nodeInputOutputAndBytesAreRecorded() {
        MetricsCollector collector = new MetricsCollector();

        collector.recordNodeInput("node-1", "in", 10);
        collector.recordNodeOutput("node-1", "out", 20);
        collector.recordSuccess("node-1", 5_000_000);

        NodeMetricsSnapshot snapshot = collector.getNodeMetrics("node-1");
        assertEquals(1, snapshot.getInputCount());
        assertEquals(1, snapshot.getOutputCount());
        assertEquals(10, snapshot.getInputBytes());
        assertEquals(20, snapshot.getOutputBytes());
        assertEquals(5.0, snapshot.getP99TimeMillis());
    }

    @Test
    @DisplayName("도메인 메트릭은 숫자 payload 필드를 추출해 평균/최소/최대와 윈도우 통계를 만든다")
    void domainMetricExtractsNumericPayloadField() {
        MetricsCollector collector = new MetricsCollector();
        DomainMetricDefinition definition = new DomainMetricDefinition(
                "temperature",
                "flow-1",
                "sensor",
                "out",
                "value",
                Map.of("location", "room-1"),
                List.of("1m", "1h", "1d")
        );
        collector.registerDomainMetric(definition);

        collector.recordDomainMessage("flow-1", "sensor", "out",
                new com.fbp.engine.message.Message(Map.of("value", 30.0)));
        collector.recordDomainMessage("flow-1", "sensor", "out",
                new com.fbp.engine.message.Message(Map.of("value", 40.0)));

        DomainMetricSnapshot snapshot = collector.getDomainMetrics().get(0);
        assertEquals("temperature", snapshot.getName());
        assertEquals(2, snapshot.getCount());
        assertEquals(35.0, snapshot.getAverage());
        assertEquals(30.0, snapshot.getMin());
        assertEquals(40.0, snapshot.getMax());
        assertEquals(3, collector.getDomainWindowMetrics().size());
        assertTrue(collector.getDomainWindowMetrics().stream()
                .anyMatch(window -> "1m".equals(window.getWindow()) && window.getCount() == 2));
    }

    @Test
    @DisplayName("도메인 메트릭은 object.temperature 같은 중첩 필드를 추출할 수 있다")
    void domainMetricExtractsNestedPayloadField() {
        MetricsCollector collector = new MetricsCollector();
        DomainMetricDefinition definition = new DomainMetricDefinition(
                "temperature",
                "flow-1",
                "sensor",
                "out",
                "object.temperature",
                Map.of("profile", "LHT65"),
                List.of("1m")
        );
        collector.registerDomainMetric(definition);

        collector.recordDomainMessage("flow-1", "sensor", "out",
                new com.fbp.engine.message.Message(Map.of(
                        "object", Map.of("temperature", 23.5, "humidity", 65.2)
                )));

        DomainMetricSnapshot snapshot = collector.getDomainMetrics().get(0);
        assertEquals(1, snapshot.getCount());
        assertEquals(23.5, snapshot.getAverage());
    }

    @Test
    @DisplayName("도메인 메트릭 태그는 메시지 필드에서 동적으로 채울 수 있다")
    void domainMetricResolvesTagsFromMessageFields() {
        MetricsCollector collector = new MetricsCollector();
        DomainMetricDefinition definition = new DomainMetricDefinition(
                "temperature",
                "flow-1",
                "router",
                "temperature",
                "value",
                Map.of(
                        "location", "field:location",
                        "point", "field:point",
                        "sensor_type", "field:sensor_type",
                        "dev_eui", "field:dev_eui",
                        "source", "mqtt"
                ),
                List.of("1m")
        );
        collector.registerDomainMetric(definition);

        collector.recordDomainMessage("flow-1", "router", "temperature",
                new com.fbp.engine.message.Message(Map.of(
                        "value", 23.5,
                        "location", "서버실",
                        "point", "랙A",
                        "sensor_type", "LHT65",
                        "dev_eui", "a1b1c2d3e4f50011"
                )));

        DomainRawMetricSnapshot raw = collector.drainDomainRawMetrics().get(0);
        assertEquals(23.5, raw.getValue());
        assertEquals("서버실", raw.getTags().get("location"));
        assertEquals("랙A", raw.getTags().get("point"));
        assertEquals("LHT65", raw.getTags().get("sensor_type"));
        assertEquals("a1b1c2d3e4f50011", raw.getTags().get("dev_eui"));
        assertEquals("mqtt", raw.getTags().get("source"));
    }

    @Test
    @DisplayName("문 열림/닫힘 문자열은 InfluxDB에 저장 가능한 숫자 값으로 변환된다")
    void domainMetricConvertsDoorStatusToNumericValue() {
        MetricsCollector collector = new MetricsCollector();
        DomainMetricDefinition definition = new DomainMetricDefinition(
                "door",
                "flow-1",
                "sensor",
                "out",
                "object.magnet_status",
                Map.of("sensor_type", "field:deviceInfo.deviceProfileName"),
                List.of("1m")
        );
        collector.registerDomainMetric(definition);

        collector.recordDomainMessage("flow-1", "sensor", "out",
                new com.fbp.engine.message.Message(Map.of(
                        "deviceInfo", Map.of("deviceProfileName", "WS301"),
                        "object", Map.of("magnet_status", "open")
                )));

        DomainRawMetricSnapshot raw = collector.drainDomainRawMetrics().get(0);
        assertEquals(1.0, raw.getValue());
        assertEquals("WS301", raw.getTags().get("sensor_type"));
    }
}
