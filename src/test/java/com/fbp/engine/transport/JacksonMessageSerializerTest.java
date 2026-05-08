package com.fbp.engine.transport;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JacksonMessageSerializer - Message를 MQTT payload용 JSON bytes로 변환")
class JacksonMessageSerializerTest {
    @Test
    @DisplayName("Message payload를 JSON bytes로 직렬화하고 다시 Message로 복원한다")
    void serializesAndDeserializesMessagePayload() {
        JacksonMessageSerializer serializer = new JacksonMessageSerializer();
        Message message = new Message(Map.of(
                "sensor", "temperature",
                "value", 23.5,
                "nested", Map.of("location", "room-1")
        ));

        byte[] bytes = serializer.serialize(message);
        Message restored = serializer.deserialize(bytes);

        assertEquals("temperature", restored.get("sensor"));
        assertEquals(23.5, (Double) restored.get("value"));
        assertEquals("room-1", restored.get("nested.location"));
    }

    @Test
    @DisplayName("깨진 JSON bytes를 역직렬화하면 TransportException으로 보고한다")
    void invalidJsonIsReportedAsTransportException() {
        JacksonMessageSerializer serializer = new JacksonMessageSerializer();
        byte[] invalidJson = "{ invalid".getBytes(StandardCharsets.UTF_8);

        assertThrows(TransportException.class,
                () -> serializer.deserialize(invalidJson));
    }
}
