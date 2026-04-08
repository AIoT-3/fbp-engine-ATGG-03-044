package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlowTest {
    private Flow flow;
    private TestNode nodeA;
    private TestNode nodeB;
    private TestNode nodeC;

    @BeforeEach
    void setUp() {
        flow = new Flow("flow-1");
        nodeA = new TestNode("A");
        nodeB = new TestNode("B");
        nodeC = new TestNode("C");
    }

    static class TestNode extends AbstractNode {
        private boolean initialized;
        private boolean shutdown;

        public TestNode(String id) {
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
    @DisplayName("노드 등록")
    void AddNodeTest(){
        flow.addNode(nodeA);
        assertTrue(flow.getNodes().containsKey("A"));
        assertSame(nodeA,flow.getNodes().get("A"));
    }
    @Test
    @DisplayName("메서드 체이닝")
    void MethodChainingTest(){
        assertDoesNotThrow(() -> flow.addNode(nodeA)
                .addNode(nodeB)
                .connect("A","out","B","in"));
    }
    @Test
    @DisplayName("정상 연결")
    void ConnectTest() {
        flow.addNode(nodeA).addNode(nodeB);

        flow.connect("A", "out", "B", "in");

        assertEquals(1, flow.getConnections().size());
    }

    @Test
    @DisplayName("존재하지 않는 소스 노드 ID")
    void WrongSourceNodeTest() {
        flow.addNode(nodeB);

        assertThrows(IllegalArgumentException.class,
                () -> flow.connect("X", "out", "B", "in"));
    }

    @Test
    @DisplayName("존재하지 않는 대상 노드 ID")
    void WrongTargetNodeTest() {
        flow.addNode(nodeA);

        assertThrows(IllegalArgumentException.class,
                () -> flow.connect("A", "out", "X", "in"));
    }

    @Test
    @DisplayName("존재하지 않는 소스 포트")
    void WrongSourcePortTest() {
        flow.addNode(nodeA).addNode(nodeB);

        assertThrows(IllegalArgumentException.class,
                () -> flow.connect("A", "wrong", "B", "in"));
    }

    @Test
    @DisplayName("존재하지 않는 대상 포트")
    void WrongTargetPortTest() {
        flow.addNode(nodeA).addNode(nodeB);

        assertThrows(IllegalArgumentException.class,
                () -> flow.connect("A", "out", "B", "wrong"));
    }

    @Test
    @DisplayName("validate — 빈 Flow")
    void ValidateEmptyFlowTest() {
        List<String> errors = flow.validate();

        assertTrue(errors.contains("노드가 없습니다"));
    }

    @Test
    @DisplayName("validate — 정상 Flow")
    void ValidateNormalFlowTest() {
        flow.addNode(nodeA).addNode(nodeB)
                .connect("A", "out", "B", "in");

        List<String> errors = flow.validate();

        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("initialize — 전체 호출")
    void InitializeTest() {
        flow.addNode(nodeA).addNode(nodeB).addNode(nodeC);

        flow.initialize();

        assertTrue(nodeA.isInitialized());
        assertTrue(nodeB.isInitialized());
        assertTrue(nodeC.isInitialized());
    }

    @Test
    @DisplayName("shutdown — 전체 호출")
    void ShutdownTest() {
        flow.addNode(nodeA).addNode(nodeB).addNode(nodeC);

        flow.shutdown();

        assertTrue(nodeA.isShutdown());
        assertTrue(nodeB.isShutdown());
        assertTrue(nodeC.isShutdown());
    }

    @Test
    @DisplayName("순환 참조 탐지 (도전)")
    void CycleTest() {
        flow.addNode(nodeA).addNode(nodeB).addNode(nodeC)
                .connect("A", "out", "B", "in")
                .connect("B", "out", "C", "in")
                .connect("C", "out", "A", "in");

        List<String> errors = flow.validate();

        assertTrue(errors.contains("순환 참조가 있습니다."));
    }

}