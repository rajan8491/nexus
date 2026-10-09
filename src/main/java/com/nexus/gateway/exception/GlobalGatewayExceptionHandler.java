package com.nexus.gateway.exception;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@Order(-2) // High precedence: intercepts errors before Spring's default error page/handler
public class GlobalGatewayExceptionHandler implements ErrorWebExceptionHandler {

    private static @NonNull HttpStatus getStatus(Throwable ex) {
        HttpStatus status;
        if (ex instanceof RouteNotFoundException) {
            status = HttpStatus.NOT_FOUND;                // 404
        } else if (ex instanceof ServiceNotFoundException) {
            status = HttpStatus.NOT_FOUND;                // 404
        } else if (ex instanceof NoAvailableInstanceException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;      // 503
        } else if (ex instanceof GatewayException) {
            status = HttpStatus.BAD_GATEWAY;              // 502
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;    // 500
        }
        return status;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // If response is already committed, pass the error downstream
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = getStatus(ex);

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonPayload = """
                {
                  "timestamp": "%s",
                  "status": %d,
                  "error": "%s",
                  "message": "%s",
                  "path": "%s"
                }
                """.formatted(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Unexpected Gateway Error",
                exchange.getRequest().getPath().value()
        );

        DataBuffer buffer = response.bufferFactory().wrap(jsonPayload.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}