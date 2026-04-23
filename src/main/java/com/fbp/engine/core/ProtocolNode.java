package com.fbp.engine.core;

import com.fbp.engine.message.Message;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class ProtocolNode extends AbstractNode{
    protected enum ConnectionState{
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
    private final Map<String, Object> config;
    private ConnectionState connectionState;
    private final long reconnectIntervalMs;
    private ScheduledExecutorService reconnectScheduler;
    private ScheduledFuture<?> reconnectFuture;

    protected ProtocolNode(String id, Map<String, Object> config) {
        super(id);
        this.config = config;
        this.connectionState = ConnectionState.DISCONNECTED;
        this.reconnectIntervalMs = ((Number) config.getOrDefault("reconnectIntervalMs", 5000L)).longValue();
    }
    @Override
    public void initialize() {
        connectionState = ConnectionState.CONNECTING;
        try {
            connect();
            connectionState = ConnectionState.CONNECTED;
        } catch (Exception e) {
            connectionState = ConnectionState.ERROR;
            reconnect();
        }
    }

    @Override
    public void shutdown() {
        stopReconnectScheduler();
        try {
            disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        connectionState = ConnectionState.DISCONNECTED;
    }

    protected abstract void connect() throws Exception;

    protected abstract void disconnect() throws Exception;

    protected void reconnect() {
        int maxRetries = (int) config.getOrDefault("maxRetries", 10);

        stopReconnectScheduler();
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        final int[] retryCount = {0};

        reconnectFuture = reconnectScheduler.scheduleAtFixedRate(() -> {
            if (retryCount[0] >= maxRetries || connectionState == ConnectionState.CONNECTED) {
                stopReconnectScheduler();
                return;
            }

            try {
                connectionState = ConnectionState.CONNECTING;
                connect();
                connectionState = ConnectionState.CONNECTED;
                stopReconnectScheduler();
            } catch (Exception e) {
                connectionState = ConnectionState.ERROR;
                retryCount[0]++;
            }
        }, reconnectIntervalMs, reconnectIntervalMs, TimeUnit.MILLISECONDS);
    }

    public Object getConfig(String key) {
        return config.get(key);
    }

    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }
    public ConnectionState getConnectionState() {
        return connectionState;
    }

    @Override
    protected void onProcess(Message message) {
    }

    private void stopReconnectScheduler() {
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdown();
            reconnectScheduler = null;
        }
    }
}
