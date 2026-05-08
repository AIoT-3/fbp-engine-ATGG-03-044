package com.fbp.engine.plugin;

public class PluginLoadFailure {
    private final String source;
    private final String message;

    public PluginLoadFailure(String source, String message) {
        this.source = source;
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }
}
