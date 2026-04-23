package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolNodeTest {

    static class TestProtocolNode extends ProtocolNode{
        private int connectionCallCount;
        private int disconnectCallCount;
        private int failUntil;

        public TestProtocolNode(String id, Map<String, Object> config, int failUntil) {
            super(id, config);
            this.failUntil = failUntil;
        }

        @Override
        protected void connect() throws Exception{
            connectionCallCount++;
            if(connectionCallCount <= failUntil){
                throw  new Exception("Connection failed");
            }
        }
        @Override
        protected void disconnect() throws Exception {
            disconnectCallCount++;
        }

        @Override
        protected void onProcess(Message message) {
        }

        public int getConnectCallCount() {
            return connectionCallCount;
        }

        public int getDisconnectCallCount() {
            return disconnectCallCount;
        }
    }

    @Test
    @DisplayName("초기 상태")
    void InitialStateTest(){
        TestProtocolNode testProtocolNode = new TestProtocolNode("protocol-1",Map.of(), 0);
        assertEquals(ProtocolNode.ConnectionState.DISCONNECTED, testProtocolNode.getConnectionState());
    }
    @Test
    @DisplayName("config 조회")
    void GetConfigTest(){
        TestProtocolNode testProtocolNode = new TestProtocolNode(
                "protocol-1",
                Map.of("host","locolhost","port",1883),
                0
        );
        assertEquals("locolhost", testProtocolNode.getConfig("host"));
        assertEquals(1883, testProtocolNode.getConfig("port"));
    }
    @Test
    @DisplayName("initialize -> CONNECTED")
    void InitializeConnectedTest() {
        TestProtocolNode node = new TestProtocolNode("protocol-1", Map.of(), 0);

        node.initialize();

        assertEquals(ProtocolNode.ConnectionState.CONNECTED, node.getConnectionState());
        assertTrue(node.isConnected());
    }
    @Test
    @DisplayName("initialize -> 연결 실패 시 상태")
    void InitializeErrorTest() {
        TestProtocolNode node = new TestProtocolNode(
                "protocol-1",
                Map.of("maxRetries", 1),
                1
        );

        node.initialize();

        assertEquals(ProtocolNode.ConnectionState.ERROR, node.getConnectionState());
        assertFalse(node.isConnected());

        node.shutdown();
    }
    @Test
    @DisplayName("shutdown -> DISCONNECTED")
    void ShutdownTest() {
        TestProtocolNode node = new TestProtocolNode("protocol-1", Map.of(), 0);

        node.initialize();
        node.shutdown();

        assertEquals(ProtocolNode.ConnectionState.DISCONNECTED, node.getConnectionState());
        assertEquals(1, node.getDisconnectCallCount());
    }
    @Test
    @DisplayName("isConnected 반환값")
    void IsConnectedTest(){
        TestProtocolNode testProtocolNode = new TestProtocolNode("protocol-1", Map.of(), 0);
        assertFalse(testProtocolNode.isConnected());
        testProtocolNode.initialize();
        assertTrue(testProtocolNode.isConnected());
        testProtocolNode.shutdown();
        assertFalse(testProtocolNode.isConnected());
    }
    @Test
    @DisplayName("재연결 시도")
    void ReconnectTest() throws InterruptedException {
        TestProtocolNode node = new TestProtocolNode(
                "protocol-1",
                Map.of("maxRetries", 3),
                1
        );

        node.initialize();

        Thread.sleep(5500);

        assertTrue(node.getConnectCallCount() >= 2);
        assertEquals(ProtocolNode.ConnectionState.CONNECTED, node.getConnectionState());

        node.shutdown();
    }
}
