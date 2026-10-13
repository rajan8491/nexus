package com.nexus.gateway.filter;

import com.nexus.gateway.handler.GatewayHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(2)
public class GatewayWebFilter implements WebFilter {

    private final GatewayHandler gatewayHandler;

    public GatewayWebFilter(GatewayHandler gatewayHandler) {
        this.gatewayHandler = gatewayHandler;
    }

    @Override
    @NonNull
    public Mono<Void> filter(
            @NonNull ServerWebExchange exchange,
            @NonNull WebFilterChain chain
    ) {

        return gatewayHandler
                .handle(exchange)
                .flatMap(response ->
                        writeResponse(exchange, response)
                );
    }

    private Mono<Void> writeResponse(
            ServerWebExchange exchange,
            ResponseEntity<byte[]> response
    ) {

        exchange.getResponse()
                .setStatusCode(response.getStatusCode());

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(response.getHeaders());

        exchange.getResponse()
                .getHeaders()
                .putAll(headers);

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            return exchange.getResponse().setComplete();
        }

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
