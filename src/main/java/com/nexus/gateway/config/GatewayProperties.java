package com.nexus.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(
        @Valid
        GatewayHttpClientProperties httpClient,

        @Valid
        GatewaySecurityProperties security,

        String defaultLoadBalancer,

        @Valid
        GatewayHealthCheckProperties healthCheck,

        @Valid
        GatewayRateLimitingProperties rateLimiting,

        @NotEmpty(message = "gateway.routes must contain at least one route definition")
        List<@Valid GatewayRouteProperties> routes,

        @NotEmpty(message = "gateway.services must contain at least one registered service")
        Map<String, @Valid GatewayServiceProperties> services

) {
    public GatewayProperties {
        if (defaultLoadBalancer == null || defaultLoadBalancer.isBlank()) {
            defaultLoadBalancer = "ROUND_ROBIN";
        }
        if (healthCheck == null) {
            healthCheck = new GatewayHealthCheckProperties(true, 10, 2000, "/actuator/health");
        }
        if (rateLimiting == null) {
            rateLimiting = new GatewayRateLimitingProperties(false, "TOKEN_BUCKET", List.of());
        }
        if (security == null) {
            security = new GatewaySecurityProperties(false, null, List.of());
        }
    }
}