package com.fbp.engine.parser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NodeDefinition {
    private final String id;
    private final String type;
    private final Map<String, Object> config;

    public NodeDefinition(String id, String type, Map<String, Object> config) {
        this.id = ParserValidation.requireText(id, "노드 id는 필수입니다");
        this.type = ParserValidation.requireText(type, "노드 type은 필수입니다");

        Map<String, Object> sourceConfig;
        if (config == null) {
            sourceConfig = Collections.emptyMap();
        } else {
            sourceConfig = config;
        }

        Map<String, Object> copiedConfig = new LinkedHashMap<>(sourceConfig);
        this.config = Collections.unmodifiableMap(copiedConfig);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}
