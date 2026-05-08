package com.fbp.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ApiResponse {
    private ApiResponse() {
    }

    public static void json(HttpExchange exchange, ObjectMapper objectMapper, int statusCode, Object body) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    public static void error(HttpExchange exchange, ObjectMapper objectMapper, int statusCode, String message) throws IOException {
        json(exchange, objectMapper, statusCode, Map.of("error", message));
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
        return requestBody;
    }
}
