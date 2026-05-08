package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HumiditySensorNodeTest {
    private HumiditySensorNode humiditySensorNode;
    private Connection connection;
    @BeforeEach
    void setUp() {
        humiditySensorNode = new HumiditySensorNode("humidity-sensor-1", 30.0, 90.0);
        connection = new LocalConnection();
        humiditySensorNode.getOutputPort("out").connect(connection);
    }
    @Test
    @DisplayName("습도 범위 확인")
    void HumidityRangeTest() throws InterruptedException {
        humiditySensorNode.process(new Message());
        Message received = connection.poll();

        Double humidity = received.get("humidity");

        assertNotNull(humidity);
        assertTrue(humidity >= 30.0);
        assertTrue(humidity <= 90.0);
    }

    @Test
    @DisplayName("필수 키 포함")
    void RequiredKeyTest() throws InterruptedException {
        humiditySensorNode.process(new Message());
        Message received = connection.poll();

        assertTrue(received.hasKey("sensorId"));
        assertTrue(received.hasKey("humidity"));
        assertTrue(received.hasKey("unit"));
    }
    @Test
    @DisplayName("sensorId 일치")
    void SensorIdTest() throws InterruptedException {
        humiditySensorNode.process(new Message());
        Message received = connection.poll();

        assertEquals("humidity-sensor-1", received.get("sensorId"));
    }

    @Test
    @DisplayName("트리거마다 생성")
    void TriggerTest() {
        humiditySensorNode.process(new Message());
        humiditySensorNode.process(new Message());
        humiditySensorNode.process(new Message());

        assertEquals(3, connection.getBufferSize());
    }

}