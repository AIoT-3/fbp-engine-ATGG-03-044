package com.fbp.engine.registry;

import com.fbp.engine.core.AbstractNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class NodeRegistry {
    private final Map<String, NodeFactory> factories = new LinkedHashMap<>();

    public synchronized void register(String typeName, NodeFactory factory) {
        String normalizedType = normalizeTypeName(typeName);
        Objects.requireNonNull(factory, "factory must not be null");
        if (factories.containsKey(normalizedType)) {
            throw new NodeRegistryException("이미 등록된 노드 타입입니다: " + normalizedType);
        }
        factories.put(normalizedType, factory);
    }

    public synchronized AbstractNode create(String typeName, String nodeId, Map<String, Object> config) {
        String normalizedType = normalizeTypeName(typeName);
        String normalizedNodeId = normalizeNodeId(nodeId);
        NodeFactory factory = factories.get(normalizedType);
        if (factory == null) {
            throw new NodeRegistryException("등록되지 않은 노드 타입입니다: " + normalizedType);
        }
        try {
            Map<String, Object> safeConfig;
            if (config == null) {
                safeConfig = Collections.emptyMap();
            } else {
                safeConfig = config;
            }

            AbstractNode node = factory.create(normalizedNodeId, safeConfig);
            if (node == null) {
                throw new NodeRegistryException("노드 팩토리가 null을 반환했습니다: " + normalizedType);
            }
            return node;
        } catch (NodeRegistryException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new NodeRegistryException("노드 생성 실패: " + normalizedType + "(" + normalizedNodeId + ")", e);
        }
    }

    public synchronized boolean isRegistered(String typeName) {
        String normalizedType = normalizeTypeName(typeName);
        return factories.containsKey(normalizedType);
    }

    public synchronized Set<String> getRegisteredTypes() {
        Set<String> typeNames = factories.keySet();
        Set<String> copiedTypeNames = new LinkedHashSet<>(typeNames);
        Set<String> readOnlyTypeNames = Collections.unmodifiableSet(copiedTypeNames);
        return readOnlyTypeNames;
    }

    private String normalizeTypeName(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            throw new NodeRegistryException("노드 타입명은 비어 있을 수 없습니다");
        }
        return typeName.trim();
    }

    private String normalizeNodeId(String nodeId) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new NodeRegistryException("노드 id는 비어 있을 수 없습니다");
        }
        return nodeId.trim();
    }
}
