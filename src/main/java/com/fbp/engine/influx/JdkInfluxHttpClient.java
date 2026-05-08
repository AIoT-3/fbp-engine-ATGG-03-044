package com.fbp.engine.influx;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class JdkInfluxHttpClient implements InfluxHttpClient {
    private final InfluxDbConfig config;
    private final HttpClient httpClient;

    public JdkInfluxHttpClient(InfluxDbConfig config) {
        this(config, HttpClient.newHttpClient());
    }

    public JdkInfluxHttpClient(InfluxDbConfig config, HttpClient httpClient) {
        if (config == null) {
            throw new IllegalArgumentException("InfluxDbConfig는 null일 수 없습니다");
        }
        if (httpClient == null) {
            throw new IllegalArgumentException("HttpClient는 null일 수 없습니다");
        }
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public void send(String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(writeUri())
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (config.getToken() != null && !config.getToken().trim().isEmpty()) {
            builder.header("Authorization", "Token " + config.getToken().trim());
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("InfluxDB write 실패: HTTP " + status + " " + response.body());
        }
    }

    private URI writeUri() {
        String baseUrl = config.getUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String org = URLEncoder.encode(config.getOrg(), StandardCharsets.UTF_8);
        String bucket = URLEncoder.encode(config.getBucket(), StandardCharsets.UTF_8);
        return URI.create(baseUrl + "/api/v2/write?org=" + org
                + "&bucket=" + bucket
                + "&precision=ns");
    }
}
