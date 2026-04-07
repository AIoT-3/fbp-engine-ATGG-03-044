package com.fbp.engine.Node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.TimerNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimerNodeTest {
    private TimerNode timerNode;
    private Connection connection;
    @BeforeEach
    void setUp() {
        timerNode = new TimerNode("timer-1", 500);
        connection = new Connection();
        timerNode.getOutputPort("out").connect(connection);
    }
    @Test
    @DisplayName("initialize 후 메시지 생성")
    void initializeTest() throws InterruptedException{
        timerNode.initialize();
        Thread.sleep(500);
        assertTrue(connection.getBufferSize()>0);
        timerNode.shutdown();
    }
    @Test
    @DisplayName("tick 증가")
    void tickTest() throws InterruptedException{
        timerNode.initialize();
        Message first = connection.poll();
        Message second = connection.poll();
        Message third = connection.poll();

        timerNode.shutdown();

        assertEquals(Integer.valueOf(0),first.get("tick"));
        assertEquals(Integer.valueOf(1),second.get("tick"));
        assertEquals(Integer.valueOf(2),third.get("tick"));
    }

    @Test
    @DisplayName("shutdown 후 정지")
    void shutdownTest() throws InterruptedException{
        timerNode.initialize();
        Thread.sleep(350);
        timerNode.shutdown();
        Thread.sleep(200);
        int size = connection.getBufferSize();
        Thread.sleep(300);
        assertEquals(size, connection.getBufferSize());
    }
    @Test
    @DisplayName("주기 확인")
    void intervalTest() throws InterruptedException{
        timerNode.initialize();
        Thread.sleep(2000);
        timerNode.shutdown();
        int count = connection.getBufferSize();
        assertTrue(count >= 3 && count <= 5);
    }

}