package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CounterNode extends AbstractNode {
    private final AtomicInteger count = new AtomicInteger();
    public CounterNode(String id) {
        super(id);
        addInputPort("in");
        addOutputPort("out");
    }
    @Override
    protected void onProcess(Message message) {
        int currentCount = count.incrementAndGet();
        Message newMessage = message.withEntry("count", currentCount);
        send("out", newMessage);
    }

    @Override
    public void shutdown() {
        log.info("[{}] 총 처리 메시지: {}건", getId(), count.get());
    }
}
