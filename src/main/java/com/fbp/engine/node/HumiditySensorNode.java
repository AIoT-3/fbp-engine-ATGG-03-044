package com.fbp.engine.node;

public class HumiditySensorNode extends SensorNode {
    public HumiditySensorNode(String id, double min, double max) {
        super(id, min, max, "humidity", "%");
    }
}
