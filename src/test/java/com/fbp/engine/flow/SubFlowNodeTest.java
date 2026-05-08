package com.fbp.engine.flow;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubFlowNode - 플로우 안에 내장된 재사용 플로우")
class SubFlowNodeTest {
    @Test
    @DisplayName("외부 입력 메시지는 내부 플로우를 지나 외부 출력 포트로 나온다")
    void passesMessageThroughInternalFlowToExternalOutput() {
        Flow internalFlow = flowWithTransforms("inner-1");
        SubFlowNode subFlowNode = new SubFlowNode("sub", internalFlow, "input", "in", "output", "out");
        Connection output = new LocalConnection("sub-out");
        subFlowNode.getOutputPort("out").connect(output);

        subFlowNode.initialize();
        try {
            subFlowNode.process(new Message(Map.of("steps", "start")));

            Message result = assertTimeoutPreemptively(Duration.ofSeconds(2), output::poll);
            assertEquals("start>input>normalizer>output", result.get("steps"));
        } finally {
            subFlowNode.shutdown();
        }
    }

    @Test
    @DisplayName("SubFlowNode 시작/정지는 내부 플로우 노드들의 생명주기와 동기화된다")
    void startsAndStopsInternalFlowLifecycle() {
        LifecycleNode input = new LifecycleNode("input");
        LifecycleNode worker = new LifecycleNode("worker");
        LifecycleNode output = new LifecycleNode("output");
        Flow internalFlow = new Flow("inner-lifecycle")
                .addNode(input)
                .addNode(worker)
                .addNode(output)
                .connect("input", "out", "worker", "in")
                .connect("worker", "out", "output", "in");
        SubFlowNode subFlowNode = new SubFlowNode("sub", internalFlow, "input", "in", "output", "out");

        subFlowNode.initialize();
        assertTrue(input.initialized);
        assertTrue(worker.initialized);
        assertTrue(output.initialized);

        subFlowNode.shutdown();
        assertTrue(input.shutdown);
        assertTrue(worker.shutdown);
        assertTrue(output.shutdown);
    }

    @Test
    @DisplayName("같은 구조의 서브플로우를 여러 인스턴스로 만들어 독립 실행할 수 있다")
    void canCreateMultipleInstancesFromSameFlowShape() {
        SubFlowNode first = new SubFlowNode("sub-1", flowWithTransforms("inner-a"), "input", "in", "output", "out");
        SubFlowNode second = new SubFlowNode("sub-2", flowWithTransforms("inner-b"), "input", "in", "output", "out");
        Connection firstOutput = new LocalConnection("first-out");
        Connection secondOutput = new LocalConnection("second-out");
        first.getOutputPort("out").connect(firstOutput);
        second.getOutputPort("out").connect(secondOutput);

        first.initialize();
        second.initialize();
        try {
            first.process(new Message(Map.of("steps", "one")));
            second.process(new Message(Map.of("steps", "two")));

            Message firstResult = assertTimeoutPreemptively(Duration.ofSeconds(2), firstOutput::poll);
            Message secondResult = assertTimeoutPreemptively(Duration.ofSeconds(2), secondOutput::poll);
            assertEquals("one>input>normalizer>output", firstResult.get("steps"));
            assertEquals("two>input>normalizer>output", secondResult.get("steps"));
        } finally {
            first.shutdown();
            second.shutdown();
        }
    }

    @Test
    @DisplayName("서브플로우 입력 처리 중 예외가 나면 외부 error 포트로 에러 메시지를 보낸다")
    void sendsDirectInputFailureToErrorPort() {
        Flow internalFlow = new Flow("inner-error")
                .addNode(new FailingNode("input"))
                .addNode(new StepNode("output", "output"));
        SubFlowNode subFlowNode = new SubFlowNode("sub", internalFlow, "input", "in", "output", "out");
        Connection error = new LocalConnection("sub-error");
        subFlowNode.getOutputPort("error").connect(error);

        subFlowNode.initialize();
        try {
            subFlowNode.process(new Message(Map.of("value", 10)));

            Message errorMessage = assertTimeoutPreemptively(Duration.ofSeconds(2), error::poll);
            assertEquals("sub", errorMessage.get("sourceNodeId"));
            assertEquals(IllegalStateException.class.getName(), errorMessage.get("errorType"));
            assertEquals("expected subflow failure", errorMessage.get("errorMessage"));
            assertEquals(Map.of("value", 10), errorMessage.get("originalPayload"));
        } finally {
            subFlowNode.shutdown();
        }
    }

    private Flow flowWithTransforms(String flowId) {
        return new Flow(flowId)
                .addNode(new StepNode("input", "input"))
                .addNode(new StepNode("normalizer", "normalizer"))
                .addNode(new StepNode("output", "output"))
                .connect("input", "out", "normalizer", "in")
                .connect("normalizer", "out", "output", "in");
    }

    private static class StepNode extends AbstractNode {
        private final String step;

        private StepNode(String id, String step) {
            super(id);
            this.step = step;
            addInputPort("in");
            addOutputPort("out");
        }

        @Override
        protected void onProcess(Message message) {
            String previous = message.get("steps");
            send("out", message.withEntry("steps", previous + ">" + step));
        }
    }

    private static class LifecycleNode extends StepNode {
        private boolean initialized;
        private boolean shutdown;

        private LifecycleNode(String id) {
            super(id, id);
        }

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }
    }

    private static class FailingNode extends AbstractNode {
        private FailingNode(String id) {
            super(id);
            addInputPort("in");
        }

        @Override
        protected void onProcess(Message message) {
            throw new IllegalStateException("expected subflow failure");
        }
    }
}
