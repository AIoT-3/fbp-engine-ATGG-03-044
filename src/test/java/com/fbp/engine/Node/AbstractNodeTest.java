package com.fbp.engine.Node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.core.AbstractNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractNodeTest {
    private TestNode testNode;
    @BeforeEach
    void setUp() {
        testNode = new TestNode("test-node");
    }

    static class TestNode extends AbstractNode {
        private Message lastProcessedMessage;

        public TestNode(String id) {
            super(id);
        }

        @Override
        protected void onProcess(Message message) {
            lastProcessedMessage = message;
        }

        public void registerInputPort(String name) {
            addInputPort(name);
        }

        public void registerOutputPort(String name) {
            addOutputPort(name);
        }

        public void sendMessage(String portName, Message message) {
            send(portName, message);
        }

        public Message getLastProcessedMessage() {
            return lastProcessedMessage;
        }
    }
    @Test
    @DisplayName("getId 반환")
    void getIdTest(){
        assertEquals("test-node", testNode.getId());
    }
    @Test
    @DisplayName("addInputPort 등록")
    void addInputTest(){
        testNode.registerInputPort("in");
        assertNotNull(testNode.getInputPort("in"));
    }
    @Test
    @DisplayName("addOutputPort 등록")
    void addOutputTest(){
        testNode.registerOutputPort("out");
        assertNotNull(testNode.getOutputPort("out"));
    }
    @Test
    @DisplayName("미등록 포트 조회")
    void notPortTest(){
        assertNull(testNode.getInputPort("not-exist"));
    }
    @Test
    @DisplayName("process → onProcess 호출")
    void onProcessTest(){
        Message message = new Message(Map.of("temperature",25.5));
        testNode.process(message);
        assertSame(message, testNode.getLastProcessedMessage());
    }
    @Test
    @DisplayName("send로 메시지 전달")
    void sendTest() throws InterruptedException{
        testNode.registerOutputPort("out");
        Connection connection = new Connection();
        Message message = new Message(Map.of("temperature",25.5));
        testNode.getOutputPort("out").connect(connection);
        testNode.sendMessage("out",message);
        Message receivedMessage = connection.poll();
        assertSame(message, receivedMessage);
    }


}
