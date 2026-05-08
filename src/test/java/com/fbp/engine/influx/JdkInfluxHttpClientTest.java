package com.fbp.engine.influx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JdkInfluxHttpClient - InfluxDB HTTP write API 요청 생성")
class JdkInfluxHttpClientTest {
    @Test
    @DisplayName("line protocol을 /api/v2/write로 POST하고 org/bucket/token/header를 설정한다")
    void sendPostsLineProtocolToInfluxWriteApi() throws Exception {
        CapturingHttpClient httpClient = new CapturingHttpClient(204, "");
        InfluxDbConfig config = config("http://localhost:8086/", "secret-token");
        JdkInfluxHttpClient client = new JdkInfluxHttpClient(config, httpClient);

        client.send("engine_stats,host=local active_flows=1i");

        HttpRequest request = httpClient.getLastRequest();
        assertEquals("POST", request.method());
        assertEquals("/api/v2/write", request.uri().getPath());
        assertEquals("org=iot+lab&bucket=sensor+data&precision=ns", request.uri().getQuery());
        assertEquals("Token secret-token",
                request.headers().firstValue("Authorization").orElseThrow());
        assertTrue(request.headers().firstValue("Content-Type").orElse("")
                .contains("text/plain"));
        assertEquals("engine_stats,host=local active_flows=1i", httpClient.getLastBody());
    }

    @Test
    @DisplayName("token이 비어 있으면 Authorization 헤더를 추가하지 않는다")
    void blankTokenDoesNotAddAuthorizationHeader() throws Exception {
        CapturingHttpClient httpClient = new CapturingHttpClient(204, "");
        JdkInfluxHttpClient client = new JdkInfluxHttpClient(config("http://localhost:8086", " "), httpClient);

        client.send("line");

        assertFalse(httpClient.getLastRequest().headers().firstValue("Authorization").isPresent());
    }

    @Test
    @DisplayName("InfluxDB가 2xx가 아닌 응답을 주면 IOException으로 실패를 보고한다")
    void nonSuccessStatusThrowsIOException() {
        CapturingHttpClient httpClient = new CapturingHttpClient(500, "broken");
        JdkInfluxHttpClient client = new JdkInfluxHttpClient(config("http://localhost:8086", "token"), httpClient);

        IOException exception = assertThrows(IOException.class, () -> client.send("line"));

        assertTrue(exception.getMessage().contains("HTTP 500"));
        assertTrue(exception.getMessage().contains("broken"));
    }

    private InfluxDbConfig config(String url, String token) {
        return new InfluxDbConfig(
                url,
                token,
                "iot lab",
                "sensor data",
                1000,
                1000,
                1,
                1,
                "memory",
                100
        );
    }

    private static class CapturingHttpClient extends HttpClient {
        private final int statusCode;
        private final String responseBody;
        private HttpRequest lastRequest;
        private String lastBody;

        private CapturingHttpClient(int statusCode, String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        private HttpRequest getLastRequest() {
            return lastRequest;
        }

        private String getLastBody() {
            return lastBody;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException {
            this.lastRequest = request;
            this.lastBody = readBody(request);
            @SuppressWarnings("unchecked")
            T body = (T) responseBody;
            return new SimpleHttpResponse<>(request, statusCode, body);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                HttpResponse.BodyHandler<T> responseBodyHandler) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                HttpResponse.BodyHandler<T> responseBodyHandler,
                                                                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        private String readBody(HttpRequest request) throws IOException {
            HttpRequest.BodyPublisher publisher = request.bodyPublisher().orElseThrow();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();

            publisher.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    byte[] bytes = new byte[item.remaining()];
                    item.get(bytes);
                    outputStream.writeBytes(bytes);
                }

                @Override
                public void onError(Throwable throwable) {
                    failure.set(throwable);
                    latch.countDown();
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });

            try {
                if (!latch.await(3, TimeUnit.SECONDS)) {
                    throw new IOException("HTTP request body 읽기 시간 초과");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            if (failure.get() != null) {
                throw new IOException(failure.get());
            }
            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }

    private static class SimpleHttpResponse<T> implements HttpResponse<T> {
        private final HttpRequest request;
        private final int statusCode;
        private final T body;

        private SimpleHttpResponse(HttpRequest request, int statusCode, T body) {
            this.request = request;
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (left, right) -> true);
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
