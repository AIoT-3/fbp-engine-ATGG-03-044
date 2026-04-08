package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.Map;

public class TemperatureSensorNode extends AbstractNode {
    private final double min;
    private final double max;

    public TemperatureSensorNode(String id, double min, double max) {
        super(id);
        this.min = min;
        this.max = max;
        addInputPort("trigger");
        addOutputPort("out");
    }
    @Override
    protected void onProcess(Message message) {
        double temperature = min + Math.random() * (max - min);
        temperature = Math.round(temperature*10)/10.0;
        Message sensorMessage = new Message(Map.of(
                "sensorId", getId(),
                "temperature", temperature,
                "unit", "°C",
                "timestamp", System.currentTimeMillis()
        ));
        send("out", sensorMessage);
    }

}
