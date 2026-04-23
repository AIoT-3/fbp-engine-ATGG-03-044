package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.RuleExpression;
import com.fbp.engine.message.Message;

import java.util.Objects;
import java.util.function.Predicate;

public class RuleNode extends AbstractNode {
    private final Predicate<Message> condition;

    public RuleNode(String id, Predicate<Message> condition) {
        super(id);
        this.condition = Objects.requireNonNull(condition);
        addInputPort("in");
        addOutputPort("match");
        addOutputPort("mismatch");
    }

    public RuleNode(String id, String expression) {
        this(id, RuleExpression.parse(expression)::evaluate);
    }

    @Override
    protected void onProcess(Message message) {
        if (condition.test(message)) {
            send("match", message);
        } else {
            send("mismatch", message);
        }
    }
}
