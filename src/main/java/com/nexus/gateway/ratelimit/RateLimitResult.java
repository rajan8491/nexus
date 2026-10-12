package com.nexus.gateway.ratelimit;

public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long resetSeconds
) {
}