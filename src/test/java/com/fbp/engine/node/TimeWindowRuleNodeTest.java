package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeWindowRuleNodeTest {

    @Test
    @DisplayName("기준 미달 -> pass")
    void PassTest() {
        TimeWindowRuleNode node = new TimeWindowRuleNode(
                "time-rule-1",
                message -> {
                    Number value = message.get("temperature");
                    return value != null && value.doubleValue() > 30.0;
                },
                5000,
                3
        );
        Connection passConnection = new LocalConnection();
        node.getOutputPort("pass").connect(passConnection);

        node.process(new Message(Map.of("temperature", 31.0)));
        node.process(new Message(Map.of("temperature", 32.0)));

        assertEquals(2, passConnection.getBufferSize());
    }

    @Test
    @DisplayName("기준 도달 -> alert")
    void AlertTest() {
        TimeWindowRuleNode node = new TimeWindowRuleNode(
                "time-rule-1",
                message -> {
                    Number value = message.get("temperature");
                    return value != null && value.doubleValue() > 30.0;
                },
                5000,
                3
        );
        Connection alertConnection = new LocalConnection();
        node.getOutputPort("alert").connect(alertConnection);

        node.process(new Message(Map.of("temperature", 31.0)));
        node.process(new Message(Map.of("temperature", 32.0)));
        node.process(new Message(Map.of("temperature", 33.0)));

        assertEquals(1, alertConnection.getBufferSize());
    }

    @Test
    @DisplayName("시간 창 만료")
    void ExpireTest() throws Exception {
        TimeWindowRuleNode node = new TimeWindowRuleNode(
                "time-rule-1",
                message -> {
                    Number value = message.get("temperature");
                    return value != null && value.doubleValue() > 30.0;
                },
                100,
                2
        );
        Connection alertConnection = new LocalConnection();
        Connection passConnection = new LocalConnection();
        node.getOutputPort("alert").connect(alertConnection);
        node.getOutputPort("pass").connect(passConnection);

        node.process(new Message(Map.of("temperature", 31.0)));
        Thread.sleep(150);
        node.process(new Message(Map.of("temperature", 32.0)));

        assertEquals(0, alertConnection.getBufferSize());
        assertEquals(2, passConnection.getBufferSize());
    }

    @Test
    @DisplayName("조건 불만족 메시지")
    void ConditionFailTest() {
        TimeWindowRuleNode node = new TimeWindowRuleNode(
                "time-rule-1",
                message -> {
                    Number value = message.get("temperature");
                    return value != null && value.doubleValue() > 30.0;
                },
                5000,
                2
        );
        Connection passConnection = new LocalConnection();
        node.getOutputPort("pass").connect(passConnection);

        node.process(new Message(Map.of("temperature", 25.0)));
        node.process(new Message(Map.of("temperature", 31.0)));

        assertEquals(2, passConnection.getBufferSize());
    }
}
