package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlowEngineTest {
    private FlowEngine engine;
    private TestFlowNode nodeA;
    private TestFlowNode nodeB;
    private TestFlowNode nodeC;

    @BeforeEach
    void setUp() {
        engine = new FlowEngine();
        nodeA = new TestFlowNode("A");
        nodeB = new TestFlowNode("B");
        nodeC = new TestFlowNode("C");
    }

    static class TestFlowNode extends AbstractNode {
        private boolean initialized;
        private boolean shutdown;

        public TestFlowNode(String id) {
            super(id);
            addInputPort("in");
            addOutputPort("out");
        }

        @Override
        protected void onProcess(Message message) {
        }

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public boolean isShutdown() {
            return shutdown;
        }
    }
    @Test
    @DisplayName("초기 상태")
    void InitialStateTest(){
        assertEquals(State.INITIALIZED,engine.getState());
    }
    @Test
    @DisplayName("플로우 등록")
    void FlowRegisterTest(){
        Flow flow = new Flow("flow-1")
                .addNode(nodeA)
                .addNode(nodeB)
                .connect("A","out","B","in");
        engine.register(flow);
        assertTrue(engine.getFlows().containsKey("flow-1"));
        assertEquals(flow, engine.getFlows().get("flow-1"));
    }
    @Test
    @DisplayName("startFlow 정상")
    void StartFlowTest(){
        Flow flow = new Flow("flow-1")
                .addNode(nodeA)
                .addNode(nodeB)
                .connect("A","out","B","in");
        engine.register(flow);
        engine.startFlow("flow-1");
        assertEquals(State.RUNNING,engine.getState());
        assertEquals(State.RUNNING,engine.getFlowStates().get("flow-1"));
    }
    @Test
    @DisplayName("startFlow - 없는 ID")
    void WrongFlowIdTest() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.startFlow("not-exist"));
    }

    @Test
    @DisplayName("startFlow - 유효성 실패")
    void InvalidFlowTest() {
        Flow invalidFlow = new Flow("invalid");

        engine.register(invalidFlow);

        assertThrows(IllegalStateException.class,
                () -> engine.startFlow("invalid"));
    }

    @Test
    @DisplayName("stopFlow 정상")
    void StopFlowTest() {
        Flow flow = new Flow("flow-1")
                .addNode(nodeA)
                .addNode(nodeB)
                .connect("A", "out", "B", "in");

        engine.register(flow);
        engine.startFlow("flow-1");
        engine.stopFlow("flow-1");

        assertEquals(State.STOPPED, engine.getFlowStates().get("flow-1"));
        assertTrue(nodeA.isShutdown());
        assertTrue(nodeB.isShutdown());
    }

    @Test
    @DisplayName("shutdown 전체")
    void ShutdownAllTest() {
        Flow flow1 = new Flow("flow-1")
                .addNode(nodeA)
                .addNode(nodeB)
                .connect("A", "out", "B", "in");

        Flow flow2 = new Flow("flow-2")
                .addNode(nodeC);

        engine.register(flow1);
        engine.register(flow2);
        engine.startFlow("flow-1");
        engine.shutdown();

        assertEquals(State.STOPPED, engine.getState());
        assertEquals(State.STOPPED, engine.getFlowStates().get("flow-1"));
        assertEquals(State.STOPPED, engine.getFlowStates().get("flow-2"));
    }

    @Test
    @DisplayName("다중 플로우 독립 동작")
    void IndependentFlowStateTest() {
        TestFlowNode nodeD = new TestFlowNode("D");

        Flow flow1 = new Flow("flow-1")
                .addNode(nodeA)
                .addNode(nodeB)
                .connect("A", "out", "B", "in");

        Flow flow2 = new Flow("flow-2")
                .addNode(nodeC)
                .addNode(nodeC)
                .addNode(nodeD)
                .connect("C", "out", "D", "in");

        engine.register(flow1);
        engine.register(flow2);

        engine.startFlow("flow-1");
        engine.startFlow("flow-2");
        engine.stopFlow("flow-1");

        assertEquals(State.STOPPED, engine.getFlowStates().get("flow-1"));
        assertEquals(State.RUNNING, engine.getFlowStates().get("flow-2"));
    }

    @Test
    @DisplayName("listFlows 출력")
    void ListFlowsTest() {
        Flow flow1 = new Flow("flow-1")
                .addNode(nodeA)
                .addNode(nodeB)
                .connect("A", "out", "B", "in");

        Flow flow2 = new Flow("flow-2")
                .addNode(nodeC);

        engine.register(flow1);
        engine.register(flow2);

        assertDoesNotThrow(() -> engine.listFlows());
        assertTrue(engine.getFlows().containsKey("flow-1"));
        assertTrue(engine.getFlows().containsKey("flow-2"));
        assertEquals(State.STOPPED, engine.getFlowStates().get("flow-1"));
        assertEquals(State.STOPPED, engine.getFlowStates().get("flow-2"));
    }
}