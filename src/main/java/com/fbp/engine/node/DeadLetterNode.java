package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeadLetterNode extends AbstractNode {
    private final List<Message> messages = Collections.synchronizedList(new ArrayList<>());

    public DeadLetterNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    protected void onProcess(Message message) {
        messages.add(message);
    }

    public List<Message> getMessages() {
        synchronized (messages) {
            return List.copyOf(messages);
        }
    }
}
