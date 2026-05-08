package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FilterNodeTest {
    private FilterNode filterNode;
    private Connection connection;
    @BeforeEach
    void setUp() {
        filterNode = new FilterNode("filter-1", "temperature", 20.0);
        connection = new LocalConnection();
        filterNode.getOutputPort("out").connect(connection);
    }

    @Test
    @DisplayName("조건 만족 시 통과")
    void ThresholdTest() {
        Message message = new Message(Map.of("temperature", 25.5));

        assertTrue(filterNode.matches(message));
    }

    @Test
    @DisplayName("조건 미달 시 차단")
    void NoThresholdTest() {
        Message message = new Message(Map.of("temperature", 15.0));

        assertFalse(filterNode.matches(message));
    }

    @Test
    @DisplayName("경계값 처리")
    void EdgeThresholdTest() {
        Message message = new Message(Map.of("temperature", 20.0));
        assertTrue(filterNode.matches(message));
    }

    @Test
    @DisplayName("키 없는 메시지")
    void NoKeyTest() {
        Message message = new Message(Map.of("humidity", 60));

        assertDoesNotThrow(() -> filterNode.matches(message));
        assertFalse(filterNode.matches(message));
    }
    @Test
    @DisplayName("조건 만족 → send 호출")
    void conditionGoodTest() throws InterruptedException{
        Message message = new Message(Map.of("temperature", 25.5));
        filterNode.process(message);
        Message received = connection.poll();
        assertSame(message, received);

    }
    @Test
    @DisplayName("조건 미달 → 차단")
    void conditionNotGoodTest(){
        Message message = new Message(Map.of("temperature", 10.5));
        filterNode.process(message);
        assertEquals(0,connection.getBufferSize());
    }
    @Test
    @DisplayName("포트 구성 확인")
    void checkPortTest(){
        assertNotNull(filterNode.getOutputPort("out"));
        assertNotNull(filterNode.getInputPort("in"));
    }
}
