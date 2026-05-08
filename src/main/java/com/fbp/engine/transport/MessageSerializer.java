package com.fbp.engine.transport;

import com.fbp.engine.message.Message;

public interface MessageSerializer {
    byte[] serialize(Message message);

    Message deserialize(byte[] payload);
}
