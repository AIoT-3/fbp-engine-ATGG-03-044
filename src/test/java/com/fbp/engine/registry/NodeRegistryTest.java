package com.fbp.engine.registry;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NodeRegistry - 타입명으로 노드 팩토리를 등록하고 생성")
class NodeRegistryTest {
    @Test
    @DisplayName("등록한 타입명으로 노드를 생성하고 config가 생성자까지 전달된다")
    void registerAndCreate() {
        NodeRegistry registry = new NodeRegistry();
        registry.register("Test", TestNode::new);

        AbstractNode node = registry.create("Test", "node-1", Map.of("value", 10));

        assertInstanceOf(TestNode.class, node);
        assertEquals("node-1", node.getId());
        assertEquals(10, ((TestNode) node).value);
    }

    @Test
    @DisplayName("등록되지 않은 타입명으로 create를 호출하면 예외가 발생한다")
    void createUnknownTypeFails() {
        NodeRegistry registry = new NodeRegistry();

        assertThrows(NodeRegistryException.class,
                () -> registry.create("Unknown", "node-1", Map.of()));
    }

    @Test
    @DisplayName("같은 타입명을 두 번 등록하면 중복 등록 예외가 발생한다")
    void duplicateTypeFails() {
        NodeRegistry registry = new NodeRegistry();
        registry.register("Test", TestNode::new);

        assertThrows(NodeRegistryException.class,
                () -> registry.register("Test", TestNode::new));
    }

    @Test
    @DisplayName("등록 타입 목록과 isRegistered 결과가 현재 Registry 상태와 일치한다")
    void registeredTypesAreReturned() {
        NodeRegistry registry = new NodeRegistry();
        registry.register("A", TestNode::new);
        registry.register("B", TestNode::new);

        assertEquals(2, registry.getRegisteredTypes().size());
        assertTrue(registry.isRegistered("A"));
        assertFalse(registry.isRegistered("C"));
    }

    @Test
    @DisplayName("빈 타입명은 등록할 수 없다")
    void blankTypeFails() {
        NodeRegistry registry = new NodeRegistry();

        assertThrows(NodeRegistryException.class,
                () -> registry.register(" ", TestNode::new));
    }

    static class TestNode extends AbstractNode {
        private final Object value;

        TestNode(String id, Map<String, Object> config) {
            super(id);
            this.value = config.get("value");
            addInputPort("in");
            addOutputPort("out");
        }

        @Override
        protected void onProcess(Message message) {
            send("out", message);
        }
    }
}
