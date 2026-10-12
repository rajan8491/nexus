package com.nexus.gateway.domain;

public record HealthCheckConfig(
        boolean enabled,
        int intervalSeconds,
        int timeoutMillis,
        String path
) {
}