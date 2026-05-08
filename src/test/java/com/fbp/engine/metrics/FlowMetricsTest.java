package com.fbp.engine.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("FlowMetrics - 플로우 단위 메트릭 집계 결과")
class FlowMetricsTest {
    @Test
    @DisplayName("노드 처리 수/에러 수와 와이어 전달 수를 플로우 합계로 계산한다")
    void aggregatesNodeAndWireTotals() {
        FlowMetrics metrics = new FlowMetrics(
                "flow-1",
                List.of(
                        new NodeMetricsSnapshot("a", 3, 1, 2.0, 0),
                        new NodeMetricsSnapshot("b", 5, 0, 4.0, 0)
                ),
                List.of(
                        new WireMetricsSnapshot("w-1", 7, 100, 0, 0, 0.0),
                        new WireMetricsSnapshot("w-2", 11, 200, 1, 2, 1.5)
                )
        );

        assertEquals("flow-1", metrics.getFlowId());
        assertEquals(8, metrics.getTotalProcessed());
        assertEquals(1, metrics.getTotalErrors());
        assertEquals(18, metrics.getTotalWireDelivered());
    }

    @Test
    @DisplayName("생성자 입력 리스트를 복사하고 조회 리스트는 수정할 수 없게 반환한다")
    void copiesInputListsAndReturnsUnmodifiableViews() {
        List<NodeMetricsSnapshot> nodes = new ArrayList<>();
        nodes.add(new NodeMetricsSnapshot("a", 1, 0, 0.0, 0));
        FlowMetrics metrics = new FlowMetrics("flow-1", nodes);

        nodes.add(new NodeMetricsSnapshot("b", 1, 0, 0.0, 0));

        assertEquals(1, metrics.getNodes().size());
        assertEquals(0, metrics.getWires().size());
        List<NodeMetricsSnapshot> nodeView = metrics.getNodes();
        List<WireMetricsSnapshot> wireView = metrics.getWires();
        NodeMetricsSnapshot extraNode = new NodeMetricsSnapshot("c", 1, 0, 0.0, 0);
        WireMetricsSnapshot extraWire = new WireMetricsSnapshot("w", 1, 0, 0, 0, 0.0);

        assertThrows(UnsupportedOperationException.class,
                () -> nodeView.add(extraNode));
        assertThrows(UnsupportedOperationException.class,
                () -> wireView.add(extraWire));
    }
}
