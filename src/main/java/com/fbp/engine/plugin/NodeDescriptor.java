package com.fbp.engine.plugin;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.registry.NodeFactory;

public class NodeDescriptor {
    private final String typeName;
    private final String description;
    private final Class<? extends AbstractNode> nodeClass;
    private final NodeFactory factory;

    public NodeDescriptor(String typeName, String description,
                          Class<? extends AbstractNode> nodeClass,
                          NodeFactory factory) {
        if (typeName == null || typeName.trim().isEmpty()) {
            throw new PluginException("typeName은 필수입니다");
        }
        if (nodeClass == null) {
            throw new PluginException("nodeClass는 필수입니다: " + typeName);
        }
        if (factory == null) {
            throw new PluginException("factory는 필수입니다: " + typeName);
        }

        this.typeName = typeName.trim();
        this.description = description;
        this.nodeClass = nodeClass;
        this.factory = factory;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getDescription() {
        return description;
    }

    public Class<? extends AbstractNode> getNodeClass() {
        return nodeClass;
    }

    public NodeFactory getFactory() {
        return factory;
    }
}
