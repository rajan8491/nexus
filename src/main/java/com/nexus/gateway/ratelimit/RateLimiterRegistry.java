package com.nexus.gateway.ratelimit;

import com.nexus.gateway.ratelimit.strategy.RateLimiterStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RateLimiterRegistry {

    private final Map<Algorithm, RateLimiterStrategy> strategies;

    public RateLimiterRegistry(List<RateLimiterStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(RateLimiterStrategy::getAlgorithm, Function.identity()));
    }

    public RateLimiterStrategy getStrategy(Algorithm algorithm) {
        RateLimiterStrategy strategy = strategies.get(algorithm);
        if (strategy == null) {
            throw new IllegalArgumentException("No rate limiter strategy configured for: " + algorithm);
        }
        return strategy;
    }
}