package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;

public class ErrorHandlerNode extends AbstractNode {
    private final int maxRetries;

    public ErrorHandlerNode(String id) {
        this(id, 0);
    }

    public ErrorHandlerNode(String id, int maxRetries) {
        super(id);
        this.maxRetries = Math.max(0, maxRetries);
        addInputPort("in");
        addOutputPort("retry");
        addOutputPort("deadLetter");
    }

    @Override
    protected void onProcess(Message message) {
        Integer retryCount = message.get("retryCount");
        int currentRetry;
        if (retryCount == null) {
            currentRetry = 0;
        } else {
            currentRetry = retryCount;
        }

        if (currentRetry < maxRetries) {
            send("retry", message.withEntry("retryCount", currentRetry + 1));
        } else {
            send("deadLetter", message);
        }
    }
}
