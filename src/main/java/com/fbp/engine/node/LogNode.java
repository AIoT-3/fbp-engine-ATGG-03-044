package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.interfaces.InputPort;
import com.fbp.engine.core.interfaces.OutputPort;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogNode extends AbstractNode {
    public LogNode(String id){
        super(id);
        addInputPort("in");
        addOutputPort("out");
    }

    public InputPort getInputPort() {
        return getInputPort("in");
    }
    public OutputPort getOutputPort() {
        return getOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        log.info("[{}] {}", getId(), message.getPayload());
        send("out", message);
    }
}
