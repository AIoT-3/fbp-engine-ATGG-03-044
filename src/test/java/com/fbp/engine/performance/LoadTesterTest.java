package com.fbp.engine.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoadTester - pass-through 기준 부하 테스트 결과")
class LoadTesterTest {
    @Test
    @DisplayName("10,000건 pass-through 처리량/지연시간/에러율이 기준 안에 들어온다")
    void passThroughLoadTestMeetsBasicThroughputTarget() {
        LoadTester loadTester = new LoadTester();

        PerformanceResult result = loadTester.runPassThrough(10_000);

        assertEquals(10_000, result.getMessageCount());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.getThroughputPerSecond() >= 1_000.0);
        assertTrue(result.getAverageLatencyMillis() < 10.0);
        assertTrue(result.getP99LatencyMillis() < 50.0);
        assertEquals(0.0, result.getErrorRate());
    }

    @Test
    @DisplayName("메시지 수가 0 이하이면 부하 테스트 실행을 거부한다")
    void rejectsInvalidMessageCount() {
        LoadTester loadTester = new LoadTester();

        assertThrows(IllegalArgumentException.class, () -> loadTester.runPassThrough(0));
    }
}
