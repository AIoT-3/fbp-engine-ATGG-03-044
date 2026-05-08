package com.fbp.engine.api;

import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.node.CollectorNode;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.registry.NodeRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@DisplayName("HttpApiServer - JDK HttpServer 기반 플로우 관리 REST API")
class HttpApiServerTest {
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    @DisplayName("GET /health는 JSON 상태와 flowCount를 반환한다")
    void healthReturnsJson() throws Exception {
        HttpApiServer server = new HttpApiServer(0, flowManager());
        server.start();
        try {
            HttpResponse<String> response = get(server, "/health");

            assertEquals(200, response.statusCode());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
            assertTrue(response.body().contains("flowCount"));
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("GET /flows는 배포된 플로우 목록과 transport 정보를 반환한다")
    void getFlowsReturnsDeployedFlows() throws Exception {
        HttpApiServer server = new HttpApiServer(0, flowManager());
        server.start();
        try {
            post(server, "/flows", validFlowJson("flow-1"));

            HttpResponse<String> response = get(server, "/flows");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("flow-1"));
            assertTrue(response.body().contains("local"));
            assertJson(response);
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("POST /flows는 JSON 플로우를 배포하고 201 CREATED를 반환한다")
    void postFlowsDeploysFlow() throws Exception {
        HttpApiServer server = new HttpApiServer(0, flowManager());
        server.start();
        try {
            HttpResponse<String> response = post(server, "/flows", validFlowJson("flow-1"));

            assertEquals(201, response.statusCode());
            assertTrue(response.body().contains("\"id\":\"flow-1\""));
            assertTrue(response.body().contains("\"status\":\"RUNNING\""));
            assertJson(response);
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("POST /flows에 잘못된 JSON을 보내면 400 BAD REQUEST를 반환한다")
    void postFlowsWithInvalidJsonReturnsBadRequest() throws Exception {
        HttpApiServer server = new HttpApiServer(0, flowManager());
        server.start();
        try {
            HttpResponse<String> response = post(server, "/flows", "{ invalid");

            assertEquals(400, response.statusCode());
            assertTrue(response.body().contains("error"));
            assertJson(response);
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("DELETE /flows/{id}는 실행 중인 플로우를 제거한다")
    void deleteFlowRemovesFlow() throws Exception {
        HttpApiServer server = new HttpApiServer(0, flowManager());
        server.start();
        try {
            post(server, "/flows", validFlowJson("flow-1"));

            HttpResponse<String> response = delete(server, "/flows/flow-1");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("removed"));
            assertJson(response);
            assertFalse(get(server, "/flows").body().contains("flow-1"));
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("DELETE /flows/{id}에서 없는 id는 404 NOT FOUND를 반환한다")
    void deleteMissingFlowReturnsNotFound() throws Exception {
        HttpApiServer server = new HttpApiServer(0, flowManager());
        server.start();
        try {
            HttpResponse<String> response = delete(server, "/flows/missing");

            assertEquals(404, response.statusCode());
            assertTrue(response.body().contains("error"));
            assertJson(response);
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("GET /flows/{id}/metrics는 플로우 메트릭 JSON을 반환한다")
    void getFlowMetricsReturnsJson() throws Exception {
        HttpApiServer server = new HttpApiServer(0, flowManager());
        server.start();
        try {
            post(server, "/flows", validFlowJson("flow-1"));

            HttpResponse<String> response = get(server, "/flows/flow-1/metrics");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("flow-1"));
            assertTrue(response.body().contains("nodes"));
            assertJson(response);
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("정의되지 않은 경로는 404 NOT FOUND를 반환한다")
    void unknownPathReturnsNotFound() throws Exception {
        HttpApiServer server = new HttpApiServer(0, flowManager());
        server.start();
        try {
            HttpResponse<String> response = get(server, "/flows/flow-1/unknown");

            assertEquals(404, response.statusCode());
            assertJson(response);
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 405 METHOD NOT ALLOWED를 반환한다")
    void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
        HttpApiServer server = new HttpApiServer(0, flowManager());
        server.start();
        try {
            HttpResponse<String> response = request(server, "PUT", "/health", "");

            assertEquals(405, response.statusCode());
            assertTrue(response.body().contains("Method Not Allowed"));
            assertJson(response);
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("이미 사용 중인 포트로 서버를 시작하면 ApiServerException이 발생한다")
    void startingOnUsedPortFails() {
        HttpApiServer first = new HttpApiServer(0, flowManager());
        first.start();
        try {
            HttpApiServer second = new HttpApiServer(first.getPort(), flowManager());

            assertThrows(ApiServerException.class, second::start);
        } finally {
            first.stop();
        }
    }

    private FlowManager flowManager() {
        NodeRegistry registry = new NodeRegistry();
        registry.register("Generator", (id, config) -> new GeneratorNode(id));
        registry.register("Collector", (id, config) -> new CollectorNode(id));
        return new FlowManager(new FlowEngine(), registry);
    }

    private HttpResponse<String> get(HttpApiServer server, String path) throws IOException, InterruptedException {
        return request(server, "GET", path, "");
    }

    private HttpResponse<String> post(HttpApiServer server, String path, String body) throws IOException, InterruptedException {
        return request(server, "POST", path, body);
    }

    private HttpResponse<String> delete(HttpApiServer server, String path) throws IOException, InterruptedException {
        return request(server, "DELETE", path, "");
    }

    private HttpResponse<String> request(HttpApiServer server, String method, String path, String body)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + path))
                .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String validFlowJson(String id) {
        return """
                {
                  "id": "%s",
                  "name": "test flow",
                  "nodes": [
                    { "id": "source", "type": "Generator" },
                    { "id": "sink", "type": "Collector" }
                  ],
                  "connections": [
                    { "from": "source:out", "to": "sink:in" }
                  ]
                }
                """.formatted(id);
    }

    private void assertJson(HttpResponse<String> response) {
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }
}
