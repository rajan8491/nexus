package com.nexus.gateway.proxy;

import com.nexus.gateway.proxy.header.DefaultGatewayHeaderFilter;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebClientRequestForwarderTest {

    private DisposableServer server;
    private WebClientRequestForwarder forwarder;
    private DefaultTargetUriBuilder targetUriBuilder;

    private static Flux<DataBuffer> bodyChunks(String... chunks) {
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        return Flux.fromArray(chunks)
                .map(chunk -> factory.wrap(chunk.getBytes(StandardCharsets.UTF_8)));
    }

    @BeforeEach
    void setUp() {
        server = HttpServer.create()
                .port(0)
                .handle(this::handleRequest)
                .bindNow();

        forwarder = new WebClientRequestForwarder(WebClient.create(), new DefaultGatewayHeaderFilter());
        targetUriBuilder = new DefaultTargetUriBuilder();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.disposeNow();
        }
    }

    @Test
    void shouldForwardGetRequestWithPathAndQueryParameters() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(
                                HttpMethod.GET,
                                URI.create("http://gateway.local/api/users%2Fencoded?verbose=true&tag=a%2Bb")
                        )
                        .build()
        );

        ForwardedResult result = forwardAndReadBody(
                exchange,
                targetUriBuilder.build(
                        URI.create("http://localhost:" + server.port() + "/base"),
                        exchange
                )
        );

        assertEquals(200, result.statusCode());
        assertEquals("echo", result.headers().getFirst("X-Upstream"));
        assertEquals(List.of("one", "two"), result.headers().getValuesAsList("X-Multi"));
        assertEquals("GET|/base/api/users%2Fencoded?verbose=true&tag=a%2Bb|", result.body());
    }

    @Test
    void shouldForwardPostBodyWithoutBufferingAndPreserveEmptyBodyRequests() {
        MockServerWebExchange postExchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(
                                HttpMethod.POST,
                                URI.create("http://gateway.local/echo/body")
                        )
                        .body(bodyChunks("hello ", "world"))
        );

        ForwardedResult postResult = forwardAndReadBody(
                postExchange,
                targetUriBuilder.build(
                        URI.create("http://localhost:" + server.port()),
                        postExchange
                )
        );

        assertEquals(200, postResult.statusCode());
        assertEquals("POST|/echo/body|hello world", postResult.body());

        MockServerWebExchange emptyExchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(
                                HttpMethod.DELETE,
                                URI.create("http://gateway.local/echo/empty")
                        )
                        .build()
        );

        ForwardedResult emptyResult = forwardAndReadBody(
                emptyExchange,
                targetUriBuilder.build(
                        URI.create("http://localhost:" + server.port()),
                        emptyExchange
                )
        );

        assertEquals(200, emptyResult.statusCode());
        assertEquals("DELETE|/echo/empty|", emptyResult.body());
    }

    @Test
    void shouldPreserveUpstreamStatusHeadersAndStreamingBody() {
        MockServerWebExchange createdExchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(
                                HttpMethod.GET,
                                URI.create("http://gateway.local/created")
                        )
                        .build()
        );

        ForwardedResult createdResult = forwardAndReadBody(
                createdExchange,
                targetUriBuilder.build(
                        URI.create("http://localhost:" + server.port()),
                        createdExchange
                )
        );

        assertEquals(201, createdResult.statusCode());
        assertEquals("created", createdResult.headers().getFirst("X-Response"));
        assertEquals(List.of("a", "b"), createdResult.headers().getValuesAsList("X-Multi"));
        assertEquals("created-body", createdResult.body());

        MockServerWebExchange streamingExchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(
                                HttpMethod.GET,
                                URI.create("http://gateway.local/stream")
                        )
                        .build()
        );

        ForwardedResult streamingResult = forwardAndReadBody(
                streamingExchange,
                targetUriBuilder.build(
                        URI.create("http://localhost:" + server.port()),
                        streamingExchange
                )
        );

        assertEquals(200, streamingResult.statusCode());
        assertEquals("true", streamingResult.headers().getFirst("X-Stream"));
        assertEquals("chunk-1chunk-2chunk-3", streamingResult.body());
    }

    @Test
    void shouldPreserveUpstreamErrorResponsesInsteadOfFailingTheProxyCall() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(
                                HttpMethod.GET,
                                URI.create("http://gateway.local/teapot")
                        )
                        .build()
        );

        ForwardedResult result = forwardAndReadBody(
                exchange,
                targetUriBuilder.build(
                        URI.create("http://localhost:" + server.port()),
                        exchange
                )
        );

        assertEquals(418, result.statusCode());
        assertEquals("upstream", result.headers().getFirst("X-Error"));
        assertEquals("short and stout", result.body());
    }

    private Mono<Void> handleRequest(
            reactor.netty.http.server.HttpServerRequest request,
            reactor.netty.http.server.HttpServerResponse response
    ) {

        String uri = request.uri();

        if (uri.startsWith("/created")) {
            return response.status(HttpResponseStatus.CREATED)
                    .header("X-Response", "created")
                    .addHeader("X-Multi", "a")
                    .addHeader("X-Multi", "b")
                    .sendString(Mono.just("created-body"))
                    .then();
        }

        if (uri.startsWith("/teapot")) {
            return response.status(HttpResponseStatus.valueOf(418))
                    .header("X-Error", "upstream")
                    .sendString(Mono.just("short and stout"))
                    .then();
        }

        if (uri.startsWith("/stream")) {
            return response.status(HttpResponseStatus.OK)
                    .header("X-Stream", "true")
                    .sendString(Flux.just("chunk-1", "chunk-2", "chunk-3"))
                    .then();
        }

        return request.receive()
                .asString(StandardCharsets.UTF_8)
                .collectList()
                .flatMap(chunks -> response.status(HttpResponseStatus.OK)
                        .header("X-Upstream", "echo")
                        .addHeader("X-Multi", "one")
                        .addHeader("X-Multi", "two")
                        .sendString(Mono.just(request.method().name() + "|" + uri + "|" + String.join("", chunks)))
                        .then());
    }

    private ForwardedResult forwardAndReadBody(
            MockServerWebExchange exchange,
            URI targetUri
    ) {

        ResponseEntity<byte[]> response = forwarder.forward(exchange, targetUri).block();
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(response.getHeaders());
        return new ForwardedResult(
                response.getStatusCode().value(),
                headers,
                response.getBody() == null ? "" : new String(response.getBody(), StandardCharsets.UTF_8)
        );
    }

    private record ForwardedResult(
            int statusCode,
            HttpHeaders headers,
            String body
    ) {
    }
}




