package com.fbp.engine.influx;

import java.io.IOException;

public interface InfluxHttpClient {
    void send(String body) throws IOException, InterruptedException;
}
