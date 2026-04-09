package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;

public class HumiditySensorNode extends AbstractNode {
    private final double min;
    private final double max;
ㅊ
    public HumiditySensorNode(String id, double min, double max) {
        super(id);
        this.min = min;
        this.max = max;
        addInputPort("trigger");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(com.fbp.engine.message.Message message) {
        double humidity = min + Math.random() * (max - min);
        humidity = Math.round(humidity * 10) / 10.0;
        com.fbp.engine.message.Message sensorMessage = new com.fbp.engine.message.Message(java.util.Map.of(
                "sensorId", getId(),
                "humidity", humidity,
                "unit", "%",
                "timestamp", System.currentTimeMillis()
        ));
        send("out", sensorMessage);
    }

}
