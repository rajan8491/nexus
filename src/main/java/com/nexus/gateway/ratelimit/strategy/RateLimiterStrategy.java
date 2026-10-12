package com.nexus.gateway.ratelimit.strategy;

import com.nexus.gateway.domain.RateLimitRuleDefinition;
import com.nexus.gateway.ratelimit.Algorithm;
import com.nexus.gateway.ratelimit.RateLimitResult;
import reactor.core.publisher.Mono;

public interface RateLimiterStrategy {
    Algorithm getAlgorithm();

    Mono<RateLimitResult> isAllowed(String clientKey, RateLimitRuleDefinition rule);
}