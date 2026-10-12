package com.nexus.gateway.ratelimit;

import com.nexus.gateway.domain.RateLimitRuleDefinition;
import com.nexus.gateway.ratelimit.strategy.RateLimiterStrategy;
import com.nexus.gateway.registry.GatewayRegistry;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Component
@Order(1) // Runs ahead of the routing GatewayWebFilter
@RequiredArgsConstructor
public class RateLimitingWebFilter implements WebFilter {

    private final GatewayRegistry gatewayRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final PathPatternParser pathPatternParser = new PathPatternParser();

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        if (!gatewayRegistry.isRateLimitingEnabled()) {
            return chain.filter(exchange);
        }

        Optional<RateLimitRuleDefinition> matchedRule = findMatchingRule(exchange);
        if (matchedRule.isEmpty()) {
            return chain.filter(exchange);
        }

        RateLimitRuleDefinition rule = matchedRule.get();
        String clientKey = resolveClientKey(exchange, rule);

        RateLimiterStrategy strategy = rateLimiterRegistry.getStrategy(rule.algorithm());

        return strategy.isAllowed(clientKey, rule)
                .flatMap(result -> {
                    ServerHttpResponse response = exchange.getResponse();
                    response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));
                    if (result.resetSeconds() > 0) {
                        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(result.resetSeconds()));
                    }

                    if (result.allowed()) {
                        return chain.filter(exchange);
                    }
                    return rejectRequest(exchange, rule);
                });
    }

    private Optional<RateLimitRuleDefinition> findMatchingRule(ServerWebExchange exchange) {
        PathContainer path = exchange.getRequest().getPath();
        String method = exchange.getRequest().getMethod().name();

        return gatewayRegistry.getRateLimitRules().stream()
                .filter(rule -> {
                    // Match HTTP method if defined
                    if (rule.method() != null && !rule.method().isBlank() && !rule.method().equalsIgnoreCase(method)) {
                        return false;
                    }

                    // Match URL path pattern
                    return pathPatternParser.parse(rule.path()).matches(path);
                })
                .findFirst();
    }

    private String resolveClientKey(ServerWebExchange exchange, RateLimitRuleDefinition rule) {
        if (rule.keyType() == RateLimitRuleDefinition.KeyType.HEADER && rule.headerName() != null) {
            String headerValue = exchange.getRequest().getHeaders().getFirst(rule.headerName());
            if (headerValue != null && !headerValue.isBlank()) {
                return headerValue;
            }
        }
        // Fallback to IP address
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown-client";
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange, RateLimitRuleDefinition rule) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = """
                {
                  "timestamp": "%s",
                  "status": 429,
                  "error": "Too Many Requests",
                  "message": "Exceeded request quota for rule '%s' using algorithm '%s'",
                  "path": "%s"
                }
                """.formatted(
                Instant.now().toString(),
                rule.id(),
                rule.algorithm(),
                exchange.getRequest().getPath().value()
        );

        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}