package com.fbp.engine.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.message.Message;

import java.io.IOException;
import java.util.Map;

public class JacksonMessageSerializer implements MessageSerializer {
    private final ObjectMapper objectMapper;

    public JacksonMessageSerializer() {
        this(new ObjectMapper());
    }

    public JacksonMessageSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(Message message) {
        try {
            return objectMapper.writeValueAsBytes(message.getPayload());
        } catch (IOException e) {
            throw new TransportException("메시지 직렬화 실패", e);
        }
    }

    @Override
    public Message deserialize(byte[] payload) {
        try {
            TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> parsedPayload = objectMapper.readValue(payload, mapType);
            return new Message(parsedPayload);
        } catch (IOException e) {
            throw new TransportException("메시지 역직렬화 실패", e);
        }
    }
}
