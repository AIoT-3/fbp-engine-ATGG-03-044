package com.fbp.engine.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("NodeMetrics - 노드 단위 처리량/에러/큐 통계")
class NodeMetricsTest {
    @Test
    @DisplayName("새 노드 메트릭은 ID를 정리하고 모든 카운터를 0으로 시작한다")
    void newMetricsStartsWithZeroSnapshot() {
        NodeMetrics metrics = new NodeMetrics(" node-1 ");

        NodeMetricsSnapshot snapshot = metrics.snapshot();

        assertEquals("node-1", snapshot.getNodeId());
        assertEquals(0, snapshot.getProcessed());
        assertEquals(0, snapshot.getErrors());
        assertEquals(0.0, snapshot.getAverageTimeMillis());
        assertEquals(0, snapshot.getQueueSize());
        assertEquals(0.0, snapshot.getP99TimeMillis());
    }

    @Test
    @DisplayName("성공/실패/입출력 바이트/큐 크기/99p 처리 시간이 스냅샷에 반영된다")
    void recordsSuccessErrorIoBytesQueueAndPercentile() {
        NodeMetrics metrics = new NodeMetrics("node-1");

        metrics.recordInput(100);
        metrics.recordInput(-10);
        metrics.recordOutput(250);
        metrics.recordOutput(-20);
        metrics.recordSuccess(1_000_000);
        metrics.recordSuccess(2_000_000);
        metrics.recordError(3_000_000);
        metrics.setQueueSize(7);

        NodeMetricsSnapshot snapshot = metrics.snapshot();
        assertEquals(3, snapshot.getProcessed());
        assertEquals(1, snapshot.getErrors());
        assertEquals(2.0, snapshot.getAverageTimeMillis());
        assertEquals(7, snapshot.getQueueSize());
        assertEquals(2, snapshot.getInputCount());
        assertEquals(2, snapshot.getOutputCount());
        assertEquals(100, snapshot.getInputBytes());
        assertEquals(250, snapshot.getOutputBytes());
        assertEquals(3.0, snapshot.getP99TimeMillis());
    }

    @Test
    @DisplayName("음수 처리 시간과 음수 큐 크기는 0으로 보정된다")
    void negativeDurationAndQueueAreClampedToZero() {
        NodeMetrics metrics = new NodeMetrics("node-1");

        metrics.recordSuccess(-1);
        metrics.recordError(-1);
        metrics.setQueueSize(-5);

        NodeMetricsSnapshot snapshot = metrics.snapshot();
        assertEquals(2, snapshot.getProcessed());
        assertEquals(1, snapshot.getErrors());
        assertEquals(0.0, snapshot.getAverageTimeMillis());
        assertEquals(0.0, snapshot.getP99TimeMillis());
        assertEquals(0, snapshot.getQueueSize());
    }

    @Test
    @DisplayName("reset 호출 후 모든 누적 카운터가 0으로 초기화된다")
    void resetClearsAllCounters() {
        NodeMetrics metrics = new NodeMetrics("node-1");
        metrics.recordInput(10);
        metrics.recordOutput(20);
        metrics.recordSuccess(5_000_000);
        metrics.recordError(7_000_000);
        metrics.setQueueSize(3);

        metrics.reset();

        NodeMetricsSnapshot snapshot = metrics.snapshot();
        assertEquals(0, snapshot.getProcessed());
        assertEquals(0, snapshot.getErrors());
        assertEquals(0, snapshot.getInputCount());
        assertEquals(0, snapshot.getOutputCount());
        assertEquals(0, snapshot.getInputBytes());
        assertEquals(0, snapshot.getOutputBytes());
        assertEquals(0, snapshot.getQueueSize());
        assertEquals(0.0, snapshot.getP99TimeMillis());
    }

    @Test
    @DisplayName("노드 ID가 null 또는 빈 문자열이면 생성할 수 없다")
    void blankNodeIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NodeMetrics(null));
        assertThrows(IllegalArgumentException.class, () -> new NodeMetrics(" "));
    }
}
