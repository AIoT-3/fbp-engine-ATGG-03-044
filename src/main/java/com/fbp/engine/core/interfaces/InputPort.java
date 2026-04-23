package com.fbp.engine.core.interfaces;

import com.fbp.engine.message.Message;

public interface InputPort {
    String getName();
    void receive(Message message);
}
