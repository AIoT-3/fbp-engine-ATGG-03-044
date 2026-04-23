package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.Node;
import com.fbp.engine.core.interfaces.InputPort;
import com.fbp.engine.message.Message;

import java.util.LinkedHashMap;
import java.util.Map;

public class MergeNode extends AbstractNode {
    private final InputPort inputPort1;
    private final InputPort inputPort2;
    private Message pending1;
    private Message pending2;

    public MergeNode(String id) {
        super(id);
        inputPort1 = new MergeInputPort("in-1", this);
        inputPort2 = new MergeInputPort("in-2", this);
        addOutputPort("out");
    }

    @Override
    public InputPort getInputPort(String name) {
        if ("in-1".equals(name)) {
            return inputPort1;
        }
        if ("in-2".equals(name)) {
            return inputPort2;
        }
        return super.getInputPort(name);
    }

    @Override
    // receive() always routes through process(), and onProcess() is synchronized.
    protected synchronized void onProcess(Message message) {
        String inputPort = message.get("_inputPort");
        Message cleanMessage = message.withoutKey("_inputPort");

        if ("in-1".equals(inputPort)) {
            pending1 = cleanMessage;
        } else if ("in-2".equals(inputPort)) {
            pending2 = cleanMessage;
        } else {
            return;
        }

        if (pending1 != null && pending2 != null) {
            Map<String, Object> mergedPayload = new LinkedHashMap<>(pending1.getPayload());
            mergedPayload.putAll(pending2.getPayload());

            send("out", new Message(mergedPayload));

            pending1 = null;
            pending2 = null;
        }
    }

    private static class MergeInputPort implements InputPort {
        private final String name;
        private final Node owner;

        private MergeInputPort(String name, Node owner) {
            this.name = name;
            this.owner = owner;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void receive(Message message) {
            owner.process(message.withEntry("_inputPort", name));
        }
    }
}
