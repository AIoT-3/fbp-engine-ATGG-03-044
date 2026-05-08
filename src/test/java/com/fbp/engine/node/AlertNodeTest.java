package com.fbp.engine.node;

import com.fbp.engine.message.Message;
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

    @Test
    @DisplayName("LHT65 중첩 payload 처리")
    void NestedPayloadTest() {
        Message message = new Message(Map.of(
                "deviceInfo", Map.of("deviceName", "LHT65-001"),
                "object", Map.of("temperature", 31.5, "humidity", 72.0)
        ));

        assertDoesNotThrow(() -> alertNode.process(message));
    }

    @Test
    @DisplayName("Node-RED 재발행 payload 처리")
    void RepublishedPayloadTest() {
        Message message = new Message(Map.of(
                "measurement_key", "temperature",
                "value", 31.5,
                "device_name", "LHT65-001"
        ));

        assertDoesNotThrow(() -> alertNode.process(message));
    }


}
