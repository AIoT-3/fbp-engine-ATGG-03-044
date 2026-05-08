package com.fbp.engine.metrics;

import com.fbp.engine.message.Message;

import java.nio.charset.StandardCharsets;

public final class MessageMetrics {
    private MessageMetrics() {
    }

    public static long estimateBytes(Message message) {
        if (message == null) {
            return 0;
        }
        String text = message.getPayload().toString();
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
