package com.fbp.engine.influx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InfluxDbWriter - line protocol 배치 쓰기와 장애 버퍼링")
class InfluxDbWriterTest {
    @Test
    @DisplayName("flush는 큐에 쌓인 여러 line protocol을 하나의 배치 본문으로 전송한다")
    void flushSendsLinesAsOneBatch() {
        FakeInfluxHttpClient client = new FakeInfluxHttpClient();
        InfluxDbWriter writer = new InfluxDbWriter(testConfig(10, 100), client);

        writer.writeLine("flow_stats processed=1i 1");
        writer.writeLine("node_stats errors=0i 1");

        assertTrue(writer.flush());

        assertEquals(1, client.bodies.size());
        assertEquals("flow_stats processed=1i 1\nnode_stats errors=0i 1", client.bodies.get(0));
        assertEquals(2, writer.status().getTotalWritten());
        assertEquals(0, writer.status().getQueueSize());
    }

    @Test
    @DisplayName("버퍼가 가득 차면 가장 오래된 line을 버리고 드롭 카운트를 증가시킨다")
    void fullBufferDropsOldestLine() {
        FakeInfluxHttpClient client = new FakeInfluxHttpClient();
        InfluxDbWriter writer = new InfluxDbWriter(testConfig(10, 2), client);

        writer.writeLine("first value=1i");
        writer.writeLine("second value=2i");
        writer.writeLine("third value=3i");
        writer.flush();

        assertEquals("second value=2i\nthird value=3i", client.bodies.get(0));
        assertEquals(1, writer.status().getTotalDropped());
    }

    @Test
    @DisplayName("전송 실패 시 line을 큐에 남겨 다음 flush에서 재전송할 수 있게 한다")
    void failedFlushKeepsLinesForNextFlush() {
        FakeInfluxHttpClient client = new FakeInfluxHttpClient();
        client.fail = true;
        InfluxDbWriter writer = new InfluxDbWriter(testConfig(10, 10), client);

        writer.writeLine("flow_stats processed=1i 1");

        assertFalse(writer.flush());
        assertEquals(1, writer.status().getQueueSize());
        assertFalse(writer.status().isConnected());

        client.fail = false;
        assertTrue(writer.flush());
        assertEquals("flow_stats processed=1i 1", client.bodies.get(0));
        assertEquals(1, writer.status().getTotalWritten());
    }

    private InfluxDbConfig testConfig(int batchSize, int maxBufferSize) {
        return new InfluxDbConfig(
                "http://localhost:8086",
                "token",
                "fbp",
                "fbp-metrics",
                batchSize,
                1000,
                1,
                1,
                "memory",
                maxBufferSize
        );
    }

    private static class FakeInfluxHttpClient implements InfluxHttpClient {
        private final List<String> bodies = new ArrayList<>();
        private boolean fail;

        @Override
        public void send(String body) throws IOException {
            if (fail) {
                throw new IOException("temporary failure");
            }
            bodies.add(body);
        }
    }
}
