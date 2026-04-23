package com.fbp.engine.node;

public class TemperatureSensorNode extends SensorNode {
    public TemperatureSensorNode(String id, double min, double max) {
        super(id, min, max, "temperature", "°C");
    }
}
