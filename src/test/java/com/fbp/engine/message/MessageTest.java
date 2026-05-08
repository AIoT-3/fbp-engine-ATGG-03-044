package com.fbp.engine.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {
    private Map<String, Object> sourcePayload;
    private Message message;

    @BeforeEach
    void setUp() {
        sourcePayload = new HashMap<>();
        sourcePayload.put("temperature", 25.5);
        sourcePayload.put("location", "room1");

        message = new Message(sourcePayload);
    }

    @Test
    @DisplayName("생성 시 ID 자동 할당")
    void getIdIsNotBlankTest() {
        assertNotNull(message.getId());
        assertFalse(message.getId().toString().isBlank());
    }
    @Test
    @DisplayName("생성 시 timestamp 자동 기록")
    void getTimestampTest() {
        assertTrue(message.getTimestamp() > 0);
    }
    @Test
    @DisplayName("페이로드 조회")
    void getPayloadTest() {
        assertEquals(25.5, message.get("temperature"));
        assertEquals("room1", message.get("location"));
    }

    @Test
    @DisplayName("점 경로로 중첩 페이로드 조회")
    void getNestedPayloadPathTest() {
        Message nestedMessage = new Message(Map.of(
                "deviceInfo", Map.of("deviceName", "LHT65-001"),
                "object", Map.of("temperature", 23.5, "humidity", 65.2)
        ));

        assertEquals("LHT65-001", nestedMessage.get("deviceInfo.deviceName"));
        assertEquals(23.5, nestedMessage.get("object.temperature"));
        assertEquals(65.2, nestedMessage.get("object.humidity"));
        assertTrue(nestedMessage.hasKey("object.temperature"));
        assertFalse(nestedMessage.hasKey("object.pressure"));
    }

    @Test
    @DisplayName("실제 키가 있으면 점 경로보다 실제 키 우선")
    void directKeyHasPriorityOverNestedPathTest() {
        Message nestedMessage = new Message(Map.of(
                "object.temperature", 99.0,
                "object", Map.of("temperature", 23.5)
        ));

        assertEquals(99.0, nestedMessage.get("object.temperature"));
    }

    @Test
    @DisplayName("제네릭 get 타입 캐스팅")
    void GenericGetTest() {
        Double temperature = message.get("temperature");
        assertEquals(25.5, temperature);
    }
    @Test
    @DisplayName("존재하지 않는 키 조회")
    void getNullKeyTest() {
        assertNull(message.get("없는키"));
    }
    @Test
    @DisplayName("페이로드 불변 - 외부 수정 차단")
    void ExternalPayloadTest() {
        assertThrows(UnsupportedOperationException.class, () ->
                message.getPayload().put("humidity", 60)
        );
    }

    @Test
    @DisplayName("페이로드 불변 - 원본 Map 수정 무영향")
    void MapUpdateNotAffectTest() {
        sourcePayload.put("temperature", 99.9);
        sourcePayload.put("humidity", 60);

        assertEquals(25.5, message.get("temperature"));
        assertNull(message.get("humidity"));
    }

    @Test
    @DisplayName("withEntry — 새 객체 반환")
    void NewWithEntryTest() {
        Message newMessage = message.withEntry("humidity", 60);
        assertNotSame(message, newMessage);

    }

    @Test
    @DisplayName("withEntry — 원본 불변")
    void NotKeyWithEntryTest() {
        message.withEntry("humidity", 60);
        assertFalse(message.hasKey("humidity"));

    }
    @Test
    @DisplayName("withEntry — 새 메시지에 값 존재")
    void NewMessageGetTest(){
        Message newMessage = message.withEntry("humidity", 60);
        Integer humidity = newMessage.get("humidity");
        assertEquals(60, humidity);
    }
    @Test
    @DisplayName("hasKey - 존재하는 키")
    void ExistingHasKeyTest() {
        assertTrue(message.hasKey("temperature"));
    }
    @Test
    @DisplayName("hasKey - 없는 키")
    void NullHasKeyTest() {
        assertFalse(message.hasKey("없는키"));
    }
    @Test
    @DisplayName("withoutKey - 키 제거 확인")
    void RemoveKeyTest() {
        Message newMessage = message.withoutKey("temperature");

        assertFalse(newMessage.hasKey("temperature"));
    }
    @Test
    @DisplayName("withoutKey - 원본 불변")
    void NotWithoutKeyTest() {
        message.withoutKey("temperature");

        assertTrue(message.hasKey("temperature"));
    }
    @Test
    @DisplayName("toString 포맷")
    void ToStringTest() {
        String result = message.toString();

        assertNotNull(result);
        assertTrue(result.contains("temperature"));
    }
}
