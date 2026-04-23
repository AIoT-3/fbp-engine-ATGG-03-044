package com.fbp.engine.Node;

import com.fbp.engine.core.Node;
import com.fbp.engine.message.Message;
import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.node.PrintNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PrintNodeTest {
    private PrintNode printNode;
    private Message message;
    @BeforeEach
    void setUp(){
        printNode = new PrintNode("printer-1");
        message = new Message(Map.of("temperature", 25.5));
    }
    @Test
    @DisplayName("getId 반환")
    void getIdTest() {
        assertEquals("printer-1", printNode.getId());
    }
    @Test
    @DisplayName("process 정상 동작")
    void processTest() {
        assertDoesNotThrow(()-> printNode.process(message));
    }
    @Test
    @DisplayName("Node 인터페이스 구현")
    void InterfaceTest(){
        Node node = new PrintNode("printer-1");
        assertNotNull(node);
    }
    @Test
    @DisplayName("InputPort 조회")
    void InputPortGetTest() {
        assertNotNull(printNode.getInputPort());
    }
    @Test
    @DisplayName("InputPort를 통한 수신")
    void shouldProcessMessageWhenInputPortReceives() {
        Message message = new Message(Map.of("temperature", 25.5));
        assertDoesNotThrow(() -> printNode.getInputPort().receive(message));
    }
    @Test
    @DisplayName("포트 구성 확인")
    void portTest(){
        assertNotNull(printNode.getInputPort("in"));
    }
    @Test
    @DisplayName("process 정상 동작")
    void processGoodTest(){
        Message message = new Message(Map.of("temperature", 25.5));
        assertDoesNotThrow(()-> printNode.process(message));
    }
    @Test
    @DisplayName("AbstractNode 상속 확인")
    void abstractNodeTest(){
        assertTrue(printNode instanceof AbstractNode);
    }
}
