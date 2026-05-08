package com.fbp.engine.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemoryMonitor - 장시간 실행 중 힙 사용량 샘플링")
class MemoryMonitorTest {
    @Test
    @DisplayName("현재 힙 사용량과 GC 후 힙 사용량을 샘플로 기록한다")
    void recordsHeapSamples() {
        MemoryMonitor monitor = new MemoryMonitor();

        long first = monitor.sampleUsedHeap();
        long second = monitor.sampleUsedHeapAfterGc();

        assertTrue(first >= 0);
        assertTrue(second >= 0);
        assertEquals(2, monitor.getSamples().size());
    }

    @Test
    @DisplayName("reset 호출 후 샘플 목록과 마지막 샘플 판정이 초기화된다")
    void resetClearsSamples() {
        MemoryMonitor monitor = new MemoryMonitor();
        monitor.sampleUsedHeap();

        monitor.reset();

        assertTrue(monitor.getSamples().isEmpty());
        assertFalse(monitor.isLastSampleWithin(Long.MAX_VALUE));
    }
}
