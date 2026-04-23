package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleExpressionTest {

    @Test
    @DisplayName("파싱 - 숫자 비교")
    void NumberTest() {
        RuleExpression expression = RuleExpression.parse("temperature > 30.0");

        assertTrue(expression.evaluate(new Message(Map.of("temperature", 31.5))));
        assertFalse(expression.evaluate(new Message(Map.of("temperature", 29.5))));
    }

    @Test
    @DisplayName("파싱 - 문자열 비교")
    void StringTest() {
        RuleExpression expression = RuleExpression.parse("status == ON");

        assertTrue(expression.evaluate(new Message(Map.of("status", "ON"))));
        assertFalse(expression.evaluate(new Message(Map.of("status", "OFF"))));
    }

    @Test
    @DisplayName("모든 연산자")
    void OperatorTest() {
        assertTrue(RuleExpression.parse("temperature > 30").evaluate(new Message(Map.of("temperature", 31))));
        assertTrue(RuleExpression.parse("temperature >= 30").evaluate(new Message(Map.of("temperature", 30))));
        assertTrue(RuleExpression.parse("temperature < 30").evaluate(new Message(Map.of("temperature", 29))));
        assertTrue(RuleExpression.parse("temperature <= 30").evaluate(new Message(Map.of("temperature", 30))));
        assertTrue(RuleExpression.parse("status == ON").evaluate(new Message(Map.of("status", "ON"))));
        assertTrue(RuleExpression.parse("status != OFF").evaluate(new Message(Map.of("status", "ON"))));
    }

    @Test
    @DisplayName("잘못된 표현식")
    void InvalidExpressionTest() {
        assertThrows(IllegalArgumentException.class, () -> RuleExpression.parse("temperature"));
        assertThrows(IllegalArgumentException.class, () -> RuleExpression.parse("temperature >< 30"));
    }

    @Test
    @DisplayName("필드 없음")
    void MissingFieldTest() {
        RuleExpression expression = RuleExpression.parse("temperature > 30");

        assertFalse(expression.evaluate(new Message(Map.of("humidity", 50))));
    }
}
