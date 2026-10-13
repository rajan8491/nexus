package com.nexus.gateway.proxy;

import com.nexus.gateway.proxy.header.GatewayHeaderFilter;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class WebClientRequestForwarder implements RequestForwarder {

    private final WebClient webClient;
    private final GatewayHeaderFilter headerFilter;

    public WebClientRequestForwarder(
            WebClient webClient,
            GatewayHeaderFilter headerFilter
    ) {
        this.webClient = webClient;
        this.headerFilter = headerFilter;
    }

    @Override
    public Mono<ResponseEntity<byte[]>> forward(
            ServerWebExchange exchange,
            URI targetUri
    ) {
        HttpMethod method = exchange.getRequest().getMethod();

        return webClient
                .method(method)
                .uri(targetUri)
                // 1. Apply centralized Request Header Filtering
                .headers(headers -> headers.addAll(headerFilter.filterRequestHeaders(exchange)))
                .body(BodyInserters.fromDataBuffers(exchange.getRequest().getBody().map(DataBufferUtils::retain)))
                .exchangeToMono(clientResponse ->
                        clientResponse
                                .bodyToMono(byte[].class)
                                .defaultIfEmpty(new byte[0])
                                .map(body -> {
                                    // 2. Apply centralized Response Header Filtering
                                    HttpHeaders filteredResponseHeaders =
                                            headerFilter.filterResponseHeaders(clientResponse.headers().asHttpHeaders());

                                    return ResponseEntity.status(clientResponse.statusCode())
                                            .headers(filteredResponseHeaders)
                                            .body(body);
                                })
                );
    }
}