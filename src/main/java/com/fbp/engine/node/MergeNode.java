package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.LinkedHashMap;
import java.util.Map;

public class MergeNode extends AbstractNode {
    private Message pending1;
    private Message pending2;

    public MergeNode(String id) {
        super(id);
        addInputPort("in-1");
        addInputPort("in-2");
        addOutputPort("out");
    }

    @Override
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
}
