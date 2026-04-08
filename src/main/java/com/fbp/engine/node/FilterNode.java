package com.fbp.engine.node;


import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.interfaces.InputPort;
import com.fbp.engine.core.interfaces.OutputPort;
import com.fbp.engine.message.Message;

public class FilterNode extends AbstractNode {

    private final String key;
    private final double threshold;

    public FilterNode(String id, String key, double threshold) {
        super(id);
        this.key = key;
        this.threshold = threshold;
        addInputPort("in");
        addOutputPort("out");
    }
    public  InputPort getInputPort() {
        return getInputPort("in");
    }
    public OutputPort getOutputPort() {
        return getOutputPort("out");
    }
    public boolean matches(Message message) {
        Object value = message.get(key);

        if (value == null) {
            return false;
        }

        if (value instanceof Number number) {
            return number.doubleValue() >= threshold;
        }

        return false;
    }

    @Override
    protected void onProcess(Message message) {
        Object value = message.get(key);
        if (value == null) {
            return;
        }
        if(value instanceof Number number){
            if(number.doubleValue() >= threshold){
                send("out",message);
            }
        }
    }
}
