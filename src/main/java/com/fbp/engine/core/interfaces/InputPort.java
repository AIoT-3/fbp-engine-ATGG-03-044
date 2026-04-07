package com.fbp.engine.core.interfaces;

import com.fbp.engine.message.Message;

public interface InputPort {
    public String getName();
    public void receive(Message message);
}
