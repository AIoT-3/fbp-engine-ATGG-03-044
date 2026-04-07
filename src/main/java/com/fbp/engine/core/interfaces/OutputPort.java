package com.fbp.engine.core.interfaces;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;

public interface OutputPort {
    public String getName();
    public void connect(Connection connection);
    public void send(Message message);
}
