package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.RuleExpression;
import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class CompositeRuleNode extends AbstractNode {
    public enum Operator {
        AND,
        OR
    }

    private final List<Predicate<Message>> conditions = new ArrayList<>();
    private final Operator operator;

    public CompositeRuleNode(String id, Operator operator) {
        super(id);
        this.operator = Objects.requireNonNull(operator);
        addInputPort("in");
        addOutputPort("match");
        addOutputPort("mismatch");
    }

    public void addCondition(Predicate<Message> condition) {
        conditions.add(Objects.requireNonNull(condition));
    }

    public void addCondition(String field, String op, Object value) {
        RuleExpression expression = new RuleExpression(field, op, value);
        addCondition(expression::evaluate);
    }

    @Override
    protected void onProcess(Message message) {
        if (conditions.isEmpty()) {
            if (operator == Operator.AND) {
                send("match", message);
            } else {
                send("mismatch", message);
            }
            return;
        }

        boolean matched = switch (operator) {
            case AND -> conditions.stream().allMatch(condition -> condition.test(message));
            case OR -> conditions.stream().anyMatch(condition -> condition.test(message));
        };

        if (matched) {
            send("match", message);
        } else {
            send("mismatch", message);
        }
    }
}
