package com.fbp.engine.Node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.SplitNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitNodeTest {
    private SplitNode splitNode;
    private Connection matchconnection;
    private Connection mismatchconnection;
    @BeforeEach
    void setUp() {
        splitNode = new SplitNode("split-1", "tick", 3.0);
        matchconnection = new Connection();
        mismatchconnection = new Connection();
        splitNode.getOutputPort("match").connect(matchconnection);
        splitNode.getOutputPort("mismatch").connect(mismatchconnection);
    }
    @Test
    @DisplayName("조건 만족 → match 포트")
    void matchTest()throws InterruptedException{
        Message message = new Message(Map.of("tick",5));
        splitNode.process(message);
        Message received = matchconnection.poll();
        assertSame(message,received);
        assertEquals(0,mismatchconnection.getBufferSize());
    }
    @Test
    @DisplayName("조건 미달 → mismatch 포트")
    void mismatchTest() throws InterruptedException{
        Message message = new Message(Map.of("tick",1));
        splitNode.process(message);
        Message received = mismatchconnection.poll();
        assertSame(message,received);
        assertEquals(0,matchconnection.getBufferSize());
    }
    @Test
    @DisplayName("양쪽 동시 확인")
    void bothTest() throws InterruptedException{
        Message matchmessage = new Message(Map.of("tick",5));
        Message mismatchmessage = new Message(Map.of("tick",1));
        splitNode.process(matchmessage);
        splitNode.process(mismatchmessage);

        Message receiveMatch = matchconnection.poll();
        Message receiveMisMatch = mismatchconnection.poll();
        assertSame(matchmessage,receiveMatch);
        assertSame(mismatchmessage,receiveMisMatch);
    }
    @Test
    @DisplayName("경계값 처리")
    void edgeTest() throws  InterruptedException{
        Message message = new Message(Map.of("tick",3.0));
        splitNode.process(message);
        Message received = matchconnection.poll();
        assertSame(message,received);
        assertEquals(0,mismatchconnection.getBufferSize());


    }

}