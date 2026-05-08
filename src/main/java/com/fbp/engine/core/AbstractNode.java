package com.fbp.engine.core;

import com.fbp.engine.core.interfaces.InputPort;
import com.fbp.engine.core.interfaces.OutputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.metrics.MetricsCollector;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractNode implements Node {
    private final String id;
    private final Map<String, InputPort> inputPorts = new HashMap<>();
    private final Map<String, OutputPort> outputPorts = new HashMap<>();
    protected AbstractNode(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }
    protected InputPort addInputPort(String name) {
        InputPort inputPort = new DefaultInputPort(name, this);
        inputPorts.put(name, inputPort);
        return inputPort;
    }
    protected OutputPort addOutputPort(String name) {
        OutputPort outputPort = new DefaultOutputPort(name, id);
        outputPorts.put(name, outputPort);
        return outputPort;
    }


    public OutputPort getOutputPort(String name) {
        return outputPorts.get(name);
    }
    protected void send(String name, Message message) {
        OutputPort outputPort = outputPorts.get(name);
        if(outputPort != null){
            outputPort.send(message);
        }
    }

    public InputPort getInputPort(String name) {
        return inputPorts.get(name);
    }

    public void attachMetrics(String flowId, MetricsCollector metricsCollector) {
        for (OutputPort outputPort : outputPorts.values()) {
            if (outputPort instanceof DefaultOutputPort defaultOutputPort) {
                defaultOutputPort.attachMetrics(flowId, metricsCollector);
            }
        }
    }

    public void detachMetrics() {
        for (OutputPort outputPort : outputPorts.values()) {
            if (outputPort instanceof DefaultOutputPort defaultOutputPort) {
                defaultOutputPort.detachMetrics();
            }
        }
    }


    @Override
    public final void process(Message message) {
        onProcess(message);
    }

    protected abstract void onProcess(Message message);

    @Override
    public void initialize() {
    }

    @Override
    public void shutdown() {
    }
}
