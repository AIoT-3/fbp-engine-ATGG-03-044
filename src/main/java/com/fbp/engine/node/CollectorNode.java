package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.List;

public class CollectorNode extends AbstractNode {
    private final List<Message> messages = new ArrayList<>();

    public CollectorNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    protected synchronized void onProcess(Message message) {
        messages.add(message);
    }

    public synchronized List<Message> getCollected() {
        return new ArrayList<>(messages);
    }
}
