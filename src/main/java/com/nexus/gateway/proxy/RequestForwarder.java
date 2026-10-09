package com.nexus.gateway.proxy;

import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

public interface RequestForwarder {
    Mono<ResponseEntity<byte[]>> forward(
            ServerWebExchange exchange,
            URI targetUri
    );
}
