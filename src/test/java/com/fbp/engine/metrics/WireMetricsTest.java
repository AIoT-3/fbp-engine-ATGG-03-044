package com.fbp.engine.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("WireMetrics - 연결 단위 전달/드롭/큐 통계")
class WireMetricsTest {
    @Test
    @DisplayName("새 와이어 메트릭은 ID를 정리하고 모든 카운터를 0으로 시작한다")
    void newMetricsStartsWithZeroSnapshot() {
        WireMetrics metrics = new WireMetrics(" wire-1 ");

        WireMetricsSnapshot snapshot = metrics.snapshot();

        assertEquals("wire-1", snapshot.getWireId());
        assertEquals(0, snapshot.getDelivered());
        assertEquals(0, snapshot.getBytes());
        assertEquals(0, snapshot.getDropped());
        assertEquals(0, snapshot.getQueueSize());
        assertEquals(0.0, snapshot.getMqttRttMillis());
    }

    @Test
    @DisplayName("전달 수, 바이트 수, 드롭 수, MQTT 왕복 시간이 스냅샷에 반영된다")
    void recordsDeliveredDroppedQueueAndMqttRtt() {
        WireMetrics metrics = new WireMetrics("wire-1");

        metrics.recordDelivered(100, 2);
        metrics.recordDelivered(-50, -3);
        metrics.recordDropped();
        metrics.recordMqttRtt(1_000_000);
        metrics.recordMqttRtt(3_000_000);

        WireMetricsSnapshot snapshot = metrics.snapshot();
        assertEquals(2, snapshot.getDelivered());
        assertEquals(100, snapshot.getBytes());
        assertEquals(1, snapshot.getDropped());
        assertEquals(0, snapshot.getQueueSize());
        assertEquals(2.0, snapshot.getMqttRttMillis());
    }

    @Test
    @DisplayName("reset 호출 후 전달/드롭/큐/MQTT 지연 카운터가 초기화된다")
    void resetClearsAllCounters() {
        WireMetrics metrics = new WireMetrics("wire-1");
        metrics.recordDelivered(100, 5);
        metrics.recordDropped();
        metrics.recordMqttRtt(10_000_000);

        metrics.reset();

        WireMetricsSnapshot snapshot = metrics.snapshot();
        assertEquals(0, snapshot.getDelivered());
        assertEquals(0, snapshot.getBytes());
        assertEquals(0, snapshot.getDropped());
        assertEquals(0, snapshot.getQueueSize());
        assertEquals(0.0, snapshot.getMqttRttMillis());
    }

    @Test
    @DisplayName("와이어 ID가 null 또는 빈 문자열이면 생성할 수 없다")
    void blankWireIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WireMetrics(null));
        assertThrows(IllegalArgumentException.class, () -> new WireMetrics(" "));
    }
}
