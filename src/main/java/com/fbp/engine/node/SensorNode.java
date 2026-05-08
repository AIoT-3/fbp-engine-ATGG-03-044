package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public abstract class SensorNode extends AbstractNode {
    private final double min;
    private final double max;
    private final String valueKey;
    private final String unit;

    protected SensorNode(String id, double min, double max, String valueKey, String unit) {
        super(id);
        this.min = min;
        this.max = max;
        this.valueKey = valueKey;
        this.unit = unit;
        addInputPort("trigger");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        double value = ThreadLocalRandom.current().nextDouble(min, max);
        value = Math.round(value * 10) / 10.0;

        send("out", new Message(Map.of(
                "sensorId", getId(),
                valueKey, value,
                "unit", unit,
                "timestamp", System.currentTimeMillis()
        )));
    }
}
