package com.fbp.engine.engine;

public class ThreadPoolConfig {
    private final int coreSize;
    private final int maxSize;
    private final int queueCapacity;

    public ThreadPoolConfig(int coreSize, int maxSize, int queueCapacity) {
        if (coreSize <= 0) {
            throw new IllegalArgumentException("coreSize는 1 이상이어야 합니다");
        }
        if (maxSize < coreSize) {
            throw new IllegalArgumentException("maxSize는 coreSize 이상이어야 합니다");
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity는 1 이상이어야 합니다");
        }

        this.coreSize = coreSize;
        this.maxSize = maxSize;
        this.queueCapacity = queueCapacity;
    }

    public static ThreadPoolConfig defaultConfig() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int defaultCoreSize = Math.max(1, availableProcessors);
        int defaultMaxSize = Math.max(2, availableProcessors * 2);
        int defaultQueueCapacity = 100;

        ThreadPoolConfig config = new ThreadPoolConfig(defaultCoreSize, defaultMaxSize, defaultQueueCapacity);
        return config;
    }

    public int getCoreSize() {
        return coreSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }
}
