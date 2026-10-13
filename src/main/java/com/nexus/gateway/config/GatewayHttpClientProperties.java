package com.nexus.gateway.config;

import jakarta.validation.constraints.Min;

import java.time.Duration;

public record GatewayHttpClientProperties(
        @Min(value = 1, message = "pool.max-connections must be at least 1")
        Integer maxConnections,

        @Min(value = 0, message = "pool.pending-acquire-max-count must be non-negative")
        Integer pendingAcquireMaxCount,

        Duration pendingAcquireTimeout,
        Duration maxIdleTime,
        Duration maxLifeTime,
        Duration connectTimeout,
        Duration responseTimeout,
        Duration readTimeout
) {
    public GatewayHttpClientProperties {
        if (maxConnections == null) maxConnections = 500;
        if (pendingAcquireMaxCount == null) pendingAcquireMaxCount = 1000;
        if (pendingAcquireTimeout == null) pendingAcquireTimeout = Duration.ofSeconds(5);
        if (maxIdleTime == null) maxIdleTime = Duration.ofSeconds(30);
        if (maxLifeTime == null) maxLifeTime = Duration.ofMinutes(5);
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(3);
        if (responseTimeout == null) responseTimeout = Duration.ofSeconds(10);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(10);
    }
}