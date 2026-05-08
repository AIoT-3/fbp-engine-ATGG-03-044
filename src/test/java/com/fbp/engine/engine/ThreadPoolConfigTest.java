package com.fbp.engine.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ThreadPoolConfig - 실행 스레드 풀 설정값 검증")
class ThreadPoolConfigTest {
    @Test
    @DisplayName("생성자로 받은 core/max/queueCapacity 값을 그대로 보관한다")
    void storesConfiguredValues() {
        ThreadPoolConfig config = new ThreadPoolConfig(2, 4, 100);

        assertEquals(2, config.getCoreSize());
        assertEquals(4, config.getMaxSize());
        assertEquals(100, config.getQueueCapacity());
    }

    @Test
    @DisplayName("기본 설정은 최소 1개 이상의 스레드와 양수 큐 용량을 가진다")
    void defaultConfigUsesSensiblePositiveValues() {
        ThreadPoolConfig config = ThreadPoolConfig.defaultConfig();

        assertTrue(config.getCoreSize() >= 1);
        assertTrue(config.getMaxSize() >= config.getCoreSize());
        assertEquals(100, config.getQueueCapacity());
    }

    @Test
    @DisplayName("core/max/queueCapacity가 잘못된 값이면 생성자를 거부한다")
    void invalidValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ThreadPoolConfig(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ThreadPoolConfig(2, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ThreadPoolConfig(1, 1, 0));
    }
}
