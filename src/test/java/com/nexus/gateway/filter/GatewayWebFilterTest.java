package com.nexus.gateway.filter;

import com.nexus.gateway.handler.GatewayHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewayWebFilterTest {

    @Test
    void shouldWriteProxiedStatusHeadersAndBodyToExchangeResponse() {
        ResponseEntity<byte[]> proxyResponse = ResponseEntity.status(202)
                .headers(headers())
                .body("hello gateway".getBytes(StandardCharsets.UTF_8));

        GatewayHandler handler = new GatewayHandler(null, null, null, null, null) {
            @Override
            public Mono<ResponseEntity<byte[]>> handle(ServerWebExchange exchange) {
                return Mono.just(proxyResponse);
            }
        };

        GatewayWebFilter filter = new GatewayWebFilter(handler);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(
                                HttpMethod.GET,
                                URI.create("http://gateway.local/proxy/test")
                        )
                        .build()
        );

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertNotNull(exchange.getResponse().getStatusCode());
        assertEquals(202, exchange.getResponse().getStatusCode().value());
        assertEquals("true", exchange.getResponse().getHeaders().getFirst("X-Proxy"));
        assertEquals("one", exchange.getResponse().getHeaders().getFirst("X-Multi"));
        assertEquals("hello gateway", exchange.getResponse().getBodyAsString().block());
    }

    private static HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Proxy", "true");
        headers.add("X-Multi", "one");
        headers.add("X-Multi", "two");
        return headers;
    }
}



