package com.nexus.gateway.proxy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultTargetUriBuilderTest {

    private final DefaultTargetUriBuilder builder = new DefaultTargetUriBuilder();

    @Test
    void shouldPreserveRawPathAndQueryEncoding() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(
                                org.springframework.http.HttpMethod.GET,
                                URI.create("http://gateway.local/api/users%2Fencoded?verbose=true&tag=a%2Bb")
                        )
                        .build()
        );

        URI targetUri = builder.build(
                URI.create("http://localhost:9090/base"),
                exchange
        );

        assertEquals(
                "http://localhost:9090/base/api/users%2Fencoded?verbose=true&tag=a%2Bb",
                targetUri.toString()
        );
    }
}

