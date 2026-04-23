package com.fbp.engine.protocol;

import java.util.concurrent.atomic.AtomicInteger;

public final class TestPorts {
    private static final AtomicInteger NEXT_PORT = new AtomicInteger(5500);

    private TestPorts() {
    }

    public static int nextPort() {
        return NEXT_PORT.getAndIncrement();
    }
}
