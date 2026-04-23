package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Predicate;

public class TimeWindowRuleNode extends AbstractNode {
    private final Predicate<Message> condition;
    private final long windowMs;
    private final int threshold;
    private final Queue<Long> events = new ArrayDeque<>();

    public TimeWindowRuleNode(String id, Predicate<Message> condition, long windowMs, int threshold) {
        super(id);
        this.condition = Objects.requireNonNull(condition);
        this.windowMs = windowMs;
        this.threshold = threshold;
        addInputPort("in");
        addOutputPort("alert");
        addOutputPort("pass");
    }

    @Override
    protected synchronized void onProcess(Message message) {
        long now = System.currentTimeMillis();

        if (condition.test(message)) {
            events.add(now);
        }

        removeExpired(now);

        if (events.size() >= threshold) {
            send("alert", message);
        } else {
            send("pass", message);
        }
    }

    private void removeExpired(long now) {
        while (!events.isEmpty() && now - events.peek() > windowMs) {
            events.poll();
        }
    }
}
