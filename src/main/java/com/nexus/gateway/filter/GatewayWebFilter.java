package com.nexus.gateway.filter;

import com.nexus.gateway.handler.GatewayHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GatewayWebFilter implements WebFilter {

    private final GatewayHandler gatewayHandler;

    public GatewayWebFilter(GatewayHandler gatewayHandler) {
        this.gatewayHandler = gatewayHandler;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
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

        exchange.getResponse()
                .getHeaders()
                .putAll(response.getHeaders());

        byte[] body = response.getBody();

        if (body == null || body.length == 0) {
            return exchange.getResponse()
                    .setComplete();
        }

        var buffer =
                exchange.getResponse()
                        .bufferFactory()
                        .wrap(body);

        return exchange.getResponse()
                .writeWith(Mono.just(buffer));
    }
}
