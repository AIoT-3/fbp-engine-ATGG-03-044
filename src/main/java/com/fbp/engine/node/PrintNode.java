package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.interfaces.InputPort;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrintNode extends AbstractNode {

    public PrintNode(String id) {
        super(id);
        addInputPort("in");
    }
    public InputPort getInputPort(){
        return getInputPort("in");
    }
    @Override
    protected void onProcess(Message message) {
        log.info("[{}] {}", getId(), message.getPayload());
    }
}
