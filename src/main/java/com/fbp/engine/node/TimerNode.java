package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerNode extends AbstractNode {
    private final long intervalMs;
    private final AtomicInteger tickCount;
    private ScheduledExecutorService scheduler;
    public TimerNode(String id, long intervalMs) {
        super(id);
        this.intervalMs = intervalMs;
        this.tickCount = new AtomicInteger();
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
    }

    @Override
    public void initialize() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            int tick = tickCount.getAndIncrement();
            Message message = new Message(Map.of(
                    "tick", tick,
                    "timestamp", System.currentTimeMillis()
            ));
            send("out",message);
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        if(scheduler != null){
            scheduler.shutdown();
        }
    }
}
