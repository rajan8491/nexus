package com.nexus.gateway.domain;

import java.time.Duration;

public record HttpClientDefinition(
        int maxConnections,
        int pendingAcquireMaxCount,
        Duration pendingAcquireTimeout,
        Duration maxIdleTime,
        Duration maxLifeTime,
        Duration connectTimeout,
        Duration responseTimeout,
        Duration readTimeout
) {
}