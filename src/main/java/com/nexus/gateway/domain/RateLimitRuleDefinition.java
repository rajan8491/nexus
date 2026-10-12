package com.nexus.gateway.domain;

import com.nexus.gateway.ratelimit.Algorithm;

public record RateLimitRuleDefinition(
        String id,
        String path,
        String method,
        KeyType keyType,
        String headerName,
        Algorithm algorithm,
        int capacity,
        int refillRate,
        int windowSeconds
) {
    public enum KeyType {IP, HEADER}
}