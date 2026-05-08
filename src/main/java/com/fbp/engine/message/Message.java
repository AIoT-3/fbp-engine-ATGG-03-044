package com.fbp.engine.message;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
public class Message {
    private final UUID id;
    private final Map<String, Object> payload;
    private final long timestamp;

    public Message(Map<String, Object> payload) {
        this(UUID.randomUUID(), payload, System.currentTimeMillis());
    }

    public Message() {
        this(Map.of());
    }

    private Message(UUID id, Map<String, Object> payload, long timestamp) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.payload = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payload, "payload must not be null")));
        this.timestamp = timestamp;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (key == null) {
            return null;
        }
        if (payload.containsKey(key)) {
            return (T) payload.get(key);
        }
        if (!key.contains(".")) {
            return null;
        }

        Object current = payload;
        String[] parts = key.split("\\.");
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = currentMap.get(part);
            if (current == null) {
                return null;
            }
        }
        return (T) current;
    }

    public Message withEntry(String key, Object value) {
        Map<String, Object> updatedPayload = new LinkedHashMap<>(payload);
        updatedPayload.put(key, value);
        return new Message(updatedPayload);
    }

    public boolean hasKey(String key) {
        return get(key) != null;
    }

    public Message withoutKey(String key) {
        Map<String, Object> updatedPayload = new LinkedHashMap<>(payload);
        updatedPayload.remove(key);
        return new Message(updatedPayload);
    }

}
