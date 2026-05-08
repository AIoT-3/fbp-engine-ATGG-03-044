package com.fbp.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.engine.FlowManagerException;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.FlowParserException;
import com.fbp.engine.parser.JsonFlowParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpApiServer {
    private final int port;
    private final FlowManager flowManager;
    private final JsonFlowParser flowParser;
    private final MetricsCollector metricsCollector;
    private final ObjectMapper objectMapper;
    private final Instant startedAt;
    private HttpServer server;
    private ExecutorService executorService;

    public HttpApiServer(int port, FlowManager flowManager, MetricsCollector metricsCollector) {
        this(port, flowManager, new JsonFlowParser(), metricsCollector, new ObjectMapper());
    }

    public HttpApiServer(int port, FlowManager flowManager) {
        this(port, flowManager, new JsonFlowParser(), flowManager.getMetricsCollector(), new ObjectMapper());
    }

    public HttpApiServer(int port, FlowManager flowManager, JsonFlowParser flowParser,
                         MetricsCollector metricsCollector, ObjectMapper objectMapper) {
        this.port = port;
        this.flowManager = flowManager;
        this.flowParser = flowParser;
        this.metricsCollector = metricsCollector;
        this.objectMapper = objectMapper;
        this.startedAt = Instant.now();
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/health", this::handleHealth);
            server.createContext("/flows", this::handleFlows);
            server.createContext("/nodes", this::handleNodes);
            executorService = Executors.newCachedThreadPool();
            server.setExecutor(executorService);
            server.start();
        } catch (IOException e) {
            throw new ApiServerException("HTTP API 서버 시작 실패", e);
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    public int getPort() {
        if (server == null) {
            return port;
        }
        return server.getAddress().getPort();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            methodNotAllowed(exchange);
            return;
        }
        ApiResponse.json(exchange, objectMapper, 200, Map.of(
                "status", flowManager.getEngineState().name(),
                "uptime", Duration.between(startedAt, Instant.now()).toMillis(),
                "flowCount", flowManager.getFlowCount()
        ));
    }

    private void handleFlows(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("/flows".equals(path)) {
                if ("GET".equals(method)) {
                    ApiResponse.json(exchange, objectMapper, 200, flowManager.list());
                    return;
                }
                if ("POST".equals(method)) {
                    String body = ApiResponse.readBody(exchange);
                    FlowDefinition definition = flowParser.parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
                    flowManager.deploy(definition);
                    ApiResponse.json(exchange, objectMapper, 201, Map.of(
                            "id", definition.getId(),
                            "status", flowManager.getStatus(definition.getId()).name()
                    ));
                    return;
                }
                methodNotAllowed(exchange);
                return;
            }

            String[] parts = splitPath(path);
            if (parts.length == 2 && "DELETE".equals(method)) {
                flowManager.remove(parts[1]);
                ApiResponse.json(exchange, objectMapper, 200, Map.of("message", "removed", "id", parts[1]));
                return;
            }
            if (parts.length == 3 && "metrics".equals(parts[2]) && "GET".equals(method)) {
                ApiResponse.json(exchange, objectMapper, 200,
                        metricsCollector.getFlowMetrics(parts[1],
                                flowManager.getNodeIds(parts[1]),
                                flowManager.getConnectionIds(parts[1])));
                return;
            }
            ApiResponse.error(exchange, objectMapper, 404, "Not Found");
        } catch (FlowManagerException e) {
            ApiResponse.error(exchange, objectMapper, statusCode(e), e.getMessage());
        } catch (FlowParserException | IllegalArgumentException e) {
            ApiResponse.error(exchange, objectMapper, 400, e.getMessage());
        }
    }

    private void handleNodes(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = splitPath(path);
        if (parts.length == 3 && "stats".equals(parts[2])) {
            if (!"GET".equals(exchange.getRequestMethod())) {
                methodNotAllowed(exchange);
                return;
            }
            ApiResponse.json(exchange, objectMapper, 200, metricsCollector.getNodeMetrics(parts[1]));
            return;
        }
        ApiResponse.error(exchange, objectMapper, 404, "Not Found");
    }

    private void methodNotAllowed(HttpExchange exchange) throws IOException {
        ApiResponse.error(exchange, objectMapper, 405, "Method Not Allowed");
    }

    private int statusCode(FlowManagerException exception) {
        return switch (exception.getErrorType()) {
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case VALIDATION -> 400;
        };
    }

    private String[] splitPath(String path) {
        String trimmed;
        if (path.startsWith("/")) {
            trimmed = path.substring(1);
        } else {
            trimmed = path;
        }
        return trimmed.split("/");
    }
}
