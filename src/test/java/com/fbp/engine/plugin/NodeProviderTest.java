package com.fbp.engine.plugin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NodeProvider SPI - 플러그인이 제공하는 노드 설명 목록")
class NodeProviderTest {
    @Test
    @DisplayName("BuiltInNodeProvider는 기본 노드 Descriptor 목록과 factory를 제공한다")
    void builtInProviderReturnsDescriptors() {
        BuiltInNodeProvider provider = new BuiltInNodeProvider();

        assertFalse(provider.getNodeDescriptors().isEmpty());
        assertTrue(provider.getNodeDescriptors().stream().anyMatch(descriptor -> "Rule".equals(descriptor.getTypeName())));
        assertTrue(provider.getNodeDescriptors().stream().allMatch(descriptor -> descriptor.getFactory() != null));
    }

    @Test
    @DisplayName("Provider는 제공할 노드가 없으면 빈 Descriptor 목록을 반환할 수 있다")
    void providerCanReturnEmptyDescriptorList() {
        NodeProvider provider = java.util.List::of;

        assertTrue(provider.getNodeDescriptors().isEmpty());
    }

    @Test
    @DisplayName("NodeDescriptor는 typeName, nodeClass, factory가 없으면 생성할 수 없다")
    void descriptorRejectsMissingRequiredValues() {
        assertThrows(PluginException.class,
                () -> new NodeDescriptor(null, "description", com.fbp.engine.node.CollectorNode.class,
                        (id, config) -> new com.fbp.engine.node.CollectorNode(id)));
        assertThrows(PluginException.class,
                () -> new NodeDescriptor("Broken", "description", null,
                        (id, config) -> new com.fbp.engine.node.CollectorNode(id)));
        assertThrows(PluginException.class,
                () -> new NodeDescriptor("Broken", "description", com.fbp.engine.node.CollectorNode.class, null));
    }
}
