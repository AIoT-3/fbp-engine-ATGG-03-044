package com.fbp.engine.registry;

import com.fbp.engine.core.AbstractNode;

import java.util.Map;

@FunctionalInterface
public interface NodeFactory {
    AbstractNode create(String id, Map<String, Object> config);
}
