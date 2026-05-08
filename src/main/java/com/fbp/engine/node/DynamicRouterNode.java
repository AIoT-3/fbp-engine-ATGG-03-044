package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.List;

public class DynamicRouterNode extends AbstractNode {
    private final List<RoutingRule> rules = new ArrayList<>();
    private final String defaultPort;

    public DynamicRouterNode(String id, List<RoutingRule> rules, String defaultPort) {
        super(id);
        if (defaultPort == null || defaultPort.trim().isEmpty()) {
            this.defaultPort = "default";
        } else {
            this.defaultPort = defaultPort.trim();
        }
        addInputPort("in");
        addOutputPort(this.defaultPort);
        if (rules != null) {
            rules.forEach(this::addRule);
        }
    }

    public synchronized void addRule(RoutingRule rule) {
        String outputPort = rule.getOutputPort();
        if (getOutputPort(outputPort) == null) {
            addOutputPort(outputPort);
        }
        rules.add(rule);
    }

    public synchronized boolean removeRule(RoutingRule rule) {
        return rules.remove(rule);
    }

    public synchronized List<RoutingRule> getRules() {
        return List.copyOf(rules);
    }

    @Override
    protected void onProcess(Message message) {
        List<RoutingRule> snapshot;
        synchronized (this) {
            snapshot = List.copyOf(rules);
        }
        for (RoutingRule rule : snapshot) {
            if (rule.matches(message)) {
                send(rule.getOutputPort(), message);
                return;
            }
        }
        send(defaultPort, message);
    }
}
