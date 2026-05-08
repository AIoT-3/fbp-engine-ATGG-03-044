package com.fbp.engine.parser;

import java.util.List;

final class ParserValidation {
    private ParserValidation() {
    }

    static String requireText(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new FlowParserException(errorMessage);
        }
        return value.trim();
    }

    static <T> List<T> requireNonEmptyList(List<T> values, String errorMessage) {
        if (values == null || values.isEmpty()) {
            throw new FlowParserException(errorMessage);
        }
        return values;
    }
}
