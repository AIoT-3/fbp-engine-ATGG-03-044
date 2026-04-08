package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.interfaces.InputPort;
import com.fbp.engine.core.interfaces.OutputPort;
import com.fbp.engine.message.Message;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LogNode extends AbstractNode {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
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
        String now = LocalTime.now().format(FORMATTER);
        System.out.println("[" + now + "][" + getId() + "] " + message.getPayload());
        send("out", message);
    }

}
