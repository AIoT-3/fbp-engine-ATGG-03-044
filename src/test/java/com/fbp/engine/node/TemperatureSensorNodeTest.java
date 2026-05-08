package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemperatureSensorNodeTest {
    private TemperatureSensorNode temperatureSensorNode;
    private Connection connection;
    @BeforeEach
    void setUp(){
        temperatureSensorNode = new TemperatureSensorNode("temp-sensor-1", 15.0, 45.0);
        connection = new LocalConnection();
        temperatureSensorNode.getOutputPort("out").connect(connection);
    }
    @Test
    @DisplayName("온도 범위 확인")
    void TemperatureRangeTest() throws InterruptedException {
        temperatureSensorNode.process(new Message());
        Message received = connection.poll();

        Double temperature = received.get("temperature");

        assertNotNull(temperature);
        assertTrue(temperature >= 15.0);
        assertTrue(temperature <= 45.0);
    }

    @Test
    @DisplayName("필수 키 포함")
    void RequiredKeyTest() throws InterruptedException {
        temperatureSensorNode.process(new Message());
        Message received = connection.poll();

        assertTrue(received.hasKey("sensorId"));
        assertTrue(received.hasKey("temperature"));
        assertTrue(received.hasKey("unit"));
        assertTrue(received.hasKey("timestamp"));
    }

    @Test
    @DisplayName("sensorId 일치")
    void SensorIdTest() throws InterruptedException {
        temperatureSensorNode.process(new Message());
        Message received = connection.poll();

        assertEquals("temp-sensor-1", received.get("sensorId"));
    }

    @Test
    @DisplayName("트리거마다 생성")
    void TriggerTest() {
        temperatureSensorNode.process(new Message());
        temperatureSensorNode.process(new Message());
        temperatureSensorNode.process(new Message());

        assertEquals(3, connection.getBufferSize());
    }
}