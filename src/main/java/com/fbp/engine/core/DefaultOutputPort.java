package com.fbp.engine.core;

import com.fbp.engine.core.interfaces.OutputPort;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class DefaultOutputPort implements OutputPort {
    private final String name;
    private final List<Connection> connections = new ArrayList<>();

    public DefaultOutputPort(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void connect(Connection connection) {
        connections.add(Objects.requireNonNull(connection));
    }

    @Override
    public void send(Message message) {
        for (Connection connection : connections) {
            try {
                connection.deliver(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("메시지 전달이 중단됨: port={}, connection={}", name, connection.getId(), e);
                return;
            }
        }
    }
}
