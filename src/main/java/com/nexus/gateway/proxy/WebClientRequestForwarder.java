package com.nexus.gateway.proxy;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class WebClientRequestForwarder
        implements RequestForwarder {

    private final WebClient webClient;

    public WebClientRequestForwarder(
            WebClient.Builder builder
    ) {
        this.webClient = builder.build();
    }

    @Override
    public Mono<ResponseEntity<byte[]>> forward(
            ServerWebExchange exchange,
            URI targetUri
    ) {

        return webClient
                .method(exchange.getRequest().getMethod())
                .uri(targetUri)
                .headers(headers ->
                        headers.addAll(
                                exchange.getRequest()
                                        .getHeaders()
                        )
                )
                .body(
                        exchange.getRequest().getBody(),
                        org.springframework.core.io.buffer
                                .DataBuffer.class
                )
                .retrieve()
                .toEntity(byte[].class);
    }
}