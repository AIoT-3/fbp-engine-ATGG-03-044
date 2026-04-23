package com.fbp.engine.Node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.CompositeRuleNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompositeRuleNodeTest {

    @Test
    @DisplayName("AND - 모두 만족")
    void AndMatchTest() {
        CompositeRuleNode node = new CompositeRuleNode("composite-1", CompositeRuleNode.Operator.AND);
        Connection matchConnection = new Connection();
        Connection mismatchConnection = new Connection();

        node.addCondition(message -> {
            Number value = message.get("temperature");
            return value != null && value.doubleValue() > 30.0;
        });
        node.addCondition(message -> {
            Number value = message.get("humidity");
            return value != null && value.doubleValue() > 70.0;
        });
        node.getOutputPort("match").connect(matchConnection);
        node.getOutputPort("mismatch").connect(mismatchConnection);
        node.process(new Message(Map.of("temperature", 32.0, "humidity", 75.0)));

        assertEquals(1, matchConnection.getBufferSize());
        assertEquals(0, mismatchConnection.getBufferSize());
    }

    @Test
    @DisplayName("AND - 하나 불만족")
    void AndMismatchTest() {
        CompositeRuleNode node = new CompositeRuleNode("composite-1", CompositeRuleNode.Operator.AND);
        Connection mismatchConnection = new Connection();

        node.addCondition(message -> {
            Number value = message.get("temperature");
            return value != null && value.doubleValue() > 30.0;
        });
        node.addCondition(message -> {
            Number value = message.get("humidity");
            return value != null && value.doubleValue() > 70.0;
        });
        node.getOutputPort("mismatch").connect(mismatchConnection);
        node.process(new Message(Map.of("temperature", 32.0, "humidity", 65.0)));

        assertEquals(1, mismatchConnection.getBufferSize());
    }

    @Test
    @DisplayName("OR - 하나 만족")
    void OrMatchTest() {
        CompositeRuleNode node = new CompositeRuleNode("composite-1", CompositeRuleNode.Operator.OR);
        Connection matchConnection = new Connection();

        node.addCondition(message -> {
            Number value = message.get("temperature");
            return value != null && value.doubleValue() > 30.0;
        });
        node.addCondition(message -> {
            Number value = message.get("humidity");
            return value != null && value.doubleValue() > 70.0;
        });
        node.getOutputPort("match").connect(matchConnection);
        node.process(new Message(Map.of("temperature", 32.0, "humidity", 65.0)));

        assertEquals(1, matchConnection.getBufferSize());
    }

    @Test
    @DisplayName("OR - 모두 불만족")
    void OrMismatchTest() {
        CompositeRuleNode node = new CompositeRuleNode("composite-1", CompositeRuleNode.Operator.OR);
        Connection mismatchConnection = new Connection();

        node.addCondition(message -> {
            Number value = message.get("temperature");
            return value != null && value.doubleValue() > 30.0;
        });
        node.addCondition(message -> {
            Number value = message.get("humidity");
            return value != null && value.doubleValue() > 70.0;
        });
        node.getOutputPort("mismatch").connect(mismatchConnection);
        node.process(new Message(Map.of("temperature", 28.0, "humidity", 65.0)));

        assertEquals(1, mismatchConnection.getBufferSize());
    }

    @Test
    @DisplayName("빈 조건")
    void EmptyConditionTest() {
        CompositeRuleNode andNode = new CompositeRuleNode("and-node", CompositeRuleNode.Operator.AND);
        CompositeRuleNode orNode = new CompositeRuleNode("or-node", CompositeRuleNode.Operator.OR);
        Connection andMatch = new Connection();
        Connection orMismatch = new Connection();

        andNode.getOutputPort("match").connect(andMatch);
        orNode.getOutputPort("mismatch").connect(orMismatch);

        andNode.process(new Message(Map.of("temperature", 20.0)));
        orNode.process(new Message(Map.of("temperature", 20.0)));

        assertEquals(1, andMatch.getBufferSize());
        assertEquals(1, orMismatch.getBufferSize());
    }
}
