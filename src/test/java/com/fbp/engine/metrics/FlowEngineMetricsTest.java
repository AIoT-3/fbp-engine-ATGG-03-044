package com.fbp.engine.metrics;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FlowEngine + MetricsCollector - 실제 플로우 실행 중 메트릭 기록")
class FlowEngineMetricsTest {
    @Test
    @DisplayName("정상 처리된 메시지는 노드 입력/출력, 와이어 전달, 도메인 메트릭에 기록된다")
    void recordsSuccessfulNodeProcessing() throws InterruptedException {
        MetricsCollector collector = new MetricsCollector();
        collector.registerDomainMetric(new DomainMetricDefinition(
                "value",
                "flow-1",
                "source",
                "out",
                "value",
                Map.of()
        ));
        FlowEngine engine = new FlowEngine(collector);
        EmittingNode source = new EmittingNode("source");
        LatchNode target = new LatchNode("target", false);
        Flow flow = new Flow("flow-1")
                .addNode(source)
                .addNode(target)
                .connect("source", "out", "target", "in");

        engine.register(flow);
        engine.startFlow("flow-1");
        source.emit(new Message(Map.of("value", 1)));

        assertTrue(target.await());
        assertTrue(awaitMetric(() -> collector.getNodeMetrics("target").getProcessed() == 1));
        assertEquals(0, collector.getNodeMetrics("target").getErrors());
        assertEquals(1, collector.getNodeMetrics("target").getInputCount());
        assertEquals(1, collector.getNodeMetrics("source").getOutputCount());
        assertEquals(1, collector.getWireMetrics("source:out->target:in").getDelivered());
        assertEquals(1, collector.getDomainMetrics().get(0).getCount());

        engine.stopFlow("flow-1");
    }

    @Test
    @DisplayName("노드 처리 중 예외가 발생하면 processed와 errors가 함께 증가한다")
    void recordsFailedNodeProcessing() throws InterruptedException {
        MetricsCollector collector = new MetricsCollector();
        FlowEngine engine = new FlowEngine(collector);
        EmittingNode source = new EmittingNode("source");
        LatchNode target = new LatchNode("target", true);
        Flow flow = new Flow("flow-1")
                .addNode(source)
                .addNode(target)
                .connect("source", "out", "target", "in");

        engine.register(flow);
        engine.startFlow("flow-1");
        source.emit(new Message(Map.of("value", 1)));

        assertTrue(target.await());
        assertTrue(awaitMetric(() -> collector.getNodeMetrics("target").getErrors() == 1));
        assertEquals(1, collector.getNodeMetrics("target").getProcessed());

        engine.stopFlow("flow-1");
    }

    private boolean awaitMetric(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }

    static class EmittingNode extends AbstractNode {
        EmittingNode(String id) {
            super(id);
            addOutputPort("out");
        }

        void emit(Message message) {
            send("out", message);
        }

        @Override
        protected void onProcess(Message message) {
        }
    }

    static class LatchNode extends AbstractNode {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final boolean fail;

        LatchNode(String id, boolean fail) {
            super(id);
            this.fail = fail;
            addInputPort("in");
        }

        @Override
        protected void onProcess(Message message) {
            latch.countDown();
            if (fail) {
                throw new IllegalStateException("expected failure");
            }
        }

        boolean await() throws InterruptedException {
            return latch.await(1, TimeUnit.SECONDS);
        }
    }
}
