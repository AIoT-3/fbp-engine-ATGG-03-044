package com.fbp.engine.node;

import com.fbp.engine.core.RuleExpression;
import com.fbp.engine.message.Message;

import java.util.Objects;

public class RoutingRule {
    private final String field;
    private final String operator;
    private final Object value;
    private final String outputPort;
    private final RuleExpression expression;

    public RoutingRule(String field, String operator, Object value, String outputPort) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("field는 필수입니다");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new IllegalArgumentException("operator는 필수입니다");
        }
        if (outputPort == null || outputPort.trim().isEmpty()) {
            throw new IllegalArgumentException("outputPort는 필수입니다");
        }

        this.field = field.trim();
        this.operator = operator.trim();
        this.value = value;
        this.outputPort = outputPort.trim();
        this.expression = new RuleExpression(this.field, this.operator, this.value);
    }

    public boolean matches(Message message) {
        return expression.evaluate(message);
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    public String getOutputPort() {
        return outputPort;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RoutingRule otherRule)) {
            return false;
        }
        return Objects.equals(field, otherRule.field)
                && Objects.equals(operator, otherRule.operator)
                && Objects.equals(value, otherRule.value)
                && Objects.equals(outputPort, otherRule.outputPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, operator, value, outputPort);
    }

    @Override
    public String toString() {
        return "RoutingRule{"
                + "field='" + field + '\''
                + ", operator='" + operator + '\''
                + ", value=" + value
                + ", outputPort='" + outputPort + '\''
                + '}';
    }
}
