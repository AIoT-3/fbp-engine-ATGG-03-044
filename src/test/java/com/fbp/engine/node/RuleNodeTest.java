package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleNodeTest {

    @Test
    @DisplayName("조건 만족 -> match")
    void MatchTest() throws Exception {
        RuleNode ruleNode = new RuleNode("rule-1", message -> {
            Number value = message.get("temperature");
            return value != null && value.doubleValue() > 30.0;
        });
        Connection matchConnection = new LocalConnection();
        Connection mismatchConnection = new LocalConnection();

        ruleNode.getOutputPort("match").connect(matchConnection);
        ruleNode.getOutputPort("mismatch").connect(mismatchConnection);
        ruleNode.process(new Message(Map.of("temperature", 35.0)));

        Message received = matchConnection.poll();

        assertEquals(35.0, received.get("temperature"));
        assertEquals(0, mismatchConnection.getBufferSize());
    }

    @Test
    @DisplayName("조건 불만족 -> mismatch")
    void MismatchTest() throws Exception {
        RuleNode ruleNode = new RuleNode("rule-1", message -> {
            Number value = message.get("temperature");
            return value != null && value.doubleValue() > 30.0;
        });
        Connection matchConnection = new LocalConnection();
        Connection mismatchConnection = new LocalConnection();

        ruleNode.getOutputPort("match").connect(matchConnection);
        ruleNode.getOutputPort("mismatch").connect(mismatchConnection);
        ruleNode.process(new Message(Map.of("temperature", 25.0)));

        Message received = mismatchConnection.poll();

        assertEquals(25.0, received.get("temperature"));
        assertEquals(0, matchConnection.getBufferSize());
    }

    @Test
    @DisplayName("포트 구성")
    void PortTest() {
        RuleNode ruleNode = new RuleNode("rule-1", message -> true);

        assertNotNull(ruleNode.getInputPort("in"));
        assertNotNull(ruleNode.getOutputPort("match"));
        assertNotNull(ruleNode.getOutputPort("mismatch"));
    }

    @Test
    @DisplayName("null 필드 처리")
    void NullFieldTest() throws Exception {
        RuleNode ruleNode = new RuleNode("rule-1", message -> {
            Number value = message.get("temperature");
            return value != null && value.doubleValue() > 30.0;
        });
        Connection mismatchConnection = new LocalConnection();

        ruleNode.getOutputPort("mismatch").connect(mismatchConnection);
        ruleNode.process(new Message(Map.of("humidity", 60)));

        Message received = mismatchConnection.poll();

        assertEquals(Integer.valueOf(60), received.get("humidity"));
    }

    @Test
    @DisplayName("다수 메시지 분기")
    void MultiMessageTest() throws Exception {
        RuleNode ruleNode = new RuleNode("rule-1", message -> {
            Number value = message.get("temperature");
            return value != null && value.doubleValue() > 30.0;
        });
        Connection matchConnection = new LocalConnection();
        Connection mismatchConnection = new LocalConnection();

        ruleNode.getOutputPort("match").connect(matchConnection);
        ruleNode.getOutputPort("mismatch").connect(mismatchConnection);

        ruleNode.process(new Message(Map.of("temperature", 35.0)));
        ruleNode.process(new Message(Map.of("temperature", 25.0)));
        ruleNode.process(new Message(Map.of("temperature", 31.0)));

        assertEquals(2, matchConnection.getBufferSize());
        assertEquals(1, mismatchConnection.getBufferSize());

        assertEquals(35.0, matchConnection.poll().get("temperature"));
        assertEquals(31.0, matchConnection.poll().get("temperature"));
        assertEquals(25.0, mismatchConnection.poll().get("temperature"));
    }
}
