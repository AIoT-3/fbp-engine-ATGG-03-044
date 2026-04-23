package com.fbp.engine.core.interfaces;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;

public interface OutputPort {
    String getName();
    void connect(Connection connection);
    void send(Message message);
}
