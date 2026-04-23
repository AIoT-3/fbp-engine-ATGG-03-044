package com.fbp.engine.core;

import com.fbp.engine.message.Message;

import java.util.Set;

public class RuleExpression {
    private static final Set<String> SUPPORTED_OPERATORS = Set.of(">", ">=", "<", "<=", "==", "!=");

    private final String field;
    private final String operator;
    private final Object value;

    public RuleExpression(String field, String operator, Object value) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        if (!SUPPORTED_OPERATORS.contains(operator)) {
            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public static RuleExpression parse(String expression) {
        String[] parts = expression.trim().split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid rule expression: " + expression);
        }

        String field = parts[0];
        String operator = parts[1];
        if (!SUPPORTED_OPERATORS.contains(operator)) {
            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        return new RuleExpression(field, operator, parseValue(parts[2]));
    }

    public boolean evaluate(Message message) {
        Object fieldValue = message.get(field);
        if (fieldValue == null) {
            return false;
        }

        if (fieldValue instanceof Number leftNumber && value instanceof Number rightNumber) {
            return compareNumbers(leftNumber.doubleValue(), rightNumber.doubleValue());
        }

        if (fieldValue instanceof String leftString) {
            return compareStrings(leftString, String.valueOf(value));
        }

        return false;
    }

    private boolean compareNumbers(double left, double right) {
        return switch (operator) {
            case ">" -> left > right;
            case ">=" -> left >= right;
            case "<" -> left < right;
            case "<=" -> left <= right;
            case "==" -> Double.compare(left, right) == 0;
            case "!=" -> Double.compare(left, right) != 0;
            default -> false;
        };
    }

    private boolean compareStrings(String left, String right) {
        return switch (operator) {
            case "==" -> left.equals(right);
            case "!=" -> !left.equals(right);
            case ">" -> left.compareTo(right) > 0;
            case ">=" -> left.compareTo(right) >= 0;
            case "<" -> left.compareTo(right) < 0;
            case "<=" -> left.compareTo(right) <= 0;
            default -> false;
        };
    }

    private static Object parseValue(String rawValue) {
        if ((rawValue.startsWith("\"") && rawValue.endsWith("\""))
                || (rawValue.startsWith("'") && rawValue.endsWith("'"))) {
            return rawValue.substring(1, rawValue.length() - 1);
        }

        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException ignored) {
            return rawValue;
        }
    }
}
