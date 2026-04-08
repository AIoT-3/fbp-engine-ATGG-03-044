package com.fbp.engine.Node;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AlertNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertNodeTest {
    private AlertNode alertNode;
    @BeforeEach
    void setUp() {
        alertNode = new AlertNode("alert-1");
    }
    @Test
    @DisplayName("정상 처리")
    void NormalProcessTest() {
        Message message = new Message(Map.of(
                "sensorId", "sensor-1",
                "temperature", 35.5
        ));

        assertDoesNotThrow(() -> alertNode.process(message));
    }

    @Test
    @DisplayName("키 누락 시 처리")
    void MissingKeyTest() {
        Message message = new Message(Map.of(
                "sensorId", "sensor-1"
        ));

        assertDoesNotThrow(() -> alertNode.process(message));
    }


}